package com.opus.readerparser.sources.freewebnovel

import com.opus.readerparser.core.util.computeSourceId
import com.opus.readerparser.data.source.HtmlSource
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.domain.model.SeriesStatus
import io.ktor.client.HttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Source plugin for [Free Web Novel](https://freewebnovel.com/), an English webnovel aggregator.
 *
 * The site serves server-rendered HTML with a consistent card-based listing layout.
 * Parser selectors extract data from the static HTML served on initial load.
 *
 * ### URL patterns
 * - Popular listing : `/sort/most-popular` (single page, no pagination)
 * - Latest listing  : `/sort/latest-release` (page 1), `/sort/latest-release/{page}` (page 2+)
 * - Series detail   : `/novel/{slug}`
 * - Chapter         : `/novel/{slug}/chapter-{n}`
 * - Search          : `/search?searchkey={query}`
 */
class FreeWebNovel(
    client: HttpClient,
) : HtmlSource(client) {

    // =========================================================================
    // Source identity
    // =========================================================================

    override val id: Long = computeSourceId("FreeWebNovel", "en", ContentType.NOVEL)
    override val name: String = "FreeWebNovel"
    override val lang: String = "en"
    override val baseUrl: String = "https://freewebnovel.com"
    override val type: ContentType = ContentType.NOVEL

    // =========================================================================
    // getPopular — most-popular listing
    // =========================================================================

    /**
     * The popular listing is a single non-paginated page.
     */
    override fun popularUrl(page: Int): String = "$baseUrl/sort/most-popular"

    /**
     * Matches each series card on the popular/latest/search listing pages.
     *
     * HTML structure:
     * ```
     * <div class="li-row">
     *   <div class="li">
     *     <div class="con">
     *       <div class="pic">
     *         <a href="/novel/{slug}"><img src="{coverUrl}" /></a>
     *       </div>
     *       <div class="txt">
     *         <h3 class="tit"><a href="/novel/{slug}" title="{title}">{title}</a></h3>
     *         <div class="desc">
     *           <div class="item">...</div>
     *           <div class="item">
     *             <a href="/genre/{genre}" class="novel">{genre}</a>
     *           </div>
     *           <div class="item">
     *             <a class="chapter" ...><span class="s2">Full</span>...</a>
     *           </div>
     *         </div>
     *       </div>
     *     </div>
     *   </div>
     * </div>
     * ```
     */
    override fun popularSelector(): String = "div.ul-list1.ul-list1-2.ss-custom > div.li-row"

    /**
     * Extracts a [Series] from a listing card.
     *
     * Completion is inferred from a `<span class="s2">Full</span>` marker
     * inside the chapter link.  Series without this marker are left as
     * [SeriesStatus.UNKNOWN] — the series details page provides the
     * definitive status.
     */
    override fun seriesFromElement(el: Element): Series {
        val titleLink = el.selectFirst("h3.tit a[href^=\"/novel/\"]")
            ?: error("Series card missing title link")
        val url = titleLink.absUrl("href")
        val title = titleLink.text().trim()
        val coverImg = el.selectFirst(".pic a img")
        val coverUrl = coverImg?.absUrl("src")
        val status = if (el.selectFirst("span.s2")?.text()?.trim() == "Full") {
            SeriesStatus.COMPLETED
        } else {
            SeriesStatus.UNKNOWN
        }
        return Series(
            sourceId = id,
            url = url,
            title = title,
            coverUrl = coverUrl,
            status = status,
            type = type,
        )
    }

    /** Popular page has no pagination — returns `null`. */
    override fun popularNextPageSelector(): String? = null

    // =========================================================================
    // getLatest — latest-release listing (paginated)
    // =========================================================================

    /**
     * Fetches the latest-release listing at `/sort/latest-release[/{page}]`.
     *
     * The page uses the same card markup as the popular listing.  Pagination is
     * detected by the presence of a `>>` link inside `.pages`.
     *
     * ```
     * <div class="pages">
     *   <ul><li>
     *     <a href="/sort/latest-release/1">1</a>
     *     <strong>1</strong>
     *     <a href="/sort/latest-release/2">2</a>
     *     ...
     *     <a href="/sort/latest-release/2">&gt;&gt;</a>
     *   </li></ul>
     * </div>
     * ```
     */
    override suspend fun getLatest(page: Int): SeriesPage {
        val url = if (page == 1) "$baseUrl/sort/latest-release" else "$baseUrl/sort/latest-release/$page"
        val doc = fetchDoc(url)
        val series = doc.select(popularSelector()).map(::seriesFromElement)
        // Exclude javascript:void(0) links — the site renders >> on the last page
        // but with href="javascript:void(0);" instead of a real page URL.
        val hasNext = doc.selectFirst("div.pages a:matches(>>):not([href*=\"javascript\"])") != null
        return SeriesPage(series, hasNext)
    }

    // =========================================================================
    // search
    // =========================================================================

    /**
     * Search uses the same card structure as the popular listing.
     * The site search form submits via POST but also responds to GET
     * at `/search?searchkey={query}`.
     */
    override fun searchUrl(query: String, page: Int, filters: FilterList): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return "$baseUrl/search?searchkey=$encoded"
    }

    // =========================================================================
    // getSeriesDetails
    // =========================================================================

    /**
     * Enriches [series] with details from the series detail page.
     *
     * Key selectors on the detail page:
     * - **Title**       : `.m-desc h1.tit`
     * - **Cover**       : `.m-book1 .pic img`
     * - **Author**      : `a[href^="/author/"]`
     * - **Genres**      : `.m-book1 .txt .item a[href^="/genre/"]`
     * - **Status**      : `.item:has(.glyphicon-time) .right .s1 a` (matches `.s1.s2` for OnGoing, `.s1.s3` for Completed)
     * - **Description** : `.m-desc .txt .inner`
     */
    override fun seriesDetailsParse(doc: Document, series: Series): Series {
        val author = doc.selectFirst("a[href^=\"/author/\"]")?.text()?.trim()
        val descriptionEl = doc.selectFirst(".m-desc .txt .inner")
        val description = descriptionEl?.html()?.trim()
        val genres = doc.select(".m-book1 .txt .item a[href^=\"/genre/\"]")
            .map { it.text().trim() }
        // Use .s1 alone so it matches both .s1.s2 (OnGoing) and .s1.s3 (Completed).
        val statusEl = doc.selectFirst(
            ".m-book1 .txt .item:has(.glyphicon-time) .right .s1 a",
        )
        val status = parseDetailStatus(statusEl?.text()?.trim())
        val coverUrl = doc.selectFirst(".m-book1 .pic img")?.absUrl("src")

        return series.copy(
            author = author,
            description = description,
            genres = genres,
            status = status,
            coverUrl = coverUrl ?: series.coverUrl,
        )
    }

    // =========================================================================
    // getChapterList
    // =========================================================================

    /**
     * Matches each chapter row in the full chapter list (`ul#idData`).
     *
     * HTML structure:
     * ```
     * <ul class="ul-list5" id="idData">
     *   <li>
     *     <span class="glyphicon glyphicon-book right-5"></span>
     *     <a href="/novel/{slug}/chapter-{n}" class="con"
     *        title="Chapter {n}: {title}">Chapter {n}: {title}</a>
     *   </li>
     *   ...
     * </ul>
     * ```
     *
     * The list is server-rendered, ascending from chapter 1 upward.
     * Per-row dates are not available, so [Chapter.uploadDate] is `null`.
     */
    override fun chapterListSelector(): String = "ul#idData a.con[href*=\"/chapter-\"]"

    /** Extracts a [Chapter] from a chapter-list row. */
    override fun chapterFromElement(el: Element, series: Series): Chapter {
        val url = el.absUrl("href").takeIf { it.isNotBlank() }
            ?: error("Chapter row missing valid URL")
        val name = el.text().trim()
        val number = parseChapterNumber(name)
        return Chapter(
            seriesUrl = series.url,
            sourceId = id,
            url = url,
            name = name,
            number = number,
            uploadDate = null,
        )
    }

    // =========================================================================
    // getChapterContent — novel text
    // =========================================================================

    /**
     * Extracts the cleaned chapter HTML from the chapter page.
     *
     * The chapter body lives inside `<div id="article">`, which contains
     * an `<h4>` title followed by `<p>` paragraph elements.  Inline ad
     * containers ([id^="pf-"], [id^="bg-ssp-]), `<script>` blocks, and
     * `<subtxt>` watermark nodes are removed before returning the HTML.
     */
    override fun chapterTextParse(doc: Document): String {
        val article = doc.selectFirst("div#article")
            ?: error("Chapter page missing div#article container")
        article.select("div[id^=\"pf-\"]").remove()
        article.select("div[id^=\"bg-ssp-\"]").remove()
        article.select("script").remove()
        article.select("subtxt").remove()
        return article.html().trim()
    }

    override fun chapterPagesParse(doc: Document): List<String> =
        error("Override for manhwa sources — FreeWebNovel is a novel source")

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Parses the status text from the series detail page.
     * The site uses "OnGoing" and "Completed".
     */
    private fun parseDetailStatus(text: String?): SeriesStatus = when {
        text.isNullOrBlank() -> SeriesStatus.UNKNOWN
        text.contains("completed", ignoreCase = true) -> SeriesStatus.COMPLETED
        text.contains("ongoing", ignoreCase = true) -> SeriesStatus.ONGOING
        else -> SeriesStatus.UNKNOWN
    }

    /**
     * Extracts the chapter number from a chapter name string.
     *
     * Supported formats:
     * - `"Chapter 876: Restless [3]"` → 876f
     * - `"Chapter 1: Prologue [1]"` → 1f
     * - `"Chapter 2233.2: Getting Married..."` → 2233.2f
     */
    private fun parseChapterNumber(name: String): Float {
        return Regex("""Chapter\s+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
    }
}

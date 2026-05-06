package com.opus.readerparser.sources.asurascans

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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Source plugin for [Asura Scans](https://asurascans.com/), a manhwa scanlation site.
 *
 * The site is built with Astro and serves server-rendered HTML for SEO.
 * Parser selectors extract data from the static HTML served on initial load.
 *
 * ### URL patterns
 * - Series listing : `/browse?page={N}`
 * - Series detail  : `/comics/{slug}-b6e039fe`
 * - Chapter        : `/comics/{slug}-b6e039fe/chapter/{N}`
 * - Search         : `/browse?search={query}&page={N}`
 */
class AsuraScans(
    client: HttpClient,
) : HtmlSource(client) {

    // =========================================================================
    // Source identity
    // =========================================================================

    override val id: Long = computeSourceId("AsuraScans", "en", ContentType.MANHWA)
    override val name: String = "AsuraScans"
    override val lang: String = "en"
    override val baseUrl: String = "https://asurascans.com"
    override val type: ContentType = ContentType.MANHWA

    // =========================================================================
    // Per-request headers (Cloudflare bypass)
    // =========================================================================

    /**
     * Fetches [url] with a realistic browser User-Agent and Accept headers
     * to bypass Cloudflare anti-bot protection.
     */
    override suspend fun fetchDocBody(url: String): String =
        client.get(url) {
            header("User-Agent", USER_AGENT)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            header("Accept-Language", "en-US,en;q=0.9")
        }.bodyAsText()

    // =========================================================================
    // getPopular — browse page
    // =========================================================================

    /** Browse page URL. Page 1 omits the `?page=` parameter. */
    override fun popularUrl(page: Int): String =
        if (page == 1) "$baseUrl/browse" else "$baseUrl/browse?page=$page"

    /** Server-rendered series cards inside the `#series-grid` container. */
    override fun popularSelector(): String = "#series-grid div.series-card"

    /**
     * Extracts a [Series] from a browse-page card.
     *
     * Card HTML structure:
     * ```
     * <div class="series-card ..." data-series-id="6063">
     *   <a href="/comics/{slug}-b6e039fe" class="block ...">
     *     <img src="{coverUrl}" alt="{title}" />
     *   </a>
     *   <div class="p-3">
     *     <a href="/comics/{slug}-b6e039fe">
     *       <h3 class="text-sm font-semibold ...">{title}</h3>
     *     </a>
     *     <div class="flex items-center gap-2 mt-2">
     *       <span>N Chapters</span>
     *       <span class="capitalize">{status}</span>
     *     </div>
     *   </div>
     * </div>
     * ```
     */
    override fun seriesFromElement(el: Element): Series {
        val coverLink = el.selectFirst("a[href^=\"/comics/\"]")
            ?: error("Series card missing cover link")
        val url = coverLink.absUrl("href")
        val title = el.selectFirst("h3")?.text()?.trim()
            ?: error("Series card missing title")
        val coverImg = coverLink.selectFirst("img")
        val coverUrl = coverImg?.absUrl("src")
        val status = el.selectFirst("span.capitalize")?.text()?.trim()

        return Series(
            sourceId = id,
            url = url,
            title = title,
            coverUrl = coverUrl,
            status = parseStatus(status),
            type = type,
        )
    }

    /**
     * Returns `a[aria-label="Next page"]` when a next page exists and is not disabled.
     * On the browse page, a disabled previous/next link has class `opacity-25 pointer-events-none`.
     */
    override fun popularNextPageSelector(): String? =
        "a[aria-label=\"Next page\"]:not(.opacity-25)"

    // =========================================================================
    // getLatest — homepage "Latest Updates" section
    // =========================================================================

    override suspend fun getLatest(page: Int): SeriesPage {
        if (page > 1) return SeriesPage(emptyList(), false)

        val doc = fetchDoc(baseUrl)
        val cards = doc.select(latestSelector())
        val series = cards.map(::latestSeriesFromElement)
        return SeriesPage(series, false)
    }

    /**
     * Matches each series row in the homepage "Latest Updates" grid.
     *
     * HTML structure:
     * ```
     * <div class="grid grid-cols-12 gap-2 py-4 px-2 border-b border-[#312f40]">
     *   <a href="/comics/{slug}-b6e039fe" class="col-span-4 ...">
     *     <img src="{coverUrl}" />
     *   </a>
     *   <div class="col-span-8 ... flex flex-col min-w-0">
     *     <a class="font-bold text-base line-clamp-1 ...">{title}</a>
     *     <!-- chapter links -->
     *   </div>
     * </div>
     * ```
     */
    private fun latestSelector(): String =
        "div.grid.grid-cols-12.gap-2.py-4.px-2.border-b"

    /** Extracts a [Series] from a homepage latest-updates row. */
    private fun latestSeriesFromElement(el: Element): Series {
        val titleLink = el.selectFirst("a.font-bold.text-base.line-clamp-1")
            ?: el.selectFirst("a[href^=\"/comics/\"]")
            ?: error("Latest series row missing title link")
        val url = titleLink.absUrl("href")
        val title = titleLink.text().trim()
        val coverImg = el.selectFirst("a.col-span-4 img")
            ?: el.selectFirst("img")
        val coverUrl = coverImg?.absUrl("src")

        return Series(
            sourceId = id,
            url = url,
            title = title,
            coverUrl = coverUrl,
            type = type,
        )
    }

    // =========================================================================
    // search
    // =========================================================================

    override fun searchUrl(query: String, page: Int, filters: FilterList): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val base = "$baseUrl/browse?search=$encoded"
        return if (page == 1) base else "$base&page=$page"
    }

    // =========================================================================
    // getSeriesDetails
    // =========================================================================

    /**
     * Enriches [series] with details from the series detail page.
     *
     * Key selectors on the detail page:
     * - **Title**       : `h1` inside `article`
     * - **Author**      : `a[href^="/browse?author="]`
     * - **Artist**      : `a[href^="/browse?artist="]`
     * - **Description** : `<meta name="description" content="...">`
     * - **Genres**      : `a[href^="/browse?genres="]` (desktop, hidden on mobile)
     * - **Status**      : `span.capitalize` inside the status card
     * - **Cover**       : `img` inside `#desktop-cover-container`
     */
    override fun seriesDetailsParse(doc: Document, series: Series): Series {
        val author = doc.selectFirst("a[href^=\"/browse?author=\"]")?.text()?.trim()
        val artist = doc.selectFirst("a[href^=\"/browse?artist=\"]")?.text()?.trim()
        val description = doc.selectFirst("meta[name=description]")
            ?.attr("content")
            ?.trim()
            ?.unescapeHtml()
        val genres = doc.select("a[href^=\"/browse?genres=\"]").map { it.text().trim() }
        val statusEl = doc.select("span.capitalize").firstOrNull {
            it.text().trim().lowercase() in STATUS_VALUES
        }
        val status = parseStatus(statusEl?.text()?.trim())
        val coverUrl = doc.selectFirst("#desktop-cover-container img")?.absUrl("src")

        return series.copy(
            author = author,
            artist = artist,
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
     * Matches chapter rows on the series detail page.
     *
     * Each chapter row is an `<a>` tag with `data-astro-prefetch="hover"`:
     * ```html
     * <a href="/comics/{slug}-b6e039fe/chapter/108"
     *    data-astro-prefetch="hover"
     *    class="group flex items-center justify-between px-4 py-4 ...">
     *   <span class="font-medium ...">Chapter <!-- -->108</span>
     *   <span class="text-sm text-white/40">22 hours ago</span>
     * </a>
     * ```
     */
    override fun chapterListSelector(): String =
        "a[data-astro-prefetch=\"hover\"][href*=\"/chapter/\"]"

    /** Extracts a [Chapter] from a chapter list row element. */
    protected override fun chapterFromElement(el: Element, series: Series): Chapter {
        val url = el.absUrl("href")
            .takeIf { it.isNotBlank() }
            ?: error("Chapter row missing valid URL")
        val nameEl = el.selectFirst("span.font-medium")
        val name = nameEl?.text()?.trim() ?: el.text().trim()
        val number = parseChapterNumber(name)
        val dateEl = el.selectFirst("span.text-sm[class*=\"text-white/40\"]")
        val uploadDate = dateEl?.text()?.trim()?.let { parseRelativeDate(it) }

        return Chapter(
            seriesUrl = series.url,
            sourceId = id,
            url = url,
            name = name,
            number = number,
            uploadDate = uploadDate,
        )
    }

    // =========================================================================
    // getChapterContent (manhwa — override chapterPagesParse)
    // =========================================================================

    /**
     * Extracts page image URLs in reading order from a chapter page.
     *
     * Images are in `<div data-page="N">` containers:
     * ```html
     * <div data-page="0" class="w-full">
     *   <img src="https://cdn.asurascans.com/asura-images/chapters/{slug}/{chapter}/hash.webp" />
     * </div>
     * ```
     */
    override fun chapterPagesParse(doc: Document): List<String> =
        doc.select("div[data-page] img").map { it.absUrl("src") }

    // =========================================================================
    // Unused — novel sources only
    // =========================================================================

    override fun chapterTextParse(doc: Document): String =
        error("Override for novel sources — AsuraScans is a manhwa source")

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun parseStatus(raw: String?): SeriesStatus = when (raw?.lowercase()?.trim()) {
        "ongoing" -> SeriesStatus.ONGOING
        "completed" -> SeriesStatus.COMPLETED
        "hiatus" -> SeriesStatus.HIATUS
        "cancelled" -> SeriesStatus.CANCELLED
        else -> SeriesStatus.UNKNOWN
    }

    private fun parseChapterNumber(name: String): Float {
        // "Chapter 108" → 108, "Chapter 12.5" → 12.5
        return Regex("""Chapter\s+(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            .find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
    }

    /**
     * Attempts to parse a relative time string into epoch millis.
     * Handles:
     * - "X hours ago", "X days ago", "X weeks ago"
     * - "last week"
     * - Absolute dates: "Apr 4, 2026", "Mar 28, 2026"
     */
    private fun parseRelativeDate(raw: String): Long? {
        val trimmed = raw.trim()

        // Absolute date: "Apr 4, 2026"
        try {
            val localDate = LocalDate.parse(trimmed, ABSOLUTE_FORMATTER)
            return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            // fall through to relative parsing
        }

        // "last week"
        if (trimmed.equals("last week", ignoreCase = true)) {
            return Instant.now().minusSeconds(7 * 24 * 3600).toEpochMilli()
        }

        // "X hours ago", "X days ago", "X weeks ago"
        val match = Regex("""(\d+)\s*(hour|day|week|month|year)s?\s+ago""", RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (match != null) {
            val amount = match.groupValues[1].toLongOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            val seconds = when (unit) {
                "hour"  -> amount * 3600
                "day"   -> amount * 86400
                "week"  -> amount * 604800
                "month" -> amount * 2592000L
                "year"  -> amount * 31536000L
                else    -> return null
            }
            return Instant.now().minusSeconds(seconds).toEpochMilli()
        }

        return null
    }

    /** Decodes HTML entities in a raw description string. */
    private fun String.unescapeHtml(): String =
        this.replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")

    companion object {
        /** Browser-like User-Agent to bypass Cloudflare anti-bot protection. */
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                " (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

        private val ABSOLUTE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

        private val STATUS_VALUES: Set<String> =
            setOf("ongoing", "completed", "hiatus", "cancelled")
    }
}

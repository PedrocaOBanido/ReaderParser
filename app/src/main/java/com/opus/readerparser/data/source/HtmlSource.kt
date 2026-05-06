package com.opus.readerparser.data.source

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Abstract base class for HTML-based source plugins.
 *
 * Wires Ktor HTTP + Jsoup HTML parsing so that concrete sources only write
 * site-specific selectors and extraction logic.
 *
 * ### What concrete sources must override
 * - [popularUrl], [popularSelector], [seriesFromElement], [popularNextPageSelector]
 * - [searchUrl]
 * - [seriesDetailsParse]
 * - [chapterListSelector], [chapterFromElement]
 * - [getLatest] (no sensible default — latest-update pages differ per site)
 * - One of [chapterTextParse] (for novels) or [chapterPagesParse] (for manhwa)
 *
 * ### What concrete sources may override
 * - [searchSelector] (defaults to [popularSelector])
 * - [searchSeriesFromElement] (defaults to [seriesFromElement])
 * - [searchNextPageSelector] (defaults to [popularNextPageSelector])
 * - [supports] (defaults to `true`)
 */
abstract class HtmlSource(
    protected val client: HttpClient,
) : Source {

    // =========================================================================
    // Shared utility
    // =========================================================================

    /**
     * Fetches [url] via Ktor and returns the raw response body as a [String].
     * Throws on any HTTP error — there is no catch here.
     *
     * Override to add per-source headers (e.g., User-Agent for Cloudflare bypass).
     * Favor overriding this over [fetchDoc] — the base class handles Jsoup parsing.
     */
    protected open suspend fun fetchDocBody(url: String): String =
        client.get(url).bodyAsText()

    /**
     * Fetches [url] via Ktor and parses the response body as a Jsoup [Document].
     * Calls [fetchDocBody] internally, so overriding [fetchDocBody] is sufficient to
     * customize HTTP headers without duplicating Jsoup wiring.
     *
     * Throws on any HTTP or parse error — there is no catch here.
     */
    protected open suspend fun fetchDoc(url: String): Document =
        Jsoup.parse(fetchDocBody(url), url)

    // =========================================================================
    // Abstract — must be overridden
    // =========================================================================

    /** Returns the URL for the popular-series page at [page] (1-based). */
    protected abstract fun popularUrl(page: Int): String

    /** CSS selector that matches each series card/row on the popular page. */
    protected abstract fun popularSelector(): String

    /** Extracts a [Series] from one matched [Element]. */
    protected abstract fun seriesFromElement(el: Element): Series

    /**
     * CSS selector for the "next page" link on the popular page, or `null`
     * if the source does not expose a next-page indicator.
     */
    protected abstract fun popularNextPageSelector(): String?

    /** Returns the URL for a search-results page. */
    protected abstract fun searchUrl(query: String, page: Int, filters: FilterList): String

    /**
     * Parses the series-detail page [doc] and merges any extra fields
     * (description, genres, status, etc.) into [series], returning the
     * enriched copy.
     */
    protected abstract fun seriesDetailsParse(doc: Document, series: Series): Series

    /** CSS selector that matches each chapter row on the series page. */
    protected abstract fun chapterListSelector(): String

    /** Extracts a [Chapter] from one matched chapter-row [Element]. */
    protected abstract fun chapterFromElement(el: Element, series: Series): Chapter

    // =========================================================================
    // Open — may be overridden
    // =========================================================================

    /**
     * CSS selector for search results. Defaults to [popularSelector] because
     * many sites reuse the same card layout.
     */
    protected open fun searchSelector(): String = popularSelector()

    /**
     * Extracts a [Series] from a search-result element.
     * Defaults to [seriesFromElement].
     */
    protected open fun searchSeriesFromElement(el: Element): Series = seriesFromElement(el)

    /**
     * CSS selector for the "next page" link on search results, or `null`.
     * Defaults to [popularNextPageSelector].
     */
    protected open fun searchNextPageSelector(): String? = popularNextPageSelector()

    /**
     * Extracts the chapter HTML body from a chapter page.
     *
     * **Must be overridden for novel sources.**
     * The default throws [IllegalStateException] — calling this on a
     * manhwa source (or on a novel source that forgot to override) is a bug.
     */
    protected open fun chapterTextParse(doc: Document): String =
        error("Override for novel sources")

    /**
     * Extracts page image URLs in reading order from a manhwa chapter page.
     *
     * **Must be overridden for manhwa sources.**
     * The default throws [IllegalStateException] — calling this on a
     * novel source (or on a manhwa source that forgot to override) is a bug.
     */
    protected open fun chapterPagesParse(doc: Document): List<String> =
        error("Override for manhwa sources")

    // =========================================================================
    // Source interface — default implementations
    // =========================================================================

    override suspend fun getPopular(page: Int): SeriesPage {
        val doc = fetchDoc(popularUrl(page))
        val series = doc.select(popularSelector()).map(::seriesFromElement)
        val next = popularNextPageSelector()?.let { doc.selectFirst(it) != null } ?: false
        return SeriesPage(series, next)
    }

    override suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage {
        val doc = fetchDoc(searchUrl(query, page, filters))
        val series = doc.select(searchSelector()).map(::searchSeriesFromElement)
        val next = searchNextPageSelector()?.let { doc.selectFirst(it) != null } ?: false
        return SeriesPage(series, next)
    }

    override suspend fun getSeriesDetails(series: Series): Series =
        seriesDetailsParse(fetchDoc(series.url), series)

    override suspend fun getChapterList(series: Series): List<Chapter> =
        fetchDoc(series.url)
            .select(chapterListSelector())
            .map { chapterFromElement(it, series) }

    override suspend fun getChapterContent(chapter: Chapter): ChapterContent {
        val doc = fetchDoc(chapter.url)
        return when (type) {
            ContentType.NOVEL -> ChapterContent.Text(chapterTextParse(doc))
            ContentType.MANHWA -> ChapterContent.Pages(chapterPagesParse(doc))
        }
    }
}

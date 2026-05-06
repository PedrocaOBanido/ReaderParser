package com.opus.readerparser.sources.asurascans

import com.opus.readerparser.core.util.computeSourceId
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesStatus
import com.opus.readerparser.testutil.mockHttpClient
import com.opus.readerparser.testutil.readFixture
import com.opus.readerparser.testutil.respondHtml
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for [AsuraScans] using [mockHttpClient] + HTML fixtures.
 *
 * Fixtures live under `src/test/resources/fixtures/asurascans/`:
 * - `popular.html`  — browse listing page (`/browse?page=1`)
 * - `search.html`   — search results page (`/browse?search=...`)
 * - `series_detail.html` — series detail page
 * - `chapter.html`  — chapter reader page
 *
 * Each fixture is a **placeholder** — they contain representative HTML
 * snippets derived from the live site on 2026-05-05. If the site changes
 * its markup, these fixtures must be updated from live page source.
 */
class AsuraScansTest {

    // =========================================================================
    // Identity
    // =========================================================================

    @Test
    fun `id is computed from name + lang + type`() = runTest {
        val source = asuraScans("<html></html>")

        assertEquals(
            computeSourceId("AsuraScans", "en", ContentType.MANHWA),
            source.id,
        )
    }

    @Test
    fun `type is MANHWA`() = runTest {
        val source = asuraScans("<html></html>")
        assertEquals(ContentType.MANHWA, source.type)
    }

    @Test
    fun `name is AsuraScans`() = runTest {
        val source = asuraScans("<html></html>")
        assertEquals("AsuraScans", source.name)
    }

    @Test
    fun `lang is en`() = runTest {
        val source = asuraScans("<html></html>")
        assertEquals("en", source.lang)
    }

    @Test
    fun `baseUrl is asurascans dot com`() = runTest {
        val source = asuraScans("<html></html>")
        assertEquals("https://asurascans.com", source.baseUrl)
    }

    // =========================================================================
    // getPopular — browse page fixtures
    // =========================================================================

    @Test
    fun `getPopular parses series cards from browse page`() = runTest {
        val html = readFixture("fixtures/asurascans/popular.html")
        val source = asuraScans(html)

        val result = source.getPopular(1)

        assertEquals(3, result.series.size)

        val first = result.series[0]
        assertEquals("Eternally Regressing Knight", first.title)
        assertEquals(source.id, first.sourceId)
        assertTrue(first.url.contains("/comics/eternally-regressing-knight-b6e039fe"))
        assertNotNull(first.coverUrl)
        assertTrue(first.coverUrl!!.contains("cdn.asurascans.com"))
        assertEquals(SeriesStatus.ONGOING, first.status)
    }

    @Test
    fun `getPopular hasNextPage true when Next page link exists`() = runTest {
        val html = readFixture("fixtures/asurascans/popular.html")
        val source = asuraScans(html)

        val result = source.getPopular(1)
        assertTrue("Fixture has Next page link", result.hasNextPage)
    }

    // =========================================================================
    // getLatest — homepage latest updates
    // =========================================================================

    @Test
    fun `getLatest page 1 parses series from latest updates grid`() = runTest {
        val html = readFixture("fixtures/asurascans/latest.html")
        val source = asuraScans(html)

        val result = source.getLatest(1)

        assertFalse("Homepage has no pagination", result.hasNextPage)
        assertEquals(2, result.series.size)

        assertEquals("Eternally Regressing Knight", result.series[0].title)
        assertEquals(source.id, result.series[0].sourceId)
        assertTrue(result.series[0].url.endsWith("-b6e039fe"))
        assertNotNull(result.series[0].coverUrl)
        assertTrue(result.series[0].coverUrl!!.contains("cdn.asurascans.com"))

        assertEquals("Solo Max-Level Newbie", result.series[1].title)
    }

    @Test
    fun `getLatest page 2 returns empty SeriesPage`() = runTest {
        val source = asuraScans("<html></html>")
        val result = source.getLatest(2)
        assertTrue(result.series.isEmpty())
        assertFalse(result.hasNextPage)
    }

    // =========================================================================
    // search
    // =========================================================================

    @Test
    fun `search parses results using same card structure as popular`() = runTest {
        val html = readFixture("fixtures/asurascans/search.html")
        val source = asuraScans(html)

        val result = source.search("solo", 1, FilterList())

        assertEquals(2, result.series.size)
        assertEquals("Solo Max-Level Newbie", result.series[0].title)
        assertEquals("Emperor of Solo Play", result.series[1].title)
    }

    // =========================================================================
    // getSeriesDetails
    // =========================================================================

    @Test
    fun `getSeriesDetails enriches with author artist description genres status`() = runTest {
        val html = readFixture("fixtures/asurascans/series_detail.html")
        val source = asuraScans(html)

        val input = Series(
            sourceId = source.id,
            url = "https://asurascans.com/comics/eternally-regressing-knight-b6e039fe",
            title = "Eternally Regressing Knight",
            type = ContentType.MANHWA,
        )
        val result = source.getSeriesDetails(input)

        assertEquals("Kanara", result.author)
        assertEquals("JQ Comics", result.artist)
        assertEquals("Eternally Regressing Knight", result.title) // preserved
        assertEquals(SeriesStatus.ONGOING, result.status)
        assertEquals(4, result.genres.size)
        assertTrue(result.genres.contains("Action"))
        assertTrue(result.genres.contains("Fantasy"))
        assertNotNull("Description should be present", result.description)
        assertTrue(result.description!!.contains("genius"))
        assertNotNull("Cover URL should be present", result.coverUrl)
    }

    // =========================================================================
    // getChapterList
    // =========================================================================

    @Test
    fun `getChapterList parses chapter rows from series detail`() = runTest {
        val html = readFixture("fixtures/asurascans/series_detail.html")
        val source = asuraScans(html)

        val series = Series(
            sourceId = source.id,
            url = "https://asurascans.com/comics/eternally-regressing-knight-b6e039fe",
            title = "Eternally Regressing Knight",
            type = ContentType.MANHWA,
        )
        val chapters = source.getChapterList(series)

        assertEquals(4, chapters.size)

        // Chapter 108 (latest, first in list)
        assertEquals("Chapter 108", chapters[0].name)
        assertEquals(108f, chapters[0].number)
        assertTrue(chapters[0].url.contains("/chapter/108"))
        assertEquals(series.url, chapters[0].seriesUrl)
        assertEquals(source.id, chapters[0].sourceId)

        // Chapter 107 - relative date "last week"
        assertEquals("Chapter 107", chapters[1].name)
        assertEquals(107f, chapters[1].number)
        assertNotNull("'last week' should parse to a date", chapters[1].uploadDate)

        // Chapter 106 - absolute date "Apr 4, 2026"
        assertEquals("Chapter 106", chapters[2].name)
        assertEquals(106f, chapters[2].number)
        assertNotNull("Absolute date should parse", chapters[2].uploadDate)

        // Chapter 105 - relative date "3 weeks ago"
        assertEquals("Chapter 105", chapters[3].name)
        assertEquals(105f, chapters[3].number)
        assertNotNull("'3 weeks ago' should parse to a date", chapters[3].uploadDate)
    }

    // =========================================================================
    // getChapterContent — manhwa pages
    // =========================================================================

    @Test
    fun `getChapterContent returns Pages with images in reading order`() = runTest {
        val html = readFixture("fixtures/asurascans/chapter.html")
        val source = asuraScans(html)

        val chapter = com.opus.readerparser.domain.model.Chapter(
            seriesUrl = "https://asurascans.com/comics/eternally-regressing-knight-b6e039fe",
            sourceId = source.id,
            url = "https://asurascans.com/comics/eternally-regressing-knight-b6e039fe/chapter/1",
            name = "Chapter 1",
            number = 1f,
        )
        val content = source.getChapterContent(chapter)

        assertTrue("Should be Pages", content is ChapterContent.Pages)
        val pages = (content as ChapterContent.Pages).imageUrls

        assertEquals(5, pages.size)
        assertTrue("All URLs should be absolute", pages.all { it.startsWith("https://") })
        assertTrue("Pages should contain CDN URLs", pages.all { it.contains("cdn.asurascans.com") })
        assertTrue(pages[0].contains("c4aabc.webp"))
        assertTrue(pages[4].contains("a509c9.webp"))
    }

    // =========================================================================
    // Error paths — throws on missing required elements
    // =========================================================================

    @Test
    fun `seriesFromElement throws when cover link is missing`() = runTest {
        val html = """<div id="series-grid">
            <div class="series-card">
              <h3>No Cover Link Title</h3>
            </div>
          </div>""".trimIndent()
        val source = asuraScans(html)

        try {
            source.getPopular(1)
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Series card missing cover link"))
        }
    }

    @Test
    fun `chapterFromElement throws when URL cannot be resolved`() = runTest {
        val html = """
            <html><body>
              <a data-astro-prefetch="hover" href="/comics/slug-b6e039fe/chapter/1">Chapter 1</a>
            </body></html>
        """.trimIndent()
        val source = asuraScans(html)
        // Invalid base URL with a space forces Jsoup's absUrl to fail resolution
        // and return "", which triggers the error inside chapterFromElement.
        val series = Series(
            sourceId = source.id,
            url = "not a url",
            title = "Test",
            type = ContentType.MANHWA,
        )

        try {
            source.getChapterList(series)
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Chapter row missing valid URL"))
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun asuraScans(html: String): AsuraScans {
        val client = mockHttpClient { respondHtml(html) }
        return AsuraScans(client)
    }
}

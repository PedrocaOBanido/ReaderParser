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
import org.junit.Assert.assertNotEquals
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
    fun `getPopular page 1 and 2 request distinct urls and parse distinct content`() = runTest {
        val page1Html = """
            <html><body>
              <div id="series-grid">
                <div class="series-card">
                  <a href="/comics/page-one-b6e039fe"><img src="https://cdn.asurascans.com/page-one.webp"/></a>
                  <div class="p-3">
                    <a href="/comics/page-one-b6e039fe"><h3>Page One</h3></a>
                    <div><span>10 Chapters</span><span class="capitalize">Ongoing</span></div>
                  </div>
                </div>
              </div>
              <a href="/browse?page=2" aria-label="Next page">Next</a>
            </body></html>
        """.trimIndent()
        val page2Html = """
            <html><body>
              <div id="series-grid">
                <div class="series-card">
                  <a href="/comics/page-two-b6e039fe"><img src="https://cdn.asurascans.com/page-two.webp"/></a>
                  <div class="p-3">
                    <a href="/comics/page-two-b6e039fe"><h3>Page Two</h3></a>
                    <div><span>20 Chapters</span><span class="capitalize">Completed</span></div>
                  </div>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val requestedUrls = mutableListOf<String>()
        val source = AsuraScans(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                when {
                    request.url.encodedPath == "/browse" && request.url.parameters["page"] == null -> respondHtml(page1Html)
                    request.url.encodedPath == "/browse" && request.url.parameters["page"] == "2" -> respondHtml(page2Html)
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )

        val page1 = source.getPopular(1)
        val page2 = source.getPopular(2)

        assertEquals(
            listOf(
                "https://asurascans.com/browse",
                "https://asurascans.com/browse?page=2",
            ),
            requestedUrls,
        )
        assertEquals("Page One", page1.series.single().title)
        assertEquals("Page Two", page2.series.single().title)
        assertNotEquals(page1.series.single().title, page2.series.single().title)
        assertTrue(page1.hasNextPage)
        assertFalse(page2.hasNextPage)
    }

    @Test
    fun `getPopular hasNextPage true when Next page link exists`() = runTest {
        val html = readFixture("fixtures/asurascans/popular.html")
        val source = asuraScans(html)

        val result = source.getPopular(1)

        assertEquals(3, result.series.size)
        assertEquals("Eternally Regressing Knight", result.series[0].title)
        assertEquals(source.id, result.series[0].sourceId)
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

    @Test
    fun `getPopular and getLatest use distinct urls and return distinct content`() = runTest {
        val popularHtml = """
            <html><body>
              <div id="series-grid">
                <div class="series-card">
                  <a href="/comics/popular-series-b6e039fe"><img src="https://cdn.asurascans.com/pop.webp"/></a>
                  <div class="p-3">
                    <a href="/comics/popular-series-b6e039fe"><h3>Popular Series</h3></a>
                    <div><span>10 Chapters</span><span class="capitalize">Ongoing</span></div>
                  </div>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val latestHtml = """
            <html><body>
              <div class="grid grid-cols-12 gap-2 py-4 px-2 border-b border-[#312f40]">
                <a href="/comics/latest-series-b6e039fe" class="col-span-4">
                  <img src="https://cdn.asurascans.com/latest.webp" />
                </a>
                <div class="col-span-8 flex flex-col min-w-0">
                  <a href="/comics/latest-series-b6e039fe" class="font-bold text-base line-clamp-1">Latest Series</a>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val requestedUrls = mutableListOf<String>()
        val source = AsuraScans(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                when {
                    request.url.encodedPath == "/browse" -> respondHtml(popularHtml)
                    request.url.encodedPath.isEmpty() -> respondHtml(latestHtml)
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )

        val popular = source.getPopular(1)
        val latest = source.getLatest(1)

        assertEquals(listOf("https://asurascans.com/browse", "https://asurascans.com"), requestedUrls.map { it.removeSuffix("/") })
        assertTrue("Popular series URL should not be blank", popular.series.single().url.isNotBlank())
        assertTrue("Latest series URL should not be blank", latest.series.single().url.isNotBlank())
        assertEquals("Popular Series", popular.series.single().title)
        assertEquals("Latest Series", latest.series.single().title)
        assertNotEquals(popular.series.single().url, latest.series.single().url)
    }

    // =========================================================================
    // search
    // =========================================================================

    @Test
    fun `search page 1 and 2 request distinct urls and parse distinct content`() = runTest {
        val page1Html = """
            <html><body>
              <div id="series-grid">
                <div class="series-card">
                  <a href="/comics/search-one-b6e039fe"><img src="https://cdn.asurascans.com/search-one.webp"/></a>
                  <div class="p-3">
                    <a href="/comics/search-one-b6e039fe"><h3>Search One</h3></a>
                    <div><span>7 Chapters</span><span class="capitalize">Ongoing</span></div>
                  </div>
                </div>
              </div>
              <a href="/browse?search=solo%20leveling&page=2" aria-label="Next page">Next</a>
            </body></html>
        """.trimIndent()
        val page2Html = """
            <html><body>
              <div id="series-grid">
                <div class="series-card">
                  <a href="/comics/search-two-b6e039fe"><img src="https://cdn.asurascans.com/search-two.webp"/></a>
                  <div class="p-3">
                    <a href="/comics/search-two-b6e039fe"><h3>Search Two</h3></a>
                    <div><span>12 Chapters</span><span class="capitalize">Completed</span></div>
                  </div>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val requestedUrls = mutableListOf<String>()
        val source = AsuraScans(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                when {
                    request.url.encodedPath == "/browse" && request.url.parameters["search"] == "solo leveling" && request.url.parameters["page"] == null -> respondHtml(page1Html)
                    request.url.encodedPath == "/browse" && request.url.parameters["search"] == "solo leveling" && request.url.parameters["page"] == "2" -> respondHtml(page2Html)
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )

        val page1 = source.search("solo leveling", 1, FilterList())
        val page2 = source.search("solo leveling", 2, FilterList())

        assertEquals(
            listOf(
                "https://asurascans.com/browse?search=solo%20leveling",
                "https://asurascans.com/browse?search=solo%20leveling&page=2",
            ),
            requestedUrls,
        )
        assertEquals("Search One", page1.series.single().title)
        assertEquals("Search Two", page2.series.single().title)
        assertNotEquals(page1.series.single().title, page2.series.single().title)
        assertTrue(page1.hasNextPage)
        assertFalse(page2.hasNextPage)
    }

    @Test
    fun `search parses fixture-backed results`() = runTest {
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

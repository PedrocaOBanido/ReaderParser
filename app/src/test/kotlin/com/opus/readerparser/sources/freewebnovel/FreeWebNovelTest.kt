package com.opus.readerparser.sources.freewebnovel

import com.opus.readerparser.core.util.computeSourceId
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesStatus
import com.opus.readerparser.testutil.mockHttpClient
import com.opus.readerparser.testutil.readFixture
import com.opus.readerparser.testutil.respondHtml
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for [FreeWebNovel] using [mockHttpClient] + HTML fixtures.
 *
 * Fixtures live under `src/test/resources/fixtures/freewebnovel/`:
 * - `popular.html` — most-popular listing page (`/sort/most-popular`)
 * - `latest.html`  — latest-release page (`/sort/latest-release`)
 * - `search.html`  — search results page (`/search?searchkey=...`)
 * - `series.html`  — series detail page (`/novel/advent-of-the-three-calamities`)
 * - `chapter.html` — chapter reader page (`/novel/.../chapter-876`)
 *
 * Each fixture is a **live-backed capture** derived from the site on 2026-05-27.
 * If the site changes its markup, these fixtures must be updated from live page source.
 * The `search.html` fixture shares the same card markup as `popular.html`.
 */
class FreeWebNovelTest {

    // =========================================================================
    // Identity
    // =========================================================================

    @Test
    fun `id is computed from name + lang + type`() = runTest {
        val source = freeWebNovel("<html></html>")

        assertEquals(
            computeSourceId("FreeWebNovel", "en", ContentType.NOVEL),
            source.id,
        )
    }

    @Test
    fun `type is NOVEL`() = runTest {
        val source = freeWebNovel("<html></html>")
        assertEquals(ContentType.NOVEL, source.type)
    }

    @Test
    fun `name is FreeWebNovel`() = runTest {
        val source = freeWebNovel("<html></html>")
        assertEquals("FreeWebNovel", source.name)
    }

    @Test
    fun `lang is en`() = runTest {
        val source = freeWebNovel("<html></html>")
        assertEquals("en", source.lang)
    }

    @Test
    fun `baseUrl is freewebnovel dot com`() = runTest {
        val source = freeWebNovel("<html></html>")
        assertEquals("https://freewebnovel.com", source.baseUrl)
    }

    // =========================================================================
    // getPopular — most-popular listing
    // =========================================================================

    @Test
    fun `getPopular parses series cards from popular page`() = runTest {
        val html = readFixture("fixtures/freewebnovel/popular.html")
        val source = freeWebNovel(html)

        val result = source.getPopular(1)

        assertEquals(50, result.series.size)

        val first = result.series[0]
        assertEquals("Invincible", first.title)
        assertEquals(source.id, first.sourceId)
        assertTrue(first.url.contains("/novel/invincible-novel"))
        assertNotNull(first.coverUrl)
        assertTrue(first.coverUrl!!.contains("files/article/image"))
        assertEquals(SeriesStatus.COMPLETED, first.status)

        // Second series should be UNKNOWN (no "Full" marker)
        assertEquals(SeriesStatus.UNKNOWN, result.series[1].status)
    }

    @Test
    fun `getPopular hasNextPage false when no pagination`() = runTest {
        val html = readFixture("fixtures/freewebnovel/popular.html")
        val source = freeWebNovel(html)

        val result = source.getPopular(1)

        assertFalse("Popular page has no pagination", result.hasNextPage)
    }

    // =========================================================================
    // getLatest — latest-release listing
    // =========================================================================

    @Test
    fun `getLatest page 1 parses series and hasNextPage true`() = runTest {
        val html = readFixture("fixtures/freewebnovel/latest.html")
        val source = freeWebNovel(html)

        val result = source.getLatest(1)

        assertEquals(20, result.series.size)
        assertTrue("Latest fixture has >> pagination link", result.hasNextPage)

        val first = result.series[0]
        assertEquals(source.id, first.sourceId)
        assertNotNull(first.title)
        assertNotNull(first.coverUrl)
        assertTrue(first.url.contains("/novel/"))
    }

    @Test
    fun `getLatest page 1 and 2 request distinct urls and parse distinct content`() = runTest {
        val page1Html = """
            <html><body>
              <div class="ul-list1 ul-list1-2 ss-custom">
                <div class="li-row">
                  <div class="li"><div class="con">
                    <div class="pic"><a href="/novel/page-one"><img src="https://cdn.freewebnovel.com/page-one.jpg"/></a></div>
                    <div class="txt"><h3 class="tit"><a href="/novel/page-one">Page One</a></h3></div>
                  </div></div>
                </div>
              </div>
              <div class="pages"><a href="/sort/latest-release/2">&gt;&gt;</a></div>
            </body></html>
        """.trimIndent()
        val page2Html = """
            <html><body>
              <div class="ul-list1 ul-list1-2 ss-custom">
                <div class="li-row">
                  <div class="li"><div class="con">
                    <div class="pic"><a href="/novel/page-two"><img src="https://cdn.freewebnovel.com/page-two.jpg"/></a></div>
                    <div class="txt"><h3 class="tit"><a href="/novel/page-two">Page Two</a></h3></div>
                  </div></div>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val requestedUrls = mutableListOf<String>()
        val source = FreeWebNovel(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                when (request.url.toString()) {
                    "https://freewebnovel.com/sort/latest-release" -> respondHtml(page1Html)
                    "https://freewebnovel.com/sort/latest-release/2" -> respondHtml(page2Html)
                    else -> error("Unexpected request: ${request.url}")
                }
            }
        )

        val page1 = source.getLatest(1)
        val page2 = source.getLatest(2)

        assertEquals(
            listOf(
                "https://freewebnovel.com/sort/latest-release",
                "https://freewebnovel.com/sort/latest-release/2",
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
    fun `getLatest last page returns hasNextPage false`() = runTest {
        val html = readFixture("fixtures/freewebnovel/latest_last.html")
        val source = freeWebNovel(html)

        val result = source.getLatest(693)

        assertTrue("Terminal page should have series", result.series.isNotEmpty())
        assertFalse("Terminal page should have no next page", result.hasNextPage)
    }

    // =========================================================================
    // search
    // =========================================================================

    @Test
    fun `search remains single-page across page 1 and page 2`() = runTest {
        val html = readFixture("fixtures/freewebnovel/search.html")
        val requestedUrls = mutableListOf<String>()
        val source = FreeWebNovel(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                respondHtml(html)
            }
        )

        val page1 = source.search("shadow", 1, FilterList())
        val page2 = source.search("shadow", 2, FilterList())

        assertEquals(
            listOf(
                "https://freewebnovel.com/search?searchkey=shadow",
                "https://freewebnovel.com/search?searchkey=shadow",
            ),
            requestedUrls,
        )
        assertTrue(page1.series.isNotEmpty())
        assertEquals(page1.series.map { it.title }, page2.series.map { it.title })
        assertFalse(page1.hasNextPage)
        assertFalse(page2.hasNextPage)
    }

    // =========================================================================
    // getSeriesDetails
    // =========================================================================

    @Test
    fun `getSeriesDetails enriches with author description genres status cover`() = runTest {
        val html = readFixture("fixtures/freewebnovel/series.html")
        val source = freeWebNovel(html)

        val input = Series(
            sourceId = source.id,
            url = "https://freewebnovel.com/novel/advent-of-the-three-calamities",
            title = "Advent of the Three Calamities",
            type = ContentType.NOVEL,
        )
        val result = source.getSeriesDetails(input)

        assertEquals("Entrail_JI", result.author)
        assertEquals("Advent of the Three Calamities", result.title) // preserved
        assertEquals(SeriesStatus.ONGOING, result.status)
        assertEquals(5, result.genres.size)
        assertTrue(result.genres.contains("Fantasy"))
        assertTrue(result.genres.contains("Romance"))
        assertTrue(result.genres.contains("Action"))
        assertTrue(result.genres.contains("Comedy"))
        assertTrue(result.genres.contains("Slice of Life"))
        assertNotNull("Description should be present", result.description)
        assertTrue(result.description!!.contains("Emotions are like a drug"))
        assertNotNull("Cover URL should be present", result.coverUrl)
        assertTrue(result.coverUrl!!.contains("files/article/image"))
    }

    @Test
    fun `getSeriesDetails returns COMPLETED status from completed series page`() = runTest {
        val html = readFixture("fixtures/freewebnovel/series_completed.html")
        val source = freeWebNovel(html)

        val input = Series(
            sourceId = source.id,
            url = "https://freewebnovel.com/novel/invincible-novel",
            title = "Invincible",
            type = ContentType.NOVEL,
        )
        val result = source.getSeriesDetails(input)

        // The completed series detail page uses .s1.s3 instead of .s1.s2
        assertEquals(SeriesStatus.COMPLETED, result.status)
        assertEquals("Invincible", result.title)
        assertNotNull(result.author)
        assertNotNull(result.description)
    }

    // =========================================================================
    // getChapterList
    // =========================================================================

    @Test
    fun `getChapterList parses 876 chapter rows from series detail`() = runTest {
        val html = readFixture("fixtures/freewebnovel/series.html")
        val source = freeWebNovel(html)

        val series = Series(
            sourceId = source.id,
            url = "https://freewebnovel.com/novel/advent-of-the-three-calamities",
            title = "Advent of the Three Calamities",
            type = ContentType.NOVEL,
        )
        val chapters = source.getChapterList(series)

        assertEquals(876, chapters.size)

        // Chapter 1
        assertEquals("Chapter 1: Prologue [1]", chapters[0].name)
        assertEquals(1f, chapters[0].number)
        assertTrue(chapters[0].url.contains("/chapter-1"))
        assertEquals(series.url, chapters[0].seriesUrl)
        assertEquals(source.id, chapters[0].sourceId)

        // Chapter 876 (last)
        val last = chapters[875]
        assertEquals("Chapter 876: Restless [3]", last.name)
        assertEquals(876f, last.number)
        assertTrue(last.url.contains("/chapter-876"))

        // Middle chapter — should have null uploadDate (no dates in the list)
        assertNull(chapters[400].uploadDate)
    }

    @Test
    fun `getChapterList fetches AJAX pages when total chapters exceed page size`() = runTest {
        val page1Html = """
            <html><body>
              <ul class="ul-list5" id="idData">
                <li><span class="glyphicon glyphicon-book right-5"></span><a href="/novel/test-pagination/chapter-1" title="Chapter 1: One" class="con">Chapter 1: One</a></li>
                <li><span class="glyphicon glyphicon-book right-5"></span><a href="/novel/test-pagination/chapter-2" title="Chapter 2: Two" class="con">Chapter 2: Two</a></li>
              </ul>
            </body></html>
        """.trimIndent()
        val page2Json = """{"code":200,"html":"<li><span class=\"glyphicon glyphicon-book right-5\"><\/span><a href=\"\/novel\/test-pagination\/chapter-2\" title=\"Chapter 2: Two (dup)\" class=\"con\">Chapter 2: Two (dup)<\/a><\/li><li><span class=\"glyphicon glyphicon-book right-5\"><\/span><a href=\"\/novel\/test-pagination\/chapter-3\" title=\"Chapter 3: Three\" class=\"con\">Chapter 3: Three<\/a><\/li>","page":2,"pageSize":40,"totalPage":2,"totalChapters":80}"""

        val requestedUrls = mutableListOf<String>()
        val series = Series(
            sourceId = computeSourceId("FreeWebNovel", "en", ContentType.NOVEL),
            url = "https://freewebnovel.com/novel/test-pagination",
            title = "Test Pagination",
            type = ContentType.NOVEL,
        )
        val source = FreeWebNovel(
            mockHttpClient { request ->
                requestedUrls += request.url.toString()
                when {
                    request.url.toString().contains("ajax=chapters") -> respond(
                        content = page2Json,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json; charset=UTF-8"),
                    )
                    else -> respondHtml(page1Html)
                }
            }
        )

        val chapters = source.getChapterList(series)

        assertEquals(
            listOf(
                "https://freewebnovel.com/novel/test-pagination",
                "https://freewebnovel.com/novel/test-pagination?ajax=chapters&page=2&pageSize=40",
                "https://freewebnovel.com/novel/test-pagination?ajax=chapters&page=3&pageSize=40",
            ),
            requestedUrls,
        )
        assertEquals(3, chapters.size)
        assertEquals("Chapter 1: One", chapters[0].name)
        assertEquals(1f, chapters[0].number)
        assertEquals("Chapter 2: Two", chapters[1].name)
        assertEquals(2f, chapters[1].number)
        assertEquals("Chapter 3: Three", chapters[2].name)
        assertEquals(3f, chapters[2].number)
        assertTrue(chapters[2].url.contains("/chapter-3"))
    }

    // =========================================================================
    // getChapterContent — novel text
    // =========================================================================

    @Test
    fun `getChapterContent returns Text with cleaned chapter HTML`() = runTest {
        val html = readFixture("fixtures/freewebnovel/chapter.html")
        val source = freeWebNovel(html)

        val chapter = com.opus.readerparser.domain.model.Chapter(
            seriesUrl = "https://freewebnovel.com/novel/advent-of-the-three-calamities",
            sourceId = source.id,
            url = "https://freewebnovel.com/novel/advent-of-the-three-calamities/chapter-876",
            name = "Chapter 876: Restless [3]",
            number = 876f,
        )
        val content = source.getChapterContent(chapter)

        assertTrue("Should be Text", content is ChapterContent.Text)
        val text = (content as ChapterContent.Text).html

        // Title preserved
        assertTrue(text.contains("Chapter 876: Restless [3]"))

        // Paragraph content preserved
        assertTrue(text.contains("Had I been careless"))
        assertTrue(text.contains("elixir"))

        // Ad containers removed
        assertFalse("pf- ad divs should be removed", text.contains("pf-"))
        assertFalse("bg-ssp ad divs should be removed", text.contains("bg-ssp"))

        // Script tags removed
        assertFalse("Script tags should be removed", text.contains("<script"))

        // subtxt watermarks removed
        assertFalse("subtxt watermarks should be removed", text.contains("<subtxt"))
    }

    // =========================================================================
    // Error paths — throws on missing required elements
    // =========================================================================

    @Test
    fun `seriesFromElement throws when card missing title link`() = runTest {
        val html = """<div class="ul-list1 ul-list1-2 ss-custom">
            <div class="li-row">
              <div class="li">
                <div class="con">
                  <div class="pic"><a href="/novel/some-slug"><img src="x.jpg"/></a></div>
                  <div class="txt">
                    <div class="desc">No title here</div>
                  </div>
                </div>
              </div>
            </div>
          </div>""".trimIndent()
        val source = freeWebNovel(html)

        try {
            source.getPopular(1)
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Series card missing title link"))
        }
    }

    @Test
    fun `chapterFromElement throws when URL cannot be resolved`() = runTest {
        val html = """
            <html><body>
              <ul id="idData">
                <li><a href="/chapter-1" class="con">Chapter 1: Bad URL</a></li>
              </ul>
            </body></html>
        """.trimIndent()
        val source = freeWebNovel(html)
        // A relative /chapter-1 path with an unparseable base URL
        // prevents Jsoup from resolving absUrl → returns ""
        val series = Series(
            sourceId = source.id,
            url = "not a valid url",
            title = "Test",
            type = ContentType.NOVEL,
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

    private fun freeWebNovel(html: String): FreeWebNovel {
        val client = mockHttpClient { request ->
            if (request.url.toString().contains("ajax=chapters")) {
                respond(
                    content = """{"code":200,"html":"","page":2,"pageSize":40,"totalPage":22,"totalChapters":876}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json; charset=UTF-8"),
                )
            } else {
                respondHtml(html)
            }
        }
        return FreeWebNovel(client)
    }
}

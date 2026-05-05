package com.opus.readerparser.data.source

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.testutil.mockHttpClient
import com.opus.readerparser.testutil.respondHtml
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [HtmlSource] default implementations using [mockHttpClient].
 * Each test creates a minimal concrete subclass and verifies the base class
 * wiring (fetch → parse → return) works correctly.
 */
class HtmlSourceTest {

    // ---- Minimal concrete source with novel content override ----

    private class TestNovelSource(
        client: HttpClient,
    ) : HtmlSource(client) {
        override val id = 1L
        override val name = "TestNovel"
        override val lang = "en"
        override val baseUrl = "https://test.invalid"
        override val type = ContentType.NOVEL

        override fun popularUrl(page: Int) = "$baseUrl/popular?page=$page"
        override fun popularSelector() = "div.series-card"
        override fun popularNextPageSelector() = "a.next-page"
        override fun seriesFromElement(el: Element) = Series(
            sourceId = id,
            url = el.selectFirst("a")!!.absUrl("href"),
            title = el.selectFirst("h3")!!.text(),
            type = type,
        )

        override fun searchUrl(query: String, page: Int, filters: FilterList) =
            "$baseUrl/search?q=$query&page=$page"

        override fun seriesDetailsParse(doc: Document, series: Series) =
            series.copy(author = doc.selectFirst(".author")?.text())

        override fun chapterListSelector() = "ul.chapters li"
        override fun chapterFromElement(el: Element, series: Series): Chapter {
            val a = el.selectFirst("a")!!
            return Chapter(
                seriesUrl = series.url,
                sourceId = id,
                url = a.absUrl("href"),
                name = a.text(),
                number = 1f,
            )
        }

        override fun chapterTextParse(doc: Document): String =
            doc.selectFirst("body")!!.html()

        override suspend fun getLatest(page: Int): SeriesPage =
            SeriesPage(emptyList(), false)
    }

    // ---- Minimal concrete source with manhwa content override ----

    private class TestManhwaSource(
        client: HttpClient,
    ) : HtmlSource(client) {
        override val id = 2L
        override val name = "TestManhwa"
        override val lang = "en"
        override val baseUrl = "https://test.invalid"
        override val type = ContentType.MANHWA

        override fun popularUrl(page: Int) = "$baseUrl/popular?page=$page"
        override fun popularSelector() = "div.series-card"
        override fun popularNextPageSelector() = "a.next-page"
        override fun seriesFromElement(el: Element) = Series(
            sourceId = id,
            url = el.selectFirst("a")!!.absUrl("href"),
            title = el.selectFirst("h3")!!.text(),
            type = type,
        )

        override fun searchUrl(query: String, page: Int, filters: FilterList) =
            "$baseUrl/search?q=$query&page=$page"

        override fun seriesDetailsParse(doc: Document, series: Series) =
            series.copy(author = doc.selectFirst(".author")?.text())

        override fun chapterListSelector() = "ul.chapters li"
        override fun chapterFromElement(el: Element, series: Series): Chapter {
            val a = el.selectFirst("a")!!
            return Chapter(
                seriesUrl = series.url,
                sourceId = id,
                url = a.absUrl("href"),
                name = a.text(),
                number = 1f,
            )
        }

        override fun chapterPagesParse(doc: Document): List<String> =
            doc.select("img.reader-page").map { it.absUrl("src") }

        override suspend fun getLatest(page: Int): SeriesPage =
            SeriesPage(emptyList(), false)
    }

    // ---- Source without content parse override (tests default error) ----

    private class TestNovelSourceNoContentParse(
        client: HttpClient,
    ) : HtmlSource(client) {
        override val id = 3L
        override val name = "TestNovelNoContent"
        override val lang = "en"
        override val baseUrl = "https://test.invalid"
        override val type = ContentType.NOVEL

        override fun popularUrl(page: Int) = "$baseUrl/popular?page=$page"
        override fun popularSelector() = "div.series-card"
        override fun popularNextPageSelector() = "a.next-page"
        override fun seriesFromElement(el: Element) = Series(
            sourceId = id,
            url = "https://test.invalid/series/x",
            title = "Title",
            type = type,
        )

        override fun searchUrl(query: String, page: Int, filters: FilterList) =
            "$baseUrl/search?q=$query&page=$page"

        override fun seriesDetailsParse(doc: Document, series: Series) = series

        override fun chapterListSelector() = "ul.chapters li"
        override fun chapterFromElement(el: Element, series: Series) = Chapter(
            seriesUrl = series.url,
            sourceId = id,
            url = "https://test.invalid/chapter/x",
            name = "Chapter",
            number = 1f,
        )

        override suspend fun getLatest(page: Int): SeriesPage =
            SeriesPage(emptyList(), false)
    }

    // ---- Helper: create a mock-backed test source ----

    private fun testNovelSource(html: String): TestNovelSource {
        val client = mockHttpClient { respondHtml(html) }
        return TestNovelSource(client)
    }

    private fun testManhwaSource(html: String): TestManhwaSource {
        val client = mockHttpClient { respondHtml(html) }
        return TestManhwaSource(client)
    }

    private fun testNovelSourceNoContentParse(html: String): TestNovelSourceNoContentParse {
        val client = mockHttpClient { respondHtml(html) }
        return TestNovelSourceNoContentParse(client)
    }

    // -----------------------------------------------------------------
    // fetchDoc (tested indirectly via getPopular, search, etc.)
    // -----------------------------------------------------------------

    @Test
    fun `fetchDoc returns parsed Jsoup Document`() = runTest {
        // Use getPopular as a proxy — it calls fetchDoc internally and
        // we verify the parsed result via the returned SeriesPage.
        val html = """
            <html><body><p>Hello</p>
              <div class="series-card"><a href="/s/1"><h3>S1</h3></a></div>
            </body></html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.getPopular(1)
        assertEquals(1, result.series.size)
        assertEquals("S1", result.series[0].title)
    }

    // -----------------------------------------------------------------
    // getPopular
    // -----------------------------------------------------------------

    @Test
    fun `getPopular fetches popularUrl and parses full page`() = runTest {
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/series/1"><h3>Series One</h3></a></div>
                <div class="series-card"><a href="/series/2"><h3>Series Two</h3></a></div>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.getPopular(1)

        assertEquals(2, result.series.size)
        assertEquals("Series One", result.series[0].title)
        assertEquals("Series Two", result.series[1].title)
        assertEquals("https://test.invalid/series/1", result.series[0].url)
        assertEquals(source.id, result.series[0].sourceId)
        assertEquals(ContentType.NOVEL, result.series[0].type)
        assertFalse(result.hasNextPage)
    }

    @Test
    fun `getPopular detects hasNextPage when nextPageSelector matches`() = runTest {
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/s/1"><h3>S1</h3></a></div>
                <a class="next-page" href="/popular?page=2">Next</a>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.getPopular(1)

        assertTrue(result.hasNextPage)
    }

    @Test
    fun `getPopular hasNextPage false when nextPageSelector is null`() = runTest {
        // Use a source where popularNextPageSelector returns null
        val html = "<html><body><div class='series-card'><a href='/s/1'><h3>S1</h3></a></div></body></html>"
        val client = mockHttpClient { respondHtml(html) }
        val source = object : HtmlSource(client) {
            override val id = 99L
            override val name = "NoNext"
            override val lang = "en"
            override val baseUrl = "https://test.invalid"
            override val type = ContentType.NOVEL
            override fun popularUrl(page: Int) = "$baseUrl/pop?page=$page"
            override fun popularSelector() = "div.series-card"
            override fun popularNextPageSelector(): String? = null
            override fun seriesFromElement(el: Element) = Series(
                sourceId = id, url = el.selectFirst("a")!!.absUrl("href"),
                title = el.selectFirst("h3")!!.text(), type = type,
            )
            override fun searchUrl(query: String, page: Int, filters: FilterList) =
                "$baseUrl/search?q=$query"
            override fun seriesDetailsParse(doc: Document, series: Series) = series
            override fun chapterListSelector() = "ul.chapters li"
            override fun chapterFromElement(el: Element, series: Series) = Chapter(
                seriesUrl = series.url, sourceId = id,
                url = "url", name = "ch", number = 1f,
            )
            override suspend fun getLatest(page: Int): SeriesPage =
                SeriesPage(emptyList(), false)
        }

        val result = source.getPopular(1)
        assertFalse(result.hasNextPage)
    }

    // -----------------------------------------------------------------
    // search
    // -----------------------------------------------------------------

    @Test
    fun `search fetches searchUrl and parses with searchSelector`() = runTest {
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/s/1"><h3>Result One</h3></a></div>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.search("term", 1, FilterList())

        assertEquals(1, result.series.size)
        assertEquals("Result One", result.series[0].title)
        assertFalse(result.hasNextPage)
    }

    @Test
    fun `searchSelector defaults to popularSelector — verified behaviorally`() = runTest {
        // If searchSelector were different from popularSelector, the search
        // test would fail because the HTML uses the popular selector.
        // The fact that search() parses correctly already proves the default.
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/s/1"><h3>Match</h3></a></div>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.search("term", 1, FilterList())
        assertEquals(1, result.series.size)
        assertEquals("Match", result.series[0].title)
    }

    @Test
    fun `searchSeriesFromElement defaults to seriesFromElement — verified behaviorally`() = runTest {
        // The search result parsing works with the same HTML structure as
        // getPopular, which means the searchSeriesFromElement default (which
        // delegates to seriesFromElement) operates correctly.
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/s/1"><h3>Search Hit</h3></a></div>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.search("q", 1, FilterList())
        assertEquals(1, result.series.size)
        assertEquals("Search Hit", result.series[0].title)
        assertEquals("https://test.invalid/s/1", result.series[0].url)
    }

    @Test
    fun `searchNextPageSelector defaults to popularNextPageSelector — verified behaviorally`() = runTest {
        // hasNextPage detection works for search the same way it does for
        // popular, confirming the default wiring.
        val html = """
            <html>
              <body>
                <div class="series-card"><a href="/s/1"><h3>S1</h3></a></div>
                <a class="next-page" href="/search?q=t&page=2">Next</a>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)
        val result = source.search("t", 1, FilterList())
        assertTrue(result.hasNextPage)
    }

    // -----------------------------------------------------------------
    // getSeriesDetails
    // -----------------------------------------------------------------

    @Test
    fun `getSeriesDetails fetches series url and enriches via seriesDetailsParse`() = runTest {
        val html = "<html><body><span class='author'>Jane Doe</span></body></html>"
        val source = testNovelSource(html)

        val input = Series(
            sourceId = source.id,
            url = "https://test.invalid/series/1",
            title = "Original Title",
            type = ContentType.NOVEL,
        )
        val result = source.getSeriesDetails(input)

        assertEquals("Original Title", result.title) // preserved
        assertEquals("Jane Doe", result.author)       // enriched
    }

    // -----------------------------------------------------------------
    // getChapterList
    // -----------------------------------------------------------------

    @Test
    fun `getChapterList fetches series url and parses chapters`() = runTest {
        val html = """
            <html>
              <body>
                <ul class="chapters">
                  <li><a href="/series/1/ch/1">Chapter 1</a></li>
                  <li><a href="/series/1/ch/2">Chapter 2</a></li>
                </ul>
              </body>
            </html>
        """.trimIndent()
        val source = testNovelSource(html)

        val series = Series(
            sourceId = source.id,
            url = "https://test.invalid/series/1",
            title = "S",
            type = ContentType.NOVEL,
        )
        val chapters = source.getChapterList(series)

        assertEquals(2, chapters.size)
        assertEquals("Chapter 1", chapters[0].name)
        assertEquals("https://test.invalid/series/1/ch/1", chapters[0].url)
        assertEquals(source.id, chapters[0].sourceId)
        assertEquals(series.url, chapters[0].seriesUrl)
    }

    // -----------------------------------------------------------------
    // getChapterContent — novel route
    // -----------------------------------------------------------------

    @Test
    fun `getChapterContent for NOVEL returns Text with parsed HTML`() = runTest {
        val html = "<html><body><article>Story content here</article></body></html>"
        val source = testNovelSource(html)

        val chapter = Chapter(
            seriesUrl = "https://test.invalid/series/1",
            sourceId = source.id,
            url = "https://test.invalid/series/1/ch/1",
            name = "Chapter 1",
            number = 1f,
        )
        val content = source.getChapterContent(chapter)

        assertTrue(content is ChapterContent.Text)
        assertTrue((content as ChapterContent.Text).html.contains("Story content here"))
    }

    // -----------------------------------------------------------------
    // getChapterContent — manhwa route
    // -----------------------------------------------------------------

    @Test
    fun `getChapterContent for MANHWA returns Pages with image URLs`() = runTest {
        val html = """
            <html>
              <body>
                <img class="reader-page" src="/img/page01.jpg" />
                <img class="reader-page" src="/img/page02.jpg" />
              </body>
            </html>
        """.trimIndent()
        val source = testManhwaSource(html)

        val chapter = Chapter(
            seriesUrl = "https://test.invalid/series/1",
            sourceId = source.id,
            url = "https://test.invalid/series/1/ch/1",
            name = "Chapter 1",
            number = 1f,
        )
        val content = source.getChapterContent(chapter)

        assertTrue(content is ChapterContent.Pages)
        val pages = (content as ChapterContent.Pages).imageUrls
        assertEquals(2, pages.size)
        assertTrue(pages[0].endsWith("/img/page01.jpg"))
        assertTrue(pages[1].endsWith("/img/page02.jpg"))
    }

    // -----------------------------------------------------------------
    // Error propagation: default implementations throw
    // -----------------------------------------------------------------

    @Test
    fun `chapterTextParse throws by default`() = runTest {
        val html = "<html><body>irrelevant</body></html>"
        val source = testNovelSourceNoContentParse(html)

        val chapter = Chapter(
            seriesUrl = "https://test.invalid/series/1",
            sourceId = source.id,
            url = "https://test.invalid/series/1/ch/1",
            name = "Chapter 1",
            number = 1f,
        )

        try {
            source.getChapterContent(chapter)
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Override for novel sources"))
        }
    }

    @Test
    fun `chapterPagesParse throws by default`() = runTest {
        val html = "<html><body>irrelevant</body></html>"
        val client = mockHttpClient { respondHtml(html) }
        val source = object : HtmlSource(client) {
            override val id = 50L
            override val name = "TestManhwaNoContent"
            override val lang = "en"
            override val baseUrl = "https://test.invalid"
            override val type = ContentType.MANHWA

            override fun popularUrl(page: Int) = "$baseUrl/pop?page=$page"
            override fun popularSelector() = "div.card"
            override fun popularNextPageSelector(): String? = null
            override fun seriesFromElement(el: Element) = Series(
                sourceId = id, url = "url", title = "t", type = type,
            )
            override fun searchUrl(query: String, page: Int, filters: FilterList) =
                "$baseUrl/s?q=$query"
            override fun seriesDetailsParse(doc: Document, series: Series) = series
            override fun chapterListSelector() = "ul li"
            override fun chapterFromElement(el: Element, series: Series) = Chapter(
                seriesUrl = series.url, sourceId = id,
                url = "url", name = "ch", number = 1f,
            )
            override suspend fun getLatest(page: Int): SeriesPage =
                SeriesPage(emptyList(), false)
        }

        val chapter = Chapter(
            seriesUrl = "https://test.invalid/series/1",
            sourceId = source.id,
            url = "https://test.invalid/series/1/ch/1",
            name = "Chapter 1",
            number = 1f,
        )

        try {
            source.getChapterContent(chapter)
            throw AssertionError("Expected an exception but none was thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Override for manhwa sources"))
        }
    }

    // -----------------------------------------------------------------
    // HtmlSource is abstract
    // -----------------------------------------------------------------

    @Test
    fun `HtmlSource is abstract and implements Source`() {
        val client = mockHttpClient { respondHtml("<html></html>") }
        val source: Source = TestNovelSource(client) // compiles = implements Source
        assertNotNull(source.id)
    }
}

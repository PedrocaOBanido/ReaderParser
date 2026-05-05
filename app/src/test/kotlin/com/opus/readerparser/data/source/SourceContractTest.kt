package com.opus.readerparser.data.source

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Filter
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compile-time contract verification for the [Source] interface.
 * Creating a minimal concrete implementation proves that the interface
 * signatures match the architecture.md §3.3 specification.
 */
class SourceContractTest {

    private val source: Source = object : Source {
        override val id: Long = 1L
        override val name: String = "TestSource"
        override val lang: String = "en"
        override val baseUrl: String = "https://test.invalid"
        override val type: ContentType = ContentType.NOVEL

        override suspend fun getPopular(page: Int): SeriesPage = SeriesPage(emptyList(), false)
        override suspend fun getLatest(page: Int): SeriesPage = SeriesPage(emptyList(), false)
        override suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage =
            SeriesPage(emptyList(), false)
        override suspend fun getSeriesDetails(series: Series): Series = series
        override suspend fun getChapterList(series: Series): List<Chapter> = emptyList()
        override suspend fun getChapterContent(chapter: Chapter): ChapterContent =
            ChapterContent.Text("")
    }

    @Test
    fun `properties are present with correct types`() {
        assertNotNull(source.id)
        assertNotNull(source.name)
        assertNotNull(source.lang)
        assertNotNull(source.baseUrl)
        assertNotNull(source.type)
    }

    @Test
    fun `supports returns true by default`() {
        val toggle = Filter.Toggle("key", true)
        assertTrue(source.supports(toggle))
    }

    @Test
    fun `suspend functions compile and return correct types`() = runTest {
        val popular = source.getPopular(1)
        assertNotNull(popular)
        assertTrue(popular is SeriesPage)

        val latest = source.getLatest(1)
        assertNotNull(latest)
        assertTrue(latest is SeriesPage)

        val search = source.search("test", 1, FilterList())
        assertNotNull(search)
        assertTrue(search is SeriesPage)

        val series = Series(
            sourceId = 1L,
            url = "https://test.invalid/series/1",
            title = "Test",
            type = ContentType.NOVEL,
        )
        val details = source.getSeriesDetails(series)
        assertNotNull(details)
        assertTrue(details is Series)

        val chapters = source.getChapterList(series)
        assertNotNull(chapters)
        assertTrue(chapters is List<*>)

        val chapter = Chapter(
            seriesUrl = series.url,
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            name = "Chapter 1",
            number = 1f,
        )
        val content = source.getChapterContent(chapter)
        assertNotNull(content)
        assertTrue(content is ChapterContent)
    }
}

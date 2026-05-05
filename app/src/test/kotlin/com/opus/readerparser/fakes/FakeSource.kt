package com.opus.readerparser.fakes

import com.opus.readerparser.core.util.computeSourceId
import com.opus.readerparser.data.source.Source
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Filter
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.testutil.TestFixtures

/**
 * Hand-rolled fake [Source] for repository tests.
 *
 * Behaviour is configured via mutable public properties before the test
 * runs.  Each method also records every invocation so tests can assert
 * that the repository called the right source methods with the right
 * arguments.
 *
 * [getChapterContent] automatically returns [ChapterContent.Text] for
 * [ContentType.NOVEL] and [ChapterContent.Pages] for [ContentType.MANHWA]
 * when [chapterContentResult] has not been explicitly set.
 */
class FakeSource(
    name: String = "FakeSource",
    lang: String = "en",
    type: ContentType = ContentType.NOVEL,
) : Source {

    // -- identity (via the stable hash) --
    override val id: Long = computeSourceId(name, lang, type)
    override val name: String = name
    override val lang: String = lang
    override val baseUrl: String = "https://fakesource.invalid"
    override val type: ContentType = type

    // -- configurable return values --
    var popularResult: SeriesPage = SeriesPage(emptyList(), false)
    var latestResult: SeriesPage = SeriesPage(emptyList(), false)
    var searchResult: SeriesPage = SeriesPage(emptyList(), false)
    var seriesDetailsResult: (Series) -> Series = { it }
    var chapterListResult: List<Chapter> = emptyList()

    /**
     * When `null` (default), [getChapterContent] auto-returns
     * [ChapterContent.Text] for novels / [ChapterContent.Pages] for
     * manhwa using sensible test defaults.
     */
    var chapterContentResult: ChapterContent? = null
    var supportsResult: (Filter) -> Boolean = { true }

    // -- call recording for assertions --
    val getPopularCalls: MutableList<Int> = mutableListOf()
    val getLatestCalls: MutableList<Int> = mutableListOf()
    val searchCalls: MutableList<Triple<String, Int, FilterList>> = mutableListOf()
    val getSeriesDetailsCalls: MutableList<Series> = mutableListOf()
    val getChapterListCalls: MutableList<Series> = mutableListOf()
    val getChapterContentCalls: MutableList<Chapter> = mutableListOf()

    // -- Source implementation --

    override fun supports(filter: Filter): Boolean = supportsResult(filter)

    override suspend fun getPopular(page: Int): SeriesPage {
        getPopularCalls.add(page)
        return popularResult
    }

    override suspend fun getLatest(page: Int): SeriesPage {
        getLatestCalls.add(page)
        return latestResult
    }

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList,
    ): SeriesPage {
        searchCalls.add(Triple(query, page, filters))
        return searchResult
    }

    override suspend fun getSeriesDetails(series: Series): Series {
        getSeriesDetailsCalls.add(series)
        return seriesDetailsResult(series)
    }

    override suspend fun getChapterList(series: Series): List<Chapter> {
        getChapterListCalls.add(series)
        return chapterListResult
    }

    override suspend fun getChapterContent(chapter: Chapter): ChapterContent {
        getChapterContentCalls.add(chapter)
        return chapterContentResult ?: when (type) {
            ContentType.NOVEL -> TestFixtures.testTextContent()
            ContentType.MANHWA -> TestFixtures.testPagesContent()
        }
    }
}

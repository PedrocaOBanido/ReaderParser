package com.opus.readerparser.fakes

import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-rolled fake [SeriesRepository] for ViewModel tests.
 *
 * Behaviour is configured via mutable public properties before the test
 * runs.  Each method also records every invocation so tests can assert
 * that the ViewModel called the right methods with the right arguments.
 *
 * The library is backed by a [MutableStateFlow] — tests can assert on its
 * contents or observe changes through [observeLibrary].
 */
class FakeSeriesRepository : SeriesRepository {

    // -- configurable return values --
    var popularResult: SeriesPage = SeriesPage(emptyList(), false)
    var latestResult: SeriesPage = SeriesPage(emptyList(), false)
    var searchResult: SeriesPage = SeriesPage(emptyList(), false)
    var refreshDetailsResult: (Series) -> Series = { it }
    var isInLibraryResult: (Long, String) -> Boolean = { _, _ -> false }

    // -- call recording --
    val fetchPopularCalls: MutableList<Pair<Long, Int>> = mutableListOf()
    val fetchLatestCalls: MutableList<Pair<Long, Int>> = mutableListOf()
    val searchCalls: MutableList<SearchCall> = mutableListOf()
    val refreshDetailsCalls: MutableList<Series> = mutableListOf()
    val addToLibraryCalls: MutableList<Series> = mutableListOf()
    val removeFromLibraryCalls: MutableList<Series> = mutableListOf()
    val isInLibraryCalls: MutableList<Pair<Long, String>> = mutableListOf()

    // -- library state --
    private val _library = MutableStateFlow<List<Series>>(emptyList())

    override fun observeLibrary(): Flow<List<Series>> = _library

    override suspend fun fetchPopular(sourceId: Long, page: Int): SeriesPage {
        fetchPopularCalls.add(sourceId to page)
        return popularResult
    }

    override suspend fun fetchLatest(sourceId: Long, page: Int): SeriesPage {
        fetchLatestCalls.add(sourceId to page)
        return latestResult
    }

    override suspend fun search(
        sourceId: Long,
        query: String,
        page: Int,
        filters: FilterList,
    ): SeriesPage {
        searchCalls.add(SearchCall(sourceId, query, page, filters))
        return searchResult
    }

    override suspend fun refreshDetails(series: Series): Series {
        refreshDetailsCalls.add(series)
        return refreshDetailsResult(series)
    }

    override suspend fun addToLibrary(series: Series) {
        addToLibraryCalls.add(series)
        _library.value = _library.value + series
    }

    override suspend fun removeFromLibrary(series: Series) {
        removeFromLibraryCalls.add(series)
        _library.value = _library.value.filter { it.url != series.url || it.sourceId != series.sourceId }
    }

    override suspend fun isInLibrary(sourceId: Long, url: String): Boolean {
        isInLibraryCalls.add(sourceId to url)
        return isInLibraryResult(sourceId, url)
    }
}

data class SearchCall(
    val sourceId: Long,
    val query: String,
    val page: Int,
    val filters: FilterList,
)

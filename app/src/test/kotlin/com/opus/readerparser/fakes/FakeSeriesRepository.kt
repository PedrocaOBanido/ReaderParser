package com.opus.readerparser.fakes

import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.LibrarySearchResult
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    var fetchPopularHandler: suspend (Long, Int) -> SeriesPage = { _, _ -> popularResult }
    var fetchLatestHandler: suspend (Long, Int) -> SeriesPage = { _, _ -> latestResult }
    var searchHandler: suspend (Long, String, Int, FilterList) -> SeriesPage = { _, _, _, _ -> searchResult }
    var searchLibraryResult: LibrarySearchResult = LibrarySearchResult.Success(emptyList())
    var searchLibraryHandler: suspend (String) -> LibrarySearchResult = { searchLibraryResult }
    var refreshDetailsResult: (Series) -> Series = { it }
    var isInLibraryResult: (Long, String) -> Boolean = { _, _ -> false }

    // -- call recording --
    val fetchPopularCalls: MutableList<Pair<Long, Int>> = mutableListOf()
    val fetchLatestCalls: MutableList<Pair<Long, Int>> = mutableListOf()
    val searchCalls: MutableList<SearchCall> = mutableListOf()
    val searchLibraryCalls: MutableList<String> = mutableListOf()
    val refreshDetailsCalls: MutableList<Series> = mutableListOf()
    val addToLibraryCalls: MutableList<Series> = mutableListOf()
    val removeFromLibraryCalls: MutableList<Series> = mutableListOf()
    val isInLibraryCalls: MutableList<Pair<Long, String>> = mutableListOf()

    // -- library state --
    private val _library = MutableStateFlow<List<Series>>(emptyList())
    private val _librarySearchInvalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeLibrary(): Flow<List<Series>> = _library

    override fun observeLibrarySearchInvalidations(): Flow<Unit> = _librarySearchInvalidations

    fun emitLibrarySearchInvalidation() {
        _librarySearchInvalidations.tryEmit(Unit)
    }

    override suspend fun fetchPopular(sourceId: Long, page: Int): SeriesPage {
        fetchPopularCalls.add(sourceId to page)
        return fetchPopularHandler(sourceId, page)
    }

    override suspend fun fetchLatest(sourceId: Long, page: Int): SeriesPage {
        fetchLatestCalls.add(sourceId to page)
        return fetchLatestHandler(sourceId, page)
    }

    override suspend fun search(
        sourceId: Long,
        query: String,
        page: Int,
        filters: FilterList,
    ): SeriesPage {
        searchCalls.add(SearchCall(sourceId, query, page, filters))
        return searchHandler(sourceId, query, page, filters)
    }

    override suspend fun searchLibrary(query: String): LibrarySearchResult {
        searchLibraryCalls.add(query)
        return searchLibraryHandler(query)
    }

    override suspend fun refreshDetails(series: Series): Series {
        refreshDetailsCalls.add(series)
        return refreshDetailsResult(series).also { refreshed ->
            if (refreshed.title.isNotBlank()) {
                _library.value = _library.value.map { existing ->
                    if (existing.sourceId == refreshed.sourceId && existing.url == refreshed.url) refreshed else existing
                }
            }
        }
    }

    override suspend fun addToLibrary(series: Series) {
        addToLibraryCalls.add(series)
        _library.value = _library.value + series
        _librarySearchInvalidations.tryEmit(Unit)
    }

    override suspend fun removeFromLibrary(series: Series) {
        removeFromLibraryCalls.add(series)
        _library.value = _library.value.filter { it.url != series.url || it.sourceId != series.sourceId }
        _librarySearchInvalidations.tryEmit(Unit)
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

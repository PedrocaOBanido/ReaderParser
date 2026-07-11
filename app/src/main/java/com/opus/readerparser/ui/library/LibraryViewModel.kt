package com.opus.readerparser.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.LibrarySearchResult
import com.opus.readerparser.domain.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val _effects = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects: Flow<LibraryEffect> = _effects.receiveAsFlow()

    /** The complete, unfiltered library list used as the source for re-filtering. */
    private var allLibrarySeries: List<Series> = emptyList()
    private var searchGeneration: Long = 0
    private var searchJob: kotlinx.coroutines.Job? = null
    private val attemptedRepairs = mutableSetOf<Pair<Long, String>>()

    init {
        viewModelScope.launch {
            seriesRepository.observeLibrary().collect { library ->
                allLibrarySeries = library

                // Repair blank bookmarked titles once per lifecycle
                val blankBooks = library.filter { it.title.isBlank() }
                for (blankBook in blankBooks) {
                    val key = blankBook.sourceId to blankBook.url
                    if (key !in attemptedRepairs) {
                        attemptedRepairs.add(key)
                        launch {
                            try {
                                seriesRepository.refreshDetails(blankBook)
                            } catch (_: Exception) {
                                // Swallow — bookmark stays unchanged; retry next lifecycle
                            }
                        }
                    }
                }

                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.isBlank()) {
                    _state.update { current ->
                        current.copy(series = filterAndSort(library, current.sortBy))
                    }
                }
            }
        }

        viewModelScope.launch {
            seriesRepository.observeLibrarySearchInvalidations().collect {
                val currentQuery = _state.value.searchQuery.trim()
                if (currentQuery.isNotBlank()) {
                    val requestId = ++searchGeneration
                    refreshSearch(currentQuery, requestId, showLoading = false)
                }
            }
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.OpenSeries -> viewModelScope.launch {
                _effects.send(LibraryEffect.NavigateToSeries(action.series))
            }
            is LibraryAction.RemoveFromLibrary -> viewModelScope.launch {
                try {
                    seriesRepository.removeFromLibrary(action.series)
                } catch (e: Exception) {
                    _effects.send(LibraryEffect.ShowError(e.message ?: "Failed to remove from library"))
                }
            }
            is LibraryAction.SetSortBy -> _state.update { current ->
                val series = if (current.searchQuery.isBlank()) {
                    filterAndSort(allLibrarySeries, action.sortBy)
                } else {
                    current.series
                }
                current.copy(sortBy = action.sortBy, series = series)
            }
            is LibraryAction.SetSearchQuery -> handleSearchQuery(action.query)
        }
    }

    private fun handleSearchQuery(query: String) {
        searchJob?.cancel()
        val trimmedQuery = query.trim()
        val requestId = ++searchGeneration

        if (trimmedQuery.isBlank()) {
            _state.update { current ->
                current.copy(
                    searchQuery = query,
                    isLoading = false,
                    error = null,
                    series = filterAndSort(allLibrarySeries, current.sortBy),
                )
            }
            return
        }

        _state.update { current ->
            current.copy(searchQuery = query, isLoading = true, error = null)
        }

        refreshSearch(trimmedQuery, requestId, showLoading = true)
    }

    private fun refreshSearch(query: String, requestId: Long, showLoading: Boolean) {
        if (showLoading) {
            _state.update { current -> current.copy(isLoading = true, error = null) }
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                when (val result = seriesRepository.searchLibrary(query)) {
                    is LibrarySearchResult.Success -> if (requestId == searchGeneration) {
                        _state.update { current ->
                            current.copy(
                                isLoading = false,
                                error = null,
                                series = result.series,
                            )
                        }
                    }
                    is LibrarySearchResult.Failure -> if (requestId == searchGeneration) {
                        _state.update { current ->
                            current.copy(
                                isLoading = false,
                                error = result.message,
                                series = emptyList(),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (requestId == searchGeneration) {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to search library",
                            series = emptyList(),
                        )
                    }
                }
            }
        }
    }

    /**
     * Applies the given [query] and [sortBy] to the raw [library] list and
     * returns the filtered, sorted result.
     */
    private fun filterAndSort(
        library: List<Series>,
        sortBy: LibrarySortBy,
    ): List<Series> {
        return when (sortBy) {
            LibrarySortBy.TITLE -> library.sortedBy { it.title }
            LibrarySortBy.DEFAULT -> library
        }
    }
}

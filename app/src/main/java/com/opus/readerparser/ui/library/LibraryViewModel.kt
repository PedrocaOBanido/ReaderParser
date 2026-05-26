package com.opus.readerparser.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.core.util.TitleMatcher
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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

    init {
        viewModelScope.launch {
            seriesRepository.observeLibrary().collect { library ->
                allLibrarySeries = library
                _state.update { current ->
                    current.copy(series = filterAndSort(library, current.searchQuery, current.sortBy))
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
                val sorted = filterAndSort(allLibrarySeries, current.searchQuery, action.sortBy)
                current.copy(sortBy = action.sortBy, series = sorted)
            }
            is LibraryAction.SetFilterUnreadOnly -> _state.update { current ->
                current.copy(
                    filterUnreadOnly = action.enabled,
                    series = filterAndSort(allLibrarySeries, current.searchQuery, current.sortBy),
                )
            }
            is LibraryAction.SetSearchQuery -> _state.update { current ->
                val filtered = filterAndSort(allLibrarySeries, action.query, current.sortBy)
                current.copy(searchQuery = action.query, series = filtered)
            }
        }
    }

    /**
     * Applies the given [query] and [sortBy] to the raw [library] list and
     * returns the filtered, sorted result.
     */
    private fun filterAndSort(
        library: List<Series>,
        query: String,
        sortBy: LibrarySortBy,
    ): List<Series> {
        val filtered = library.filter { series ->
            query.isBlank() || TitleMatcher.matches(query, series.title)
        }
        return when (sortBy) {
            LibrarySortBy.TITLE -> filtered.sortedBy { it.title }
            LibrarySortBy.DEFAULT -> filtered
        }
    }
}

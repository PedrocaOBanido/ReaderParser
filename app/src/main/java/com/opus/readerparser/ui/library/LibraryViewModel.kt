package com.opus.readerparser.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        viewModelScope.launch {
            seriesRepository.observeLibrary().collect { library ->
                _state.update { current ->
                    current.copy(series = library.sorted(current.sortBy))
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
                current.copy(
                    sortBy = action.sortBy,
                    series = current.series.sorted(action.sortBy),
                )
            }
            is LibraryAction.SetFilterUnreadOnly -> _state.update {
                it.copy(filterUnreadOnly = action.enabled)
            }
        }
    }

    private fun List<Series>.sorted(sortBy: LibrarySortBy): List<Series> = when (sortBy) {
        LibrarySortBy.TITLE -> sortedBy { it.title }
        LibrarySortBy.DEFAULT -> this
    }
}

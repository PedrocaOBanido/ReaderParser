package com.opus.readerparser.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesStatus
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
class SeriesViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val seriesRepository: SeriesRepository,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    private val sourceId: Long = checkNotNull(savedState["sourceId"])
    private val seriesUrl: String = checkNotNull(savedState["seriesUrl"])

    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state.asStateFlow()

    private val _effects = Channel<SeriesEffect>(Channel.BUFFERED)
    val effects: Flow<SeriesEffect> = _effects.receiveAsFlow()

    private val stubSeries: Series
        get() = Series(
            sourceId = sourceId,
            url = seriesUrl,
            title = "",
            type = ContentType.NOVEL,
        )

    init {
        observe()
        refresh()
    }

    fun onAction(action: SeriesAction) {
        when (action) {
            is SeriesAction.Refresh -> refresh()
            is SeriesAction.ToggleLibrary -> viewModelScope.launch {
                val series = _state.value.series ?: return@launch
                try {
                    if (action.inLibrary) {
                        seriesRepository.addToLibrary(series)
                    } else {
                        seriesRepository.removeFromLibrary(series)
                    }
                    _state.update { it.copy(inLibrary = action.inLibrary) }
                } catch (e: Exception) {
                    _effects.send(SeriesEffect.ShowError(e.message ?: "Failed to update library"))
                }
            }
            is SeriesAction.OpenChapter -> viewModelScope.launch {
                val type = _state.value.series?.type ?: return@launch
                _effects.send(SeriesEffect.NavigateToReader(action.chapter, type))
            }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            chapterRepository.observeChapters(stubSeries).collect { chapters ->
                _state.update { it.copy(chapters = chapters) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val updated = seriesRepository.refreshDetails(stubSeries)
                chapterRepository.refreshChapters(updated)
                val inLibrary = seriesRepository.isInLibrary(updated.sourceId, updated.url)
                _state.update { it.copy(series = updated, inLibrary = inLibrary, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load series") }
                _effects.send(SeriesEffect.ShowError(e.message ?: "Failed to load series"))
            }
        }
    }
}

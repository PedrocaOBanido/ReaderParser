package com.opus.readerparser.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.SourceRepository
import com.opus.readerparser.domain.model.FilterList
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
class BrowseViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val seriesRepository: SeriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private val _effects = Channel<BrowseEffect>(Channel.BUFFERED)
    val effects: Flow<BrowseEffect> = _effects.receiveAsFlow()

    init {
        val sources = sourceRepository.getSources()
        _state.update { it.copy(sources = sources) }
        sources.firstOrNull()?.let { first ->
            _state.update { it.copy(selectedSourceId = first.id) }
            fetchPage(first.id, BrowseMode.POPULAR, page = 1, reset = true)
        }
    }

    fun onAction(action: BrowseAction) {
        when (action) {
            is BrowseAction.SelectSource -> {
                val sourceId = action.sourceId
                _state.update { it.copy(selectedSourceId = sourceId) }
                fetchPage(sourceId, _state.value.mode, page = 1, reset = true)
            }
            is BrowseAction.SetMode -> {
                val sourceId = _state.value.selectedSourceId ?: return
                _state.update { it.copy(mode = action.mode, searchQuery = "") }
                fetchPage(sourceId, action.mode, page = 1, reset = true)
            }
            is BrowseAction.LoadMore -> {
                val current = _state.value
                val sourceId = current.selectedSourceId ?: return
                if (!current.hasNextPage || current.isLoading) return
                fetchPage(sourceId, current.mode, page = current.currentPage + 1, reset = false)
            }
            is BrowseAction.SetSearchQuery -> _state.update { it.copy(searchQuery = action.query) }
            is BrowseAction.Search -> {
                val sourceId = _state.value.selectedSourceId ?: return
                _state.update { it.copy(mode = BrowseMode.SEARCH) }
                fetchPage(sourceId, BrowseMode.SEARCH, page = 1, reset = true)
            }
            is BrowseAction.OpenSeries -> viewModelScope.launch {
                _effects.send(BrowseEffect.NavigateToSeries(action.series))
            }
        }
    }

    private fun fetchPage(sourceId: Long, mode: BrowseMode, page: Int, reset: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = when (mode) {
                    BrowseMode.POPULAR -> seriesRepository.fetchPopular(sourceId, page)
                    BrowseMode.LATEST -> seriesRepository.fetchLatest(sourceId, page)
                    BrowseMode.SEARCH -> seriesRepository.search(
                        sourceId, _state.value.searchQuery, page, FilterList()
                    )
                }
                _state.update { current ->
                    current.copy(
                        series = if (reset) result.series else current.series + result.series,
                        hasNextPage = result.hasNextPage,
                        currentPage = page,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
                _effects.send(BrowseEffect.ShowError(e.message ?: "Failed to load"))
            }
        }
    }
}

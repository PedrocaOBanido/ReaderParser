package com.opus.readerparser.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.DownloadRepository
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
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    private val _effects = Channel<DownloadsEffect>(Channel.BUFFERED)
    val effects: Flow<DownloadsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            downloadRepository.observeQueue().collect { queue ->
                _state.update { it.copy(downloads = queue) }
            }
        }
    }

    fun onAction(action: DownloadsAction) {
        when (action) {
            is DownloadsAction.Cancel -> viewModelScope.launch {
                try {
                    downloadRepository.cancel(action.sourceId, action.chapterUrl)
                } catch (e: Exception) {
                    _effects.send(DownloadsEffect.ShowError(e.message ?: "Failed to cancel download"))
                }
            }
            is DownloadsAction.Retry -> viewModelScope.launch {
                try {
                    downloadRepository.retry(action.sourceId, action.chapterUrl)
                } catch (e: Exception) {
                    _effects.send(DownloadsEffect.ShowError(e.message ?: "Failed to retry download"))
                }
            }
        }
    }
}

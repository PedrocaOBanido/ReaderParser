package com.opus.readerparser.ui.reader.novel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NovelReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    private val sourceId: Long = checkNotNull(savedState["sourceId"])
    private val seriesUrl: String = checkNotNull(savedState["seriesUrl"])
    private val chapterUrl: String = checkNotNull(savedState["chapterUrl"])

    private val _state = MutableStateFlow(NovelReaderUiState())
    val state: StateFlow<NovelReaderUiState> = _state.asStateFlow()

    private val _effects = Channel<NovelReaderEffect>(Channel.BUFFERED)
    val effects: Flow<NovelReaderEffect> = _effects.receiveAsFlow()

    private val seriesStub: Series
        get() = Series(sourceId = sourceId, url = seriesUrl, title = "", type = ContentType.NOVEL)

    init {
        loadCurrentChapter()
    }

    fun onAction(action: NovelReaderAction) {
        when (action) {
            is NovelReaderAction.Load -> loadChapter(action.chapter)
            is NovelReaderAction.SetProgress -> viewModelScope.launch {
                val chapter = _state.value.chapter ?: return@launch
                chapterRepository.setProgress(chapter, action.progress)
                _state.update { it.copy(progress = action.progress) }
            }
            is NovelReaderAction.PreviousChapter -> navigateChapter(forward = false)
            is NovelReaderAction.NextChapter -> navigateChapter(forward = true)
            is NovelReaderAction.OpenChapterList -> viewModelScope.launch {
                _effects.send(NovelReaderEffect.ShowChapterList)
            }
        }
    }

    private fun loadCurrentChapter() {
        viewModelScope.launch {
            val chapters = chapterRepository.observeChapters(seriesStub).first()
            val chapter = chapters.firstOrNull { it.chapter.url == chapterUrl }?.chapter
                ?: return@launch
            loadChapter(chapter)
        }
    }

    private fun loadChapter(chapter: Chapter) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, chapter = chapter) }
            try {
                val content = chapterRepository.getContent(chapter)
                val html = (content as? ChapterContent.Text)?.html ?: run {
                    _state.update { it.copy(isLoading = false, error = "Unexpected content type") }
                    return@launch
                }
                chapterRepository.markRead(chapter, true)
                val chapters = chapterRepository.observeChapters(seriesStub).first()
                val idx = chapters.indexOfFirst { it.chapter.url == chapter.url }
                _state.update {
                    it.copy(
                        html = html,
                        isLoading = false,
                        progress = chapters.getOrNull(idx)?.progress ?: 0f,
                        hasPreviousChapter = idx > 0,
                        hasNextChapter = idx in 0 until chapters.lastIndex,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load chapter") }
                _effects.send(NovelReaderEffect.ShowError(e.message ?: "Failed to load chapter"))
            }
        }
    }

    private fun navigateChapter(forward: Boolean) {
        viewModelScope.launch {
            val current = _state.value.chapter ?: return@launch
            val chapters = chapterRepository.observeChapters(seriesStub).first()
            val idx = chapters.indexOfFirst { it.chapter.url == current.url }
            val target = if (forward) chapters.getOrNull(idx + 1) else chapters.getOrNull(idx - 1)
            if (target != null) {
                _effects.send(NovelReaderEffect.NavigateToChapter(target.chapter))
            }
        }
    }
}

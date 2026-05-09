package com.opus.readerparser.ui.reader.manhwa

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
class MangaReaderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val chapterRepository: ChapterRepository,
) : ViewModel() {

    private val sourceId: Long = checkNotNull(savedState["sourceId"])
    private val seriesUrl: String = checkNotNull(savedState["seriesUrl"])
    private val chapterUrl: String = checkNotNull(savedState["chapterUrl"])

    private val _state = MutableStateFlow(MangaReaderUiState())
    val state: StateFlow<MangaReaderUiState> = _state.asStateFlow()

    private val _effects = Channel<MangaReaderEffect>(Channel.BUFFERED)
    val effects: Flow<MangaReaderEffect> = _effects.receiveAsFlow()

    private val seriesStub: Series
        get() = Series(sourceId = sourceId, url = seriesUrl, title = "", type = ContentType.MANHWA)

    init {
        loadCurrentChapter()
    }

    fun onAction(action: MangaReaderAction) {
        when (action) {
            is MangaReaderAction.Load -> loadChapter(action.chapter)
            is MangaReaderAction.SetPage -> {
                val pages = _state.value.pages
                _state.update { it.copy(currentPage = action.page) }
                if (action.page == pages.lastIndex) {
                    markReadIfLoaded()
                }
            }
            is MangaReaderAction.PreviousChapter -> navigateChapter(forward = false)
            is MangaReaderAction.NextChapter -> navigateChapter(forward = true)
            is MangaReaderAction.OpenChapterList -> viewModelScope.launch {
                _effects.send(MangaReaderEffect.ShowChapterList)
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
            _state.update { it.copy(isLoading = true, error = null, chapter = chapter, currentPage = 0) }
            try {
                val content = chapterRepository.getContent(chapter)
                val pages = (content as? ChapterContent.Pages)?.imageUrls ?: run {
                    _state.update { it.copy(isLoading = false, error = "Unexpected content type") }
                    return@launch
                }
                val chapters = chapterRepository.observeChapters(seriesStub).first()
                val idx = chapters.indexOfFirst { it.chapter.url == chapter.url }
                _state.update {
                    it.copy(
                        pages = pages,
                        isLoading = false,
                        hasPreviousChapter = idx > 0,
                        hasNextChapter = idx in 0 until chapters.lastIndex,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load chapter") }
                _effects.send(MangaReaderEffect.ShowError(e.message ?: "Failed to load chapter"))
            }
        }
    }

    private fun markReadIfLoaded() {
        viewModelScope.launch {
            val chapter = _state.value.chapter ?: return@launch
            chapterRepository.markRead(chapter, true)
        }
    }

    private fun navigateChapter(forward: Boolean) {
        viewModelScope.launch {
            val current = _state.value.chapter ?: return@launch
            val chapters = chapterRepository.observeChapters(seriesStub).first()
            val idx = chapters.indexOfFirst { it.chapter.url == current.url }
            val target = if (forward) chapters.getOrNull(idx + 1) else chapters.getOrNull(idx - 1)
            if (target != null) {
                _effects.send(MangaReaderEffect.NavigateToChapter(target.chapter))
            }
        }
    }
}

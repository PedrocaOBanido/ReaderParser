package com.opus.readerparser.ui.reader

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ContentType

data class ReaderUiState(
    val chapter: Chapter? = null,
    val chapterUrl: String? = null,
    val seriesChapters: List<Chapter> = emptyList(),
    val contentType: ContentType = ContentType.NOVEL,
    val html: String = "",
    val pages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val progress: Float = 0f,
    val currentPage: Int = 0,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
)

sealed interface ReaderAction {
    data class Load(val chapter: Chapter) : ReaderAction
    data class SelectChapter(val chapter: Chapter) : ReaderAction
    data class SetProgress(val progress: Float) : ReaderAction
    data class SetPage(val page: Int) : ReaderAction
    data object PreviousChapter : ReaderAction
    data object NextChapter : ReaderAction
    data object OpenChapterList : ReaderAction
    data object DownloadChapter : ReaderAction
    data object Retry : ReaderAction
}

sealed interface ReaderEffect {
    data class NavigateToChapter(val chapter: Chapter) : ReaderEffect
    data object ShowChapterList : ReaderEffect
    data class ShowError(val message: String) : ReaderEffect
    data class ShowSnackbar(val message: String) : ReaderEffect
}

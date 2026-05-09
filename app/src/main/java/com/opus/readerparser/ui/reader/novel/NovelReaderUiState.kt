package com.opus.readerparser.ui.reader.novel

import com.opus.readerparser.domain.model.Chapter

data class NovelReaderUiState(
    val chapter: Chapter? = null,
    val html: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val progress: Float = 0f,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
)

sealed interface NovelReaderAction {
    data class Load(val chapter: Chapter) : NovelReaderAction
    data class SetProgress(val progress: Float) : NovelReaderAction
    data object PreviousChapter : NovelReaderAction
    data object NextChapter : NovelReaderAction
    data object OpenChapterList : NovelReaderAction
}

sealed interface NovelReaderEffect {
    data class NavigateToChapter(val chapter: Chapter) : NovelReaderEffect
    data object ShowChapterList : NovelReaderEffect
    data class ShowError(val message: String) : NovelReaderEffect
}

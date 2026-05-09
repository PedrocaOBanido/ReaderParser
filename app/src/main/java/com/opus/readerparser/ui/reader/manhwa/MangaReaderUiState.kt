package com.opus.readerparser.ui.reader.manhwa

import com.opus.readerparser.domain.model.Chapter

data class MangaReaderUiState(
    val chapter: Chapter? = null,
    val pages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
)

sealed interface MangaReaderAction {
    data class Load(val chapter: Chapter) : MangaReaderAction
    data class SetPage(val page: Int) : MangaReaderAction
    data object PreviousChapter : MangaReaderAction
    data object NextChapter : MangaReaderAction
    data object OpenChapterList : MangaReaderAction
}

sealed interface MangaReaderEffect {
    data class NavigateToChapter(val chapter: Chapter) : MangaReaderEffect
    data object ShowChapterList : MangaReaderEffect
    data class ShowError(val message: String) : MangaReaderEffect
}

package com.opus.readerparser.ui.series

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series

data class SeriesUiState(
    val series: Series? = null,
    val chapters: List<ChapterWithState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inLibrary: Boolean = false,
    val showRangePicker: Boolean = false,
)

sealed interface SeriesAction {
    data object Refresh : SeriesAction
    data class ToggleLibrary(val inLibrary: Boolean) : SeriesAction
    data class OpenChapter(val chapter: Chapter) : SeriesAction
    data object DownloadUnread : SeriesAction
    data object ShowRangePicker : SeriesAction
    data object DismissRangePicker : SeriesAction
    data class DownloadRange(val startIndex: Int, val endIndex: Int) : SeriesAction
}

sealed interface SeriesEffect {
    data class NavigateToReader(val chapter: Chapter, val type: ContentType) : SeriesEffect
    data class ShowError(val message: String) : SeriesEffect
    data class ShowSnackbar(val message: String) : SeriesEffect
}

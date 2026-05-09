package com.opus.readerparser.ui.library

import com.opus.readerparser.domain.model.Series

enum class LibrarySortBy { TITLE, DEFAULT }

data class LibraryUiState(
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortBy: LibrarySortBy = LibrarySortBy.DEFAULT,
    val filterUnreadOnly: Boolean = false,
)

sealed interface LibraryAction {
    data class OpenSeries(val series: Series) : LibraryAction
    data class RemoveFromLibrary(val series: Series) : LibraryAction
    data class SetSortBy(val sortBy: LibrarySortBy) : LibraryAction
    data class SetFilterUnreadOnly(val enabled: Boolean) : LibraryAction
}

sealed interface LibraryEffect {
    data class NavigateToSeries(val series: Series) : LibraryEffect
    data class ShowError(val message: String) : LibraryEffect
}

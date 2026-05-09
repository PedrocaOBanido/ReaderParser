package com.opus.readerparser.ui.downloads

import com.opus.readerparser.domain.model.DownloadItem

data class DownloadsUiState(
    val downloads: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface DownloadsAction {
    data class Cancel(val sourceId: Long, val chapterUrl: String) : DownloadsAction
    data class Retry(val sourceId: Long, val chapterUrl: String) : DownloadsAction
}

sealed interface DownloadsEffect {
    data class ShowError(val message: String) : DownloadsEffect
}

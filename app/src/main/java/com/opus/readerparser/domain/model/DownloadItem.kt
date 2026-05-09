package com.opus.readerparser.domain.model

data class DownloadItem(
    val sourceId: Long,
    val chapterUrl: String,
    val state: DownloadState,
    val progress: Float,
    val errorMessage: String?,
)

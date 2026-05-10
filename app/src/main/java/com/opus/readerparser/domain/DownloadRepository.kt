package com.opus.readerparser.domain

import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeQueue(): Flow<List<DownloadItem>>
    suspend fun cancel(sourceId: Long, chapterUrl: String)
    suspend fun retry(sourceId: Long, chapterUrl: String)
    suspend fun updateQueueState(
        sourceId: Long,
        chapterUrl: String,
        state: DownloadState,
        progress: Float,
        errorMessage: String? = null,
    )
}

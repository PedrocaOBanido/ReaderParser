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

    /**
     * Cancels an in-progress batch download.
     *
     * Removes all QUEUED items whose chapter URLs are in [chapterUrls]
     * and cancels the active WorkManager worker if it belongs to this batch.
     */
    suspend fun cancelBatch(sourceId: Long, chapterUrls: Set<String>)

    /**
     * Deletes a completed download's files and removes it from the queue.
     *
     * The chapter's `downloaded` flag in the chapter table is also set to false.
     */
    suspend fun deleteDownload(sourceId: Long, chapterUrl: String)
}

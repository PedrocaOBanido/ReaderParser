package com.opus.readerparser.domain

/**
 * Use-case for enqueueing chapter downloads into the background queue.
 *
 * Implementations live in `data/repository/` and coordinate with both
 * [DownloadRepository] and WorkManager. ViewModels depend on this
 * interface only — they never touch WorkManager or the DAO directly.
 */
interface DownloadEnqueuer {

    /**
     * Enqueues a single chapter for download.
     *
     * If the chapter is already QUEUED or RUNNING, the call is a no-op
     * (no duplicate work request is created).
     */
    suspend fun enqueueChapter(sourceId: Long, chapterUrl: String)

    /**
     * Enqueues a batch of chapters for download.
     *
     * Chapters are inserted in the order given and executed sequentially
     * by WorkManager. Duplicate chapters (already QUEUED/RUNNING) are
     * silently skipped.
     *
     * @param sourceId the source these chapters belong to.
     * @param chapterUrls ordered list of chapter URLs to download.
     */
    suspend fun enqueueBatch(sourceId: Long, chapterUrls: List<String>)
}

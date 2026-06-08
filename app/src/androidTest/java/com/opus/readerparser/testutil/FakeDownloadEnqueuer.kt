package com.opus.readerparser.testutil

import com.opus.readerparser.domain.DownloadEnqueuer

/**
 * No-op fake for [DownloadEnqueuer] used in androidTest compose UI tests.
 *
 * Download enqueue calls are recorded so tests can assert they happened,
 * but no WorkManager or Room interaction occurs.
 */
class FakeDownloadEnqueuer : DownloadEnqueuer {

    val enqueueChapterCalls = mutableListOf<Pair<Long, String>>()
    val enqueueBatchCalls = mutableListOf<Pair<Long, List<String>>>()

    override suspend fun enqueueChapter(sourceId: Long, chapterUrl: String) {
        enqueueChapterCalls.add(sourceId to chapterUrl)
    }

    override suspend fun enqueueBatch(sourceId: Long, chapterUrls: List<String>) {
        enqueueBatchCalls.add(sourceId to chapterUrls)
    }
}

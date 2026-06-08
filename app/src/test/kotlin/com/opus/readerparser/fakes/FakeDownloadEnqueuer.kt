package com.opus.readerparser.fakes

import com.opus.readerparser.domain.DownloadEnqueuer

class FakeDownloadEnqueuer : DownloadEnqueuer {

    data class EnqueueChapterCall(val sourceId: Long, val chapterUrl: String)
    data class EnqueueBatchCall(val sourceId: Long, val chapterUrls: List<String>)

    val enqueueChapterCalls: MutableList<EnqueueChapterCall> = mutableListOf()
    val enqueueBatchCalls: MutableList<EnqueueBatchCall> = mutableListOf()

    override suspend fun enqueueChapter(sourceId: Long, chapterUrl: String) {
        enqueueChapterCalls.add(EnqueueChapterCall(sourceId, chapterUrl))
    }

    override suspend fun enqueueBatch(sourceId: Long, chapterUrls: List<String>) {
        enqueueBatchCalls.add(EnqueueBatchCall(sourceId, chapterUrls))
    }
}

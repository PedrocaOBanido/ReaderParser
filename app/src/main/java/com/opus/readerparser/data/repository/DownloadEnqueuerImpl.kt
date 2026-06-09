package com.opus.readerparser.data.repository

import androidx.work.ExistingWorkPolicy
import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.domain.DownloadEnqueuer
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.workers.ChapterDownloadWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEnqueuerImpl @Inject constructor(
    private val dao: DownloadQueueDao,
    private val workManager: WorkManagerHelper,
) : DownloadEnqueuer {

    override suspend fun enqueueChapter(sourceId: Long, chapterUrl: String) {
        val existingState = dao.getState(sourceId, chapterUrl)
        if (existingState == DownloadState.QUEUED.name || existingState == DownloadState.RUNNING.name) {
            return // duplicate — no-op
        }

        dao.upsert(
            DownloadQueueEntity(
                sourceId = sourceId,
                chapterUrl = chapterUrl,
                state = DownloadState.QUEUED.name,
            ),
        )

        val request = ChapterDownloadWorker.buildRequest(sourceId, chapterUrl)
        workManager.enqueueUniqueWork(
            workName(sourceId, chapterUrl),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun enqueueBatch(sourceId: Long, chapterUrls: List<String>) {
        // Filter out chapters that are already QUEUED or RUNNING
        val toEnqueue = chapterUrls.filter { url ->
            val existingState = dao.getState(sourceId, url)
            existingState != DownloadState.QUEUED.name && existingState != DownloadState.RUNNING.name
        }
        if (toEnqueue.isEmpty()) return

        // Insert all chapters into the queue
        for (chapterUrl in toEnqueue) {
            dao.upsert(
                DownloadQueueEntity(
                    sourceId = sourceId,
                    chapterUrl = chapterUrl,
                    state = DownloadState.QUEUED.name,
                ),
            )
        }

        // Build a sequential chain so chapters download one at a time
        val requests = toEnqueue.map { chapterUrl ->
            ChapterDownloadWorker.buildRequest(sourceId, chapterUrl)
        }
        val batchWorkName = "batch-$sourceId-${hashUrl(toEnqueue.joinToString(","))}"
        workManager.enqueueChain(
            workName = batchWorkName,
            policy = ExistingWorkPolicy.KEEP,
            requests = requests,
        )
    }

    private fun workName(sourceId: Long, chapterUrl: String): String =
        "download-$sourceId-${hashUrl(chapterUrl)}"
}

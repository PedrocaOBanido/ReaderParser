package com.opus.readerparser.data.repository

import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.dao.DownloadQueueWithDetails
import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.DownloadRepository
import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val dao: DownloadQueueDao,
    private val downloadStore: DownloadStore,
    private val chapterRepository: ChapterRepository,
    private val workManager: WorkManagerHelper,
) : DownloadRepository {

    override fun observeQueue(): Flow<List<DownloadItem>> =
        dao.observeAllWithDetails().map { rows -> rows.map { it.toDomain() } }

    override suspend fun cancel(sourceId: Long, chapterUrl: String) {
        dao.delete(sourceId, chapterUrl)
    }

    override suspend fun retry(sourceId: Long, chapterUrl: String) {
        dao.updateState(sourceId, chapterUrl, DownloadState.QUEUED.name, 0f)
    }

    override suspend fun updateQueueState(
        sourceId: Long,
        chapterUrl: String,
        state: DownloadState,
        progress: Float,
        errorMessage: String?,
    ) {
        dao.updateStateWithError(sourceId, chapterUrl, state.name, progress, errorMessage)
    }

    override suspend fun cancelBatch(sourceId: Long, chapterUrls: Set<String>) {
        dao.deleteBatch(sourceId, chapterUrls.toList())
        if (chapterUrls.isNotEmpty()) {
            // Cancel individual chapter work items
            for (chapterUrl in chapterUrls) {
                val tag = "download-$sourceId-${hashUrl(chapterUrl)}"
                workManager.cancelAllWorkByTag(tag)
            }
            // Also cancel the batch chain if one exists
            val sortedUrls = chapterUrls.sorted()
            val batchWorkName = "batch-$sourceId-${hashUrl(sortedUrls.joinToString(","))}"
            workManager.cancelAllWorkByTag(batchWorkName)
        }
    }

    override suspend fun deleteDownload(sourceId: Long, chapterUrl: String) {
        val chapter = chapterRepository.findByUrl(sourceId, chapterUrl)
        if (chapter != null) {
            // Chapter row exists — use the precise delete path
            downloadStore.delete(chapter)
            chapterRepository.markDownloaded(chapter, false)
        } else {
            // Chapter row was cleaned up (stale-row cleanup) but files may
            // still exist on disk. Delete by hash search.
            val chapterUrlHash = hashUrl(chapterUrl)
            downloadStore.deleteByHash(sourceId, chapterUrlHash)
        }
        dao.delete(sourceId, chapterUrl)
    }

    private fun DownloadQueueWithDetails.toDomain() = DownloadItem(
        sourceId = sourceId,
        chapterUrl = chapterUrl,
        seriesTitle = seriesTitle,
        chapterName = chapterName,
        state = DownloadState.valueOf(state),
        progress = progress,
        errorMessage = errorMessage,
    )
}

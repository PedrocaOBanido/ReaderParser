package com.opus.readerparser.data.repository

import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.dao.DownloadQueueWithDetails
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

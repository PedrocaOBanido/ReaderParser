package com.opus.readerparser.fakes

import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.dao.DownloadQueueWithDetails
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-rolled in-memory fake for [DownloadQueueDao].
 *
 * Backed by a [MutableStateFlow] so tests can observe queue changes.
 * All query methods return correct results based on the in-memory store.
 */
class FakeDownloadQueueDao : DownloadQueueDao {

    private val store = MutableStateFlow<List<DownloadQueueEntity>>(emptyList())

    override fun observeAll(): Flow<List<DownloadQueueEntity>> = store

    override fun observeAllWithDetails(): Flow<List<DownloadQueueWithDetails>> =
        store.map { entities ->
            entities.map { entity ->
                DownloadQueueWithDetails(
                    sourceId = entity.sourceId,
                    chapterUrl = entity.chapterUrl,
                    state = entity.state,
                    progress = entity.progress,
                    errorMessage = entity.errorMessage,
                    chapterName = "", // Not needed for enqueuer tests
                    seriesTitle = "", // Not needed for enqueuer tests
                )
            }
        }

    override fun observeQueued(): Flow<List<DownloadQueueEntity>> =
        store.map { entities -> entities.filter { it.state == "QUEUED" } }

    override suspend fun upsert(entry: DownloadQueueEntity) {
        val current = store.value.toMutableList()
        val idx = current.indexOfFirst {
            it.sourceId == entry.sourceId && it.chapterUrl == entry.chapterUrl
        }
        if (idx >= 0) {
            current[idx] = entry
        } else {
            current.add(entry)
        }
        store.value = current
    }

    override suspend fun updateState(
        sourceId: Long,
        chapterUrl: String,
        state: String,
        progress: Float,
    ) {
        val current = store.value.toMutableList()
        val idx = current.indexOfFirst {
            it.sourceId == sourceId && it.chapterUrl == chapterUrl
        }
        if (idx >= 0) {
            current[idx] = current[idx].copy(state = state, progress = progress)
            store.value = current
        }
    }

    override suspend fun updateStateWithError(
        sourceId: Long,
        chapterUrl: String,
        state: String,
        progress: Float,
        errorMessage: String?,
    ) {
        val current = store.value.toMutableList()
        val idx = current.indexOfFirst {
            it.sourceId == sourceId && it.chapterUrl == chapterUrl
        }
        if (idx >= 0) {
            current[idx] = current[idx].copy(
                state = state,
                progress = progress,
                errorMessage = errorMessage,
            )
            store.value = current
        }
    }

    override suspend fun delete(sourceId: Long, chapterUrl: String) {
        store.value = store.value.filter {
            it.sourceId != sourceId || it.chapterUrl != chapterUrl
        }
    }

    override suspend fun deleteBatch(sourceId: Long, chapterUrls: List<String>) {
        store.value = store.value.filter {
            it.sourceId != sourceId || it.chapterUrl !in chapterUrls
        }
    }

    override suspend fun getState(sourceId: Long, chapterUrl: String): String? =
        store.value.find {
            it.sourceId == sourceId && it.chapterUrl == chapterUrl
        }?.state
}

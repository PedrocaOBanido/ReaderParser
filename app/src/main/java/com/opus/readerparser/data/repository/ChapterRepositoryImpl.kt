package com.opus.readerparser.data.repository

import android.util.Log
import com.opus.readerparser.data.local.database.dao.ChapterDao
import com.opus.readerparser.data.local.database.mappers.toChapterWithState
import com.opus.readerparser.data.local.database.mappers.toDomain
import com.opus.readerparser.data.local.database.mappers.toEntity
import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.Series
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val chapterDao: ChapterDao,
    private val downloadStore: DownloadStore,
) : ChapterRepository {

    override fun observeChapters(series: Series): Flow<List<ChapterWithState>> =
        chapterDao.observeChapters(series.sourceId, series.url).map { entities ->
            entities.map { it.toChapterWithState() }
        }

    override suspend fun refreshChapters(series: Series) {
        val remote = sourceRegistry[series.sourceId].getChapterList(series)
        val remoteEntities = remote.map { it.toEntity() }
        val existingChapters = chapterDao.getChaptersForSeries(series.sourceId, series.url)
        val existingMap = existingChapters.associateBy { it.url }

        val merged = remoteEntities.map { entity ->
            val existing = existingMap[entity.url]
            if (existing != null) {
                entity.copy(
                    read = existing.read,
                    progress = existing.progress,
                    downloaded = existing.downloaded,
                )
            } else {
                entity
            }
        }

        chapterDao.deleteBySeries(series.sourceId, series.url)
        chapterDao.upsertAll(merged)
    }

    override suspend fun findByUrl(sourceId: Long, url: String): Chapter? =
        chapterDao.getByUrl(sourceId, url)?.toDomain()

    override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent {
        if (!forceNetwork) {
            // Offline-first: try local cache before hitting the network.
            // Corrupt or unreadable cache entries are treated as a miss
            // so the caller falls back to the remote source.
            try {
                val cached = downloadStore.read(chapter)
                if (cached != null) return cached
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read cached content for ${chapter.url}, falling back to network", e)
            }
        }
        return sourceRegistry[chapter.sourceId].getChapterContent(chapter)
    }

    override suspend fun markRead(chapter: Chapter, read: Boolean) {
        chapterDao.markRead(chapter.sourceId, chapter.url, read)
    }

    override suspend fun setProgress(chapter: Chapter, progress: Float) {
        chapterDao.setProgress(chapter.sourceId, chapter.url, progress)
    }

    override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {
        chapterDao.markDownloaded(chapter.sourceId, chapter.url, downloaded)
    }

    companion object {
        private const val TAG = "ChapterRepositoryImpl"
    }
}

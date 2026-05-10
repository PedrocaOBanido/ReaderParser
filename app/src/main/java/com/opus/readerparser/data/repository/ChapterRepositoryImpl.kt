package com.opus.readerparser.data.repository

import com.opus.readerparser.data.local.database.dao.ChapterDao
import com.opus.readerparser.data.local.database.mappers.toChapterWithState
import com.opus.readerparser.data.local.database.mappers.toDomain
import com.opus.readerparser.data.local.database.mappers.toEntity
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.Series
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val chapterDao: ChapterDao,
) : ChapterRepository {

    override fun observeChapters(series: Series): Flow<List<ChapterWithState>> =
        chapterDao.observeChapters(series.sourceId, series.url).map { entities ->
            entities.map { it.toChapterWithState() }
        }

    override suspend fun refreshChapters(series: Series) {
        val remote = sourceRegistry[series.sourceId].getChapterList(series)
        chapterDao.upsertAll(remote.map { it.toEntity() })
    }

    override suspend fun findByUrl(sourceId: Long, url: String): Chapter? =
        chapterDao.getByUrl(sourceId, url)?.toDomain()

    override suspend fun getContent(chapter: Chapter): ChapterContent =
        sourceRegistry[chapter.sourceId].getChapterContent(chapter)

    override suspend fun markRead(chapter: Chapter, read: Boolean) {
        chapterDao.markRead(chapter.sourceId, chapter.url, read)
    }

    override suspend fun setProgress(chapter: Chapter, progress: Float) {
        chapterDao.setProgress(chapter.sourceId, chapter.url, progress)
    }

    override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {
        chapterDao.markDownloaded(chapter.sourceId, chapter.url, downloaded)
    }
}

package com.opus.readerparser.testutil

import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.Series
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ReaderScreenTestChapterRepository(
    chapters: List<ChapterWithState>,
    private val content: ChapterContent,
) : ChapterRepository {

    private val chapterState = MutableStateFlow(chapters)

    override fun observeChapters(series: Series): Flow<List<ChapterWithState>> = chapterState

    override suspend fun refreshChapters(series: Series) = Unit

    override suspend fun findByUrl(sourceId: Long, url: String): Chapter? =
        chapterState.value.firstOrNull { it.chapter.sourceId == sourceId && it.chapter.url == url }?.chapter

    override suspend fun getContent(chapter: Chapter): ChapterContent = content

    override suspend fun markRead(chapter: Chapter, read: Boolean) = Unit

    override suspend fun setProgress(chapter: Chapter, progress: Float) = Unit

    override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) = Unit
}

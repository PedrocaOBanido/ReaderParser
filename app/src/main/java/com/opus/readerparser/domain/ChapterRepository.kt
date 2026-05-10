package com.opus.readerparser.domain

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.Series
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun observeChapters(series: Series): Flow<List<ChapterWithState>>

    suspend fun refreshChapters(series: Series)

    /** Returns the chapter for the given identity pair, or null if not cached locally. */
    suspend fun findByUrl(sourceId: Long, url: String): Chapter?

    suspend fun getContent(chapter: Chapter): ChapterContent

    suspend fun markRead(chapter: Chapter, read: Boolean)
    suspend fun setProgress(chapter: Chapter, progress: Float)
    suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean)
}

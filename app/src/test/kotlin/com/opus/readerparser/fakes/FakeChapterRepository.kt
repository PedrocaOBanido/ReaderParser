package com.opus.readerparser.fakes

import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.Series
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-rolled fake [ChapterRepository] for ViewModel tests.
 *
 * Behaviour is configured via mutable public properties before the test
 * runs.  Each method also records every invocation so tests can assert
 * that the ViewModel called the right methods with the right arguments.
 *
 * Chapter states are backed by a [MutableStateFlow] keyed by series URL —
 * tests can pre-populate states, assert on them, or observe changes.
 */
class FakeChapterRepository : ChapterRepository {

    // -- configurable return values --
    var contentResult: ChapterContent? = null

    // -- call recording --
    val refreshChaptersCalls: MutableList<Series> = mutableListOf()
    val getContentCalls: MutableList<Chapter> = mutableListOf()
    val markReadCalls: MutableList<Pair<Chapter, Boolean>> = mutableListOf()
    val setProgressCalls: MutableList<Pair<Chapter, Float>> = mutableListOf()
    val markDownloadedCalls: MutableList<Pair<Chapter, Boolean>> = mutableListOf()

    // -- chapter state --
    private val _chapterStates = MutableStateFlow<Map<String, List<ChapterWithState>>>(emptyMap())

    override fun observeChapters(series: Series): Flow<List<ChapterWithState>> =
        _chapterStates.map { states -> states[series.url] ?: emptyList() }

    override suspend fun refreshChapters(series: Series) {
        refreshChaptersCalls.add(series)
    }

    /** Pre-populate via [setChapters]; returns the first match for (sourceId, url). */
    override suspend fun findByUrl(sourceId: Long, url: String): Chapter? =
        _chapterStates.value.values.flatten()
            .firstOrNull { it.chapter.sourceId == sourceId && it.chapter.url == url }
            ?.chapter

    override suspend fun getContent(chapter: Chapter): ChapterContent {
        getContentCalls.add(chapter)
        return contentResult ?: ChapterContent.Text("<p>Test content</p>")
    }

    override suspend fun markRead(chapter: Chapter, read: Boolean) {
        markReadCalls.add(chapter to read)
        updateChapterState(chapter) { it.copy(read = read) }
    }

    override suspend fun setProgress(chapter: Chapter, progress: Float) {
        setProgressCalls.add(chapter to progress)
        updateChapterState(chapter) { it.copy(progress = progress) }
    }

    override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {
        markDownloadedCalls.add(chapter to downloaded)
        updateChapterState(chapter) { it.copy(downloaded = downloaded) }
    }

    // -- helpers for tests --

    fun setChapters(seriesUrl: String, chapters: List<ChapterWithState>) {
        _chapterStates.value = _chapterStates.value + (seriesUrl to chapters)
    }

    fun chaptersFor(seriesUrl: String): List<ChapterWithState> =
        _chapterStates.value[seriesUrl] ?: emptyList()

    private fun updateChapterState(
        chapter: Chapter,
        transform: (ChapterWithState) -> ChapterWithState,
    ) {
        val current = _chapterStates.value.toMutableMap()
        val key = chapter.seriesUrl
        val list = (current[key] ?: emptyList()).toMutableList()
        val idx = list.indexOfFirst { it.chapter.url == chapter.url }
        if (idx >= 0) {
            list[idx] = transform(list[idx])
            current[key] = list
            _chapterStates.value = current
        }
    }
}

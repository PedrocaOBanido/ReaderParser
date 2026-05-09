package com.opus.readerparser.data.repository

import com.opus.readerparser.data.local.database.dao.ChapterDao
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.data.local.database.mappers.toEntity
import com.opus.readerparser.data.source.Source
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.core.util.computeSourceId
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.fakes.FakeSource
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for [ChapterRepositoryImpl] using hand-rolled fakes.
 *
 * Verifies that the repository correctly delegates to [SourceRegistry] for
 * network operations and to [ChapterDao] for persistence, and that source
 * exceptions propagate without being caught or wrapped.
 */
class ChapterRepositoryImplTest {

    // ---- Hand-rolled fake DAO (in-memory list) ----

    private class FakeChapterDao : ChapterDao {

        private val store = mutableListOf<ChapterEntity>()

        override fun observeChapters(sourceId: Long, seriesUrl: String) =
            flowOf(store.filter { it.sourceId == sourceId && it.seriesUrl == seriesUrl })

        override suspend fun getByUrl(sourceId: Long, url: String): ChapterEntity? =
            store.find { it.sourceId == sourceId && it.url == url }

        override suspend fun upsertAll(chapters: List<ChapterEntity>) {
            chapters.forEach { chapter ->
                val idx = store.indexOfFirst {
                    it.sourceId == chapter.sourceId && it.url == chapter.url
                }
                if (idx >= 0) store[idx] = chapter else store.add(chapter)
            }
        }

        override suspend fun markRead(sourceId: Long, url: String, read: Boolean) {
            val idx = store.indexOfFirst { it.sourceId == sourceId && it.url == url }
            if (idx >= 0) store[idx] = store[idx].copy(read = read)
        }

        override suspend fun setProgress(sourceId: Long, url: String, progress: Float) {
            val idx = store.indexOfFirst { it.sourceId == sourceId && it.url == url }
            if (idx >= 0) store[idx] = store[idx].copy(progress = progress)
        }

        override suspend fun deleteBySeries(sourceId: Long, seriesUrl: String) {
            store.removeAll { it.sourceId == sourceId && it.seriesUrl == seriesUrl }
        }
    }

    // ---- Test fixtures ----

    private val fakeSource = FakeSource(name = "TestSource", lang = "en", type = ContentType.NOVEL)

    private val sourceRegistry = SourceRegistry(mapOf(fakeSource.id to fakeSource))

    private val fakeDao = FakeChapterDao()

    private val repository = ChapterRepositoryImpl(sourceRegistry, fakeDao)

    private val testSeries = TestFixtures.testSeries(sourceId = fakeSource.id)

    private val testChapter = TestFixtures.testChapter(
        seriesUrl = testSeries.url,
        sourceId = fakeSource.id,
    )

    // -----------------------------------------------------------------
    // observeChapters
    // -----------------------------------------------------------------

    @Test
    fun `observeChapters emits chapters filtered by sourceId and seriesUrl`() = runTest {
        val chapter1 = testChapter.copy(url = "https://test.invalid/chapter/1", number = 1f)
        val chapter2 = testChapter.copy(url = "https://test.invalid/chapter/2", number = 2f)
        val otherSeriesChapter = testChapter.copy(
            url = "https://test.invalid/chapter/other",
            seriesUrl = "https://test.invalid/series/other",
        )
        fakeDao.upsertAll(listOf(chapter1.toEntity(), chapter2.toEntity(), otherSeriesChapter.toEntity()))

        val result = repository.observeChapters(testSeries).first()

        assertEquals(2, result.size)
        assertEquals(chapter1.url, result[0].chapter.url)
        assertEquals(chapter2.url, result[1].chapter.url)
    }

    @Test
    fun `observeChapters returns empty list when no chapters exist`() = runTest {
        val result = repository.observeChapters(testSeries).first()

        assertEquals(0, result.size)
    }

    @Test
    fun `observeChapters does not include chapters from different series`() = runTest {
        val otherSeriesChapter = testChapter.copy(
            url = "https://test.invalid/chapter/other",
            seriesUrl = "https://test.invalid/series/other",
        )
        fakeDao.upsertAll(listOf(otherSeriesChapter.toEntity()))

        val result = repository.observeChapters(testSeries).first()

        assertEquals(0, result.size)
    }

    // -----------------------------------------------------------------
    // refreshChapters
    // -----------------------------------------------------------------

    @Test
    fun `refreshChapters fetches chapter list from Source and upserts all to DAO`() = runTest {
        val remoteChapters = listOf(
            testChapter.copy(url = "https://test.invalid/chapter/1", number = 1f),
            testChapter.copy(url = "https://test.invalid/chapter/2", number = 2f),
        )
        fakeSource.chapterListResult = remoteChapters

        repository.refreshChapters(testSeries)

        assertEquals(listOf(testSeries), fakeSource.getChapterListCalls)

        val stored = repository.observeChapters(testSeries).first()
        assertEquals(2, stored.size)
        assertEquals(remoteChapters[0].url, stored[0].chapter.url)
        assertEquals(remoteChapters[1].url, stored[1].chapter.url)
    }

    @Test
    fun `refreshChapters upserts empty list when Source returns no chapters`() = runTest {
        fakeSource.chapterListResult = emptyList()

        repository.refreshChapters(testSeries)

        val stored = repository.observeChapters(testSeries).first()
        assertEquals(0, stored.size)
    }

    // -----------------------------------------------------------------
    // getContent
    // -----------------------------------------------------------------

    @Test
    fun `getContent delegates to Source getChapterContent for novel`() = runTest {
        val expectedContent = TestFixtures.testTextContent(html = "<p>Hello</p>")
        fakeSource.chapterContentResult = expectedContent

        val result = repository.getContent(testChapter)

        assertEquals(expectedContent, result)
        assertEquals(listOf(testChapter), fakeSource.getChapterContentCalls)
    }

    @Test
    fun `getContent delegates to Source getChapterContent for manhwa`() = runTest {
        val manhwaSource = FakeSource(name = "ManhwaSource", lang = "en", type = ContentType.MANHWA)
        val manhwaRegistry = SourceRegistry(mapOf(manhwaSource.id to manhwaSource))
        val manhwaRepo = ChapterRepositoryImpl(manhwaRegistry, fakeDao)
        val manhwaChapter = testChapter.copy(sourceId = manhwaSource.id)
        val expectedContent = TestFixtures.testPagesContent(
            imageUrls = listOf("https://test.invalid/page/1.jpg", "https://test.invalid/page/2.jpg"),
        )
        manhwaSource.chapterContentResult = expectedContent

        val result = manhwaRepo.getContent(manhwaChapter)

        assertEquals(expectedContent, result)
        assertEquals(listOf(manhwaChapter), manhwaSource.getChapterContentCalls)
    }

    @Test
    fun `getContent does not check downloads - always delegates to Source`() = runTest {
        val chapterInDao = testChapter.toEntity().copy(downloaded = true)
        fakeDao.upsertAll(listOf(chapterInDao))

        val expectedContent = TestFixtures.testTextContent()
        fakeSource.chapterContentResult = expectedContent

        val result = repository.getContent(testChapter)

        assertEquals(listOf(testChapter), fakeSource.getChapterContentCalls)
        assertEquals(expectedContent, result)
    }

    // -----------------------------------------------------------------
    // markRead
    // -----------------------------------------------------------------

    @Test
    fun `markRead delegates to DAO markRead with true`() = runTest {
        fakeDao.upsertAll(listOf(testChapter.toEntity()))

        repository.markRead(testChapter, read = true)

        val stored = fakeDao.getByUrl(testChapter.sourceId, testChapter.url)!!
        assertEquals(true, stored.read)
    }

    @Test
    fun `markRead delegates to DAO markRead with false`() = runTest {
        fakeDao.upsertAll(listOf(testChapter.toEntity().copy(read = true)))

        repository.markRead(testChapter, read = false)

        val stored = fakeDao.getByUrl(testChapter.sourceId, testChapter.url)!!
        assertEquals(false, stored.read)
    }

    // -----------------------------------------------------------------
    // setProgress
    // -----------------------------------------------------------------

    @Test
    fun `setProgress delegates to DAO setProgress`() = runTest {
        fakeDao.upsertAll(listOf(testChapter.toEntity()))

        repository.setProgress(testChapter, 0.75f)

        val stored = fakeDao.getByUrl(testChapter.sourceId, testChapter.url)!!
        assertEquals(0.75f, stored.progress, 0.001f)
    }

    // -----------------------------------------------------------------
    // ChapterWithState.downloaded is always false
    // -----------------------------------------------------------------

    @Test
    fun `observeChapters maps downloaded to false from DAO`() = runTest {
        val entity = testChapter.toEntity().copy(downloaded = false)
        fakeDao.upsertAll(listOf(entity))

        val result = repository.observeChapters(testSeries).first()

        assertEquals(1, result.size)
        assertFalse(result[0].downloaded)
    }

    // -----------------------------------------------------------------
    // Error propagation: Source exceptions propagate without being caught
    // -----------------------------------------------------------------

    @Test
    fun `refreshChapters propagates Source exception`() = runTest {
        val throwingSource = object : Source {
            override val id: Long = computeSourceId("ThrowingSource", "en", ContentType.NOVEL)
            override val name = "ThrowingSource"
            override val lang = "en"
            override val baseUrl = "https://throwing.invalid"
            override val type = ContentType.NOVEL
            override suspend fun getPopular(page: Int) = error("not implemented")
            override suspend fun getLatest(page: Int) = error("not implemented")
            override suspend fun search(query: String, page: Int, filters: FilterList) =
                error("not implemented")
            override suspend fun getSeriesDetails(series: Series) = error("not implemented")
            override suspend fun getChapterList(series: Series): List<Chapter> =
                throw RuntimeException("Source error")
            override suspend fun getChapterContent(chapter: Chapter): ChapterContent =
                error("not implemented")
        }
        val throwingRegistry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val throwingRepo = ChapterRepositoryImpl(throwingRegistry, fakeDao)
        val series = testSeries.copy(sourceId = throwingSource.id)

        try {
            throwingRepo.refreshChapters(series)
            fail("Expected RuntimeException to propagate")
        } catch (e: RuntimeException) {
            assertEquals("Source error", e.message)
        }
    }

    @Test
    fun `getContent propagates Source exception`() = runTest {
        val throwingSource = object : Source {
            override val id: Long = computeSourceId("ThrowingContentSource", "en", ContentType.NOVEL)
            override val name = "ThrowingContentSource"
            override val lang = "en"
            override val baseUrl = "https://throwing.invalid"
            override val type = ContentType.NOVEL
            override suspend fun getPopular(page: Int) = error("not implemented")
            override suspend fun getLatest(page: Int) = error("not implemented")
            override suspend fun search(query: String, page: Int, filters: FilterList) =
                error("not implemented")
            override suspend fun getSeriesDetails(series: Series) = error("not implemented")
            override suspend fun getChapterList(series: Series) = error("not implemented")
            override suspend fun getChapterContent(chapter: Chapter): ChapterContent =
                throw IllegalStateException("Content error")
        }
        val throwingRegistry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val throwingRepo = ChapterRepositoryImpl(throwingRegistry, fakeDao)
        val chapter = testChapter.copy(sourceId = throwingSource.id)

        try {
            throwingRepo.getContent(chapter)
            fail("Expected IllegalStateException to propagate")
        } catch (e: IllegalStateException) {
            assertEquals("Content error", e.message)
        }
    }

    @Test
    fun `refreshChapters throws for unknown sourceId`() = runTest {
        val unknownSeries = testSeries.copy(sourceId = 99999L)

        try {
            repository.refreshChapters(unknownSeries)
            fail("Expected an error for unknown source ID")
        } catch (_: Exception) {
            // Expected — SourceRegistry throws for unregistered IDs
        }
    }

    @Test
    fun `getContent throws for unknown sourceId`() = runTest {
        val unknownChapter = testChapter.copy(sourceId = 99999L)

        try {
            repository.getContent(unknownChapter)
            fail("Expected an error for unknown source ID")
        } catch (_: Exception) {
            // Expected — SourceRegistry throws for unregistered IDs
        }
    }
}
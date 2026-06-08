package com.opus.readerparser.data.repository

import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.fakes.FakeChapterRepository
import com.opus.readerparser.fakes.FakeDownloadQueueDao
import com.opus.readerparser.fakes.FakeDownloadStore
import com.opus.readerparser.fakes.FakeWorkManagerHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for [DownloadRepositoryImpl].
 *
 * Uses hand-rolled fakes to verify:
 * - cancelBatch removes matching queue rows and cancels WorkManager work by tag.
 * - deleteDownload removes files, resets downloaded flag, and removes queue entry.
 */
class DownloadRepositoryImplTest {

    private lateinit var dao: FakeDownloadQueueDao
    private lateinit var downloadStore: FakeDownloadStore
    private lateinit var chapterRepo: FakeChapterRepository
    private lateinit var workManager: FakeWorkManagerHelper
    private lateinit var repository: DownloadRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeDownloadQueueDao()
        downloadStore = FakeDownloadStore()
        chapterRepo = FakeChapterRepository()
        workManager = FakeWorkManagerHelper()
        repository = DownloadRepositoryImpl(dao, downloadStore, chapterRepo, workManager)
    }

    // -----------------------------------------------------------------
    // cancelBatch
    // -----------------------------------------------------------------

    @Test
    fun `cancelBatch removes all matching queue rows for the given sourceId and chapterUrls`() = runTest {
        // Seed the queue with 3 items for source 1
        val urls = listOf("https://test.invalid/ch/1", "https://test.invalid/ch/2", "https://test.invalid/ch/3")
        for (url in urls) {
            dao.upsert(
                DownloadQueueEntity(sourceId = 1L, chapterUrl = url, state = DownloadState.QUEUED.name),
            )
        }

        repository.cancelBatch(1L, setOf("https://test.invalid/ch/1", "https://test.invalid/ch/3"))

        // Only ch/2 should remain
        assertThat(dao.getState(1L, "https://test.invalid/ch/1")).isNull()
        assertThat(dao.getState(1L, "https://test.invalid/ch/2")).isEqualTo(DownloadState.QUEUED.name)
        assertThat(dao.getState(1L, "https://test.invalid/ch/3")).isNull()
    }

    @Test
    fun `cancelBatch cancels WorkManager work by tag for each chapterUrl and the batch chain`() = runTest {
        val urls = listOf("https://test.invalid/ch/1", "https://test.invalid/ch/2")

        repository.cancelBatch(1L, urls.toSet())

        // 2 individual tags + 1 batch chain tag
        assertThat(workManager.cancelCalls).hasSize(3)
        for (url in urls) {
            val expectedTag = "download-1-${hashUrl(url)}"
            assertThat(workManager.cancelCalls).contains(expectedTag)
        }
        // Batch chain tag should also be cancelled
        val sortedUrls = urls.sorted()
        val batchTag = "batch-1-${hashUrl(sortedUrls.joinToString(","))}"
        assertThat(workManager.cancelCalls).contains(batchTag)
    }

    @Test
    fun `cancelBatch does not affect chapters from different sourceId`() = runTest {
        // Seed items for source 1 and source 2
        dao.upsert(DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1", state = DownloadState.QUEUED.name))
        dao.upsert(DownloadQueueEntity(sourceId = 2L, chapterUrl = "https://test.invalid/ch/1", state = DownloadState.QUEUED.name))

        repository.cancelBatch(1L, setOf("https://test.invalid/ch/1"))

        // Source 1 item should be removed, source 2 item should remain
        assertThat(dao.getState(1L, "https://test.invalid/ch/1")).isNull()
        assertThat(dao.getState(2L, "https://test.invalid/ch/1")).isEqualTo(DownloadState.QUEUED.name)
    }

    @Test
    fun `cancelBatch on empty set does nothing`() = runTest {
        repository.cancelBatch(1L, emptySet())

        assertThat(workManager.cancelCalls).isEmpty()
    }

    // -----------------------------------------------------------------
    // deleteDownload
    // -----------------------------------------------------------------

    @Test
    fun `deleteDownload removes files, resets downloaded flag, and removes queue entry`() = runTest {
        // Seed a queue entry and a chapter in the fake chapter repo
        dao.upsert(
            DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1", state = DownloadState.COMPLETED.name),
        )
        val chapter = com.opus.readerparser.testutil.TestFixtures.testChapter(url = "https://test.invalid/ch/1")
        chapterRepo.setChapters(chapter.seriesUrl, listOf(
            com.opus.readerparser.domain.model.ChapterWithState(chapter, read = false, downloaded = true, progress = 0f),
        ))

        repository.deleteDownload(1L, "https://test.invalid/ch/1")

        assertThat(downloadStore.deleteCalls).containsExactly(chapter)
        assertThat(chapterRepo.markDownloadedCalls).containsExactly(chapter to false)
        assertThat(dao.getState(1L, "https://test.invalid/ch/1")).isNull()
    }

    @Test
    fun `deleteDownload still removes queue entry and files when chapter not found locally`() = runTest {
        dao.upsert(
            DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/missing", state = DownloadState.COMPLETED.name),
        )
        downloadStore.deleteByHashResult = true

        repository.deleteDownload(1L, "https://test.invalid/ch/missing")

        // Queue entry should be removed even if chapter not in repo
        assertThat(dao.getState(1L, "https://test.invalid/ch/missing")).isNull()
        // Files should be deleted via hash search
        assertThat(downloadStore.deleteByHashCalls).hasSize(1)
        assertThat(downloadStore.deleteByHashCalls.first().first).isEqualTo(1L)
    }

    @Test
    fun `deleteDownload uses hash search when chapter row is missing but files may exist`() = runTest {
        dao.upsert(
            DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/orphan", state = DownloadState.COMPLETED.name),
        )
        downloadStore.deleteByHashResult = false

        repository.deleteDownload(1L, "https://test.invalid/ch/orphan")

        // Even if no files found, the hash search was attempted
        assertThat(downloadStore.deleteByHashCalls).hasSize(1)
        assertThat(dao.getState(1L, "https://test.invalid/ch/orphan")).isNull()
    }

    // -----------------------------------------------------------------
    // cancel
    // -----------------------------------------------------------------

    @Test
    fun `cancel removes the queue entry`() = runTest {
        dao.upsert(
            DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1", state = DownloadState.QUEUED.name),
        )

        repository.cancel(1L, "https://test.invalid/ch/1")

        assertThat(dao.getState(1L, "https://test.invalid/ch/1")).isNull()
    }

    // -----------------------------------------------------------------
    // retry
    // -----------------------------------------------------------------

    @Test
    fun `retry resets item to QUEUED state with zero progress`() = runTest {
        dao.upsert(
            DownloadQueueEntity(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1", state = DownloadState.FAILED.name, progress = 0.5f),
        )

        repository.retry(1L, "https://test.invalid/ch/1")

        assertThat(dao.getState(1L, "https://test.invalid/ch/1")).isEqualTo(DownloadState.QUEUED.name)
    }
}

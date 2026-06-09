package com.opus.readerparser.data.repository

import androidx.work.ExistingWorkPolicy
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.fakes.FakeDownloadQueueDao
import com.opus.readerparser.fakes.FakeWorkManagerHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for [DownloadEnqueuerImpl].
 *
 * Uses hand-rolled fakes for [FakeDownloadQueueDao] and
 * [FakeWorkManagerHelper] to verify:
 * - Deduplication: chapters already QUEUED or RUNNING are not re-enqueued.
 * - Correct entity creation with QUEUED state.
 * - WorkManager enqueueUniqueWork called with correct work name and policy.
 * - Batch enqueueing processes chapters in order.
 */
class DownloadEnqueuerImplTest {

    private lateinit var dao: FakeDownloadQueueDao
    private lateinit var workManager: FakeWorkManagerHelper
    private lateinit var enqueuer: DownloadEnqueuerImpl

    @Before
    fun setUp() {
        dao = FakeDownloadQueueDao()
        workManager = FakeWorkManagerHelper()
        enqueuer = DownloadEnqueuerImpl(dao, workManager)
    }

    @Test
    fun `enqueueChapter inserts entity with QUEUED state when chapter not in queue`() = runTest {
        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        val state = dao.getState(1L, "https://test.invalid/ch/1")
        assertThat(state).isEqualTo(DownloadState.QUEUED.name)
    }

    @Test
    fun `enqueueChapter calls WorkManager with correct work name and KEEP policy`() = runTest {
        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        assertThat(workManager.enqueueCalls).hasSize(1)
        val call = workManager.enqueueCalls.first()
        assertThat(call.policy).isEqualTo(ExistingWorkPolicy.KEEP)
        assertThat(call.workName).contains("1-")
    }

    @Test
    fun `enqueueChapter is a no-op when chapter is already QUEUED`() = runTest {
        // Pre-populate the queue with a QUEUED entry
        dao.upsert(
            DownloadQueueEntity(
                sourceId = 1L,
                chapterUrl = "https://test.invalid/ch/1",
                state = DownloadState.QUEUED.name,
            ),
        )

        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        // No new WorkManager call should have been made
        assertThat(workManager.enqueueCalls).isEmpty()
    }

    @Test
    fun `enqueueChapter is a no-op when chapter is already RUNNING`() = runTest {
        dao.upsert(
            DownloadQueueEntity(
                sourceId = 1L,
                chapterUrl = "https://test.invalid/ch/1",
                state = DownloadState.RUNNING.name,
            ),
        )

        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        assertThat(workManager.enqueueCalls).isEmpty()
    }

    @Test
    fun `enqueueChapter re-enqueues when previous attempt was FAILED`() = runTest {
        dao.upsert(
            DownloadQueueEntity(
                sourceId = 1L,
                chapterUrl = "https://test.invalid/ch/1",
                state = DownloadState.FAILED.name,
            ),
        )

        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        // Should re-enqueue since FAILED is not QUEUED or RUNNING
        assertThat(workManager.enqueueCalls).hasSize(1)
        val state = dao.getState(1L, "https://test.invalid/ch/1")
        assertThat(state).isEqualTo(DownloadState.QUEUED.name)
    }

    @Test
    fun `enqueueChapter re-enqueues when previous attempt was COMPLETED`() = runTest {
        dao.upsert(
            DownloadQueueEntity(
                sourceId = 1L,
                chapterUrl = "https://test.invalid/ch/1",
                state = DownloadState.COMPLETED.name,
            ),
        )

        enqueuer.enqueueChapter(sourceId = 1L, chapterUrl = "https://test.invalid/ch/1")

        assertThat(workManager.enqueueCalls).hasSize(1)
    }

    @Test
    fun `enqueueBatch enqueues all chapters as a sequential chain`() = runTest {
        val urls = listOf(
            "https://test.invalid/ch/1",
            "https://test.invalid/ch/2",
            "https://test.invalid/ch/3",
        )

        enqueuer.enqueueBatch(sourceId = 1L, chapterUrls = urls)

        // Each chapter should have been inserted into the queue
        for (url in urls) {
            assertThat(dao.getState(1L, url)).isEqualTo(DownloadState.QUEUED.name)
        }
        // A single chain call should have been made with all 3 requests
        assertThat(workManager.enqueueChainCalls).hasSize(1)
        assertThat(workManager.enqueueChainCalls.first().requests).hasSize(3)
        // No individual enqueue calls should have been made
        assertThat(workManager.enqueueCalls).isEmpty()
    }

    @Test
    fun `enqueueBatch skips already queued chapters`() = runTest {
        // Pre-enqueue chapter 2
        dao.upsert(
            DownloadQueueEntity(
                sourceId = 1L,
                chapterUrl = "https://test.invalid/ch/2",
                state = DownloadState.QUEUED.name,
            ),
        )

        val urls = listOf(
            "https://test.invalid/ch/1",
            "https://test.invalid/ch/2",
            "https://test.invalid/ch/3",
        )

        enqueuer.enqueueBatch(sourceId = 1L, chapterUrls = urls)

        // Only chapters 1 and 3 should be in the chain
        assertThat(workManager.enqueueChainCalls).hasSize(1)
        assertThat(workManager.enqueueChainCalls.first().requests).hasSize(2)
    }

    @Test
    fun `enqueueBatch on empty list does nothing`() = runTest {
        enqueuer.enqueueBatch(sourceId = 1L, chapterUrls = emptyList())

        assertThat(workManager.enqueueChainCalls).isEmpty()
        assertThat(workManager.enqueueCalls).isEmpty()
    }
}

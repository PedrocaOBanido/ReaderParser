package com.opus.readerparser.data.local.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.AppDatabase
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadQueueDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DownloadQueueDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.downloadQueueDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Helper factories ---

    private fun downloadEntity(
        sourceId: Long = 1L,
        chapterUrl: String = "https://example.com/series/1/ch/1",
        state: String = "QUEUED",
        progress: Float = 0f,
        errorMessage: String? = null,
    ) = DownloadQueueEntity(
        sourceId = sourceId,
        chapterUrl = chapterUrl,
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )

    // --- insert (upsert) ---

    @Test
    fun upsert_insertsNewEntry() = runTest {
        val entry = downloadEntity()
        dao.upsert(entry)

        dao.observeQueued().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].chapterUrl).isEqualTo("https://example.com/series/1/ch/1")
            assertThat(items[0].state).isEqualTo("QUEUED")
        }
    }

    @Test
    fun upsert_replacesExistingEntry() = runTest {
        val original = downloadEntity(state = "QUEUED", progress = 0f)
        dao.upsert(original)

        val updated = original.copy(state = "RUNNING", progress = 0.5f)
        dao.upsert(updated)

        dao.observeQueued().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].state).isEqualTo("RUNNING")
            assertThat(items[0].progress).isEqualTo(0.5f)
        }
    }

    // --- observeQueued ---

    @Test
    fun observeQueued_returnsOnlyQueuedEntries() = runTest {
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/1", state = "QUEUED"))
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/2", state = "RUNNING"))
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/3", state = "COMPLETED"))
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/4", state = "QUEUED"))

        dao.observeQueued().test {
            val items = awaitItem()
            assertThat(items).hasSize(2)
            assertThat(items.all { it.state == "QUEUED" }).isTrue()
        }
    }

    @Test
    fun observeQueued_ordersBySourceIdAscending() = runTest {
        dao.upsert(downloadEntity(sourceId = 3L, chapterUrl = "https://example.com/ch/3", state = "QUEUED"))
        dao.upsert(downloadEntity(sourceId = 1L, chapterUrl = "https://example.com/ch/1", state = "QUEUED"))
        dao.upsert(downloadEntity(sourceId = 2L, chapterUrl = "https://example.com/ch/2", state = "QUEUED"))

        dao.observeQueued().test {
            val items = awaitItem()
            assertThat(items).hasSize(3)
            assertThat(items[0].sourceId).isEqualTo(1L)
            assertThat(items[1].sourceId).isEqualTo(2L)
            assertThat(items[2].sourceId).isEqualTo(3L)
        }
    }

    @Test
    fun observeQueued_emitsUpdateWhenStateChanges() = runTest {
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/1", state = "QUEUED"))

        dao.observeQueued().test {
            assertThat(awaitItem()).hasSize(1)

            dao.updateState(sourceId = 1L, chapterUrl = "https://example.com/ch/1", state = "RUNNING", progress = 0.3f)

            assertThat(awaitItem()).isEmpty()
        }
    }

    // --- updateState ---

    @Test
    fun updateState_changesStateAndProgress() = runTest {
        dao.upsert(downloadEntity(state = "QUEUED", progress = 0f))
        dao.updateState(sourceId = 1L, chapterUrl = "https://example.com/series/1/ch/1", state = "RUNNING", progress = 0.5f)

        dao.observeQueued().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun updateState_canSetToCompleted() = runTest {
        dao.upsert(downloadEntity(state = "QUEUED"))
        dao.updateState(sourceId = 1L, chapterUrl = "https://example.com/series/1/ch/1", state = "COMPLETED", progress = 1f)

        dao.observeQueued().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun updateState_canSetToFailed() = runTest {
        dao.upsert(downloadEntity(state = "RUNNING"))
        dao.updateState(sourceId = 1L, chapterUrl = "https://example.com/series/1/ch/1", state = "FAILED", progress = 0.3f)

        dao.observeQueued().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    // --- delete ---

    @Test
    fun delete_removesEntry() = runTest {
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/1", state = "QUEUED"))
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/2", state = "QUEUED"))

        dao.delete(sourceId = 1L, chapterUrl = "https://example.com/ch/1")

        dao.observeQueued().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].chapterUrl).isEqualTo("https://example.com/ch/2")
        }
    }

    @Test
    fun delete_nonExistentEntry_doesNotAffectOthers() = runTest {
        dao.upsert(downloadEntity(chapterUrl = "https://example.com/ch/1", state = "QUEUED"))

        dao.delete(sourceId = 99L, chapterUrl = "https://example.com/nonexistent")

        dao.observeQueued().test {
            assertThat(awaitItem()).hasSize(1)
        }
    }
}
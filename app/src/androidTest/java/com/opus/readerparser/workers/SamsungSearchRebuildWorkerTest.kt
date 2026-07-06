package com.opus.readerparser.workers

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.data.local.search.SearchIndexSyncer
import com.opus.readerparser.data.local.search.SearchProviderDelegate
import com.opus.readerparser.data.local.search.SamsungSearchClient
import com.opus.readerparser.data.local.search.SamsungSearchSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SamsungSearchRebuildWorker] that exercise the
 * real worker instead of reimplementing its control flow.
 *
 * Uses [TestListenableWorkerBuilder] to instantiate the worker with a
 * custom [WorkerFactory] that wires a controllable [SearchIndexSyncer].
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SamsungSearchRebuildWorkerTest {

    /** DAO that throws on demand so the worker's catch path is exercised. */
    private class ThrowSeriesDao : SeriesDao {
        var throwOnGetIndexable = false

        override suspend fun getIndexableSeries(): List<SeriesEntity> {
            if (throwOnGetIndexable) throw RuntimeException("rebuild failed")
            return emptyList()
        }

        // --- unused DAO methods ---
        override fun observeIndexableSeries(): Flow<List<SeriesEntity>> = emptyFlow()
        override fun observeLibrary(): Flow<List<SeriesEntity>> = emptyFlow()
        override suspend fun getByUrl(sourceId: Long, url: String): SeriesEntity? = null
        override suspend fun getBySourceId(sourceId: Long): List<SeriesEntity> = emptyList()
        override suspend fun upsert(series: SeriesEntity) {}
        override suspend fun upsertAll(series: List<SeriesEntity>) {}
        override suspend fun addToLibrary(sourceId: Long, url: String, addedAt: Long) {}
        override suspend fun removeFromLibrary(sourceId: Long, url: String) {}
        override suspend fun updateDetails(
            sourceId: Long, url: String, title: String, author: String?,
            artist: String?, description: String?, coverUrl: String?,
            genresJson: String, status: String, type: String,
        ): Int = 0
        override suspend fun insert(series: SeriesEntity) {}
        override suspend fun delete(sourceId: Long, url: String) {}
    }

    private class FakeSearchClientForWorker : SamsungSearchClient(
        delegate = object : SearchProviderDelegate {
            override fun getType(uri: Uri): String? =
                "vnd.android.cursor.dir/vnd.samsung.search"

            override fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle? =
                Bundle().apply { putInt("status", 0) }
            override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int = values.size
            override fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int = 0
        },
        schema = SamsungSearchSchema.fake(ByteArray(0)),
    )

    /** Client that reports available + registered but fails on actual writes. */
    private class FailingSearchClient : SamsungSearchClient(
        delegate = object : SearchProviderDelegate {
            override fun getType(uri: Uri): String? =
                "vnd.android.cursor.dir/vnd.samsung.search"

            override fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle? =
                Bundle().apply { putInt("status", 0) }
            override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int =
                throw RuntimeException("write failed")
            override fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int =
                throw RuntimeException("write failed")
        },
        schema = SamsungSearchSchema.fake(ByteArray(0)),
    )

    private lateinit var fakeDao: ThrowSeriesDao
    private lateinit var syncer: SearchIndexSyncer

    @Before
    fun setUp() {
        fakeDao = ThrowSeriesDao()
        syncer = SearchIndexSyncer(fakeDao, FakeSearchClientForWorker())
    }

    private fun createWorker(runAttemptCount: Int = 0): SamsungSearchRebuildWorker {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context, workerClassName: String, workerParameters: WorkerParameters,
            ): ListenableWorker = SamsungSearchRebuildWorker(context, workerParameters, syncer)
        }
        return TestListenableWorkerBuilder.from(
            context,
            SamsungSearchRebuildWorker::class.java,
        ).setWorkerFactory(factory).setRunAttemptCount(runAttemptCount).build()
    }

    @Test
    fun success_returnsResultSuccess() = runTest {
        val worker = createWorker()
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun exception_firstAttempt_returnsRetry() = runTest {
        fakeDao.throwOnGetIndexable = true
        val worker = createWorker(runAttemptCount = 0)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun exception_thirdAttempt_returnsFailure() = runTest {
        fakeDao.throwOnGetIndexable = true
        val worker = createWorker(runAttemptCount = 2)
        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun rebuildIndex_returnsFalse_returnsFailure() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val failingSyncer = SearchIndexSyncer(
            fakeDao,
            FailingSearchClient(),
        )
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context, workerClassName: String, workerParameters: WorkerParameters,
            ): ListenableWorker = SamsungSearchRebuildWorker(context, workerParameters, failingSyncer)
        }
        val worker = TestListenableWorkerBuilder.from(
            context,
            SamsungSearchRebuildWorker::class.java,
        ).setWorkerFactory(factory).build()

        val result = worker.startWork().get()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}

package com.opus.readerparser.data.local.search

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SearchIndexSyncer].
 *
 * Verifies the rebuild path: that it queries the DAO, clears the index,
 * and bulk-inserts the current series set. Also verifies that
 * [SamsungSearchClient] unavailability is handled gracefully.
 *
 * Requires the Android framework (ContentValues, Uri, Bundle) and
 * therefore lives in androidTest rather than local JVM tests.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SearchIndexSyncerTest {

    private class FakeSeriesDao : SeriesDao {
        private val backingStore = mutableListOf<SeriesEntity>()
        private val indexableFlow = MutableSharedFlow<List<SeriesEntity>>(replay = 1)

        /**
         * Updates the shared backing store so that both
         * [observeIndexableSeries] and [getIndexableSeries] reflect the
         * same data — the one-shot path used by [SearchIndexSyncer.rebuildIndex]
         * and the flow path used by [SearchIndexSyncer.startObserving].
         */
        fun emitIndexable(series: List<SeriesEntity>) {
            backingStore.clear()
            backingStore.addAll(series)
            indexableFlow.tryEmit(series)
        }

        override fun observeIndexableSeries(): Flow<List<SeriesEntity>> = indexableFlow

        override suspend fun getIndexableSeries(): List<SeriesEntity> = backingStore.toList()

        override suspend fun getLibraryIndexableSeries(sourceId: Long, url: String): SeriesEntity? =
            backingStore.find { it.sourceId == sourceId && it.url == url && it.inLibrary }

        // --- unused DAO methods ---
        override fun observeLibrary(): Flow<List<SeriesEntity>> = emptyFlow()
        override suspend fun getByUrl(sourceId: Long, url: String): SeriesEntity? = null
        override suspend fun getBySourceId(sourceId: Long): List<SeriesEntity> = emptyList()
        override suspend fun upsert(series: SeriesEntity) { backingStore.add(series) }
        override suspend fun upsertAll(series: List<SeriesEntity>) { backingStore.addAll(series) }
        override suspend fun addToLibrary(sourceId: Long, url: String, addedAt: Long) {}
        override suspend fun removeFromLibrary(sourceId: Long, url: String) {}
        override suspend fun updateDetails(
            sourceId: Long, url: String, title: String, author: String?,
            artist: String?, description: String?, coverUrl: String?,
            genresJson: String, status: String, type: String,
        ): Int = 0
        override suspend fun insert(series: SeriesEntity) { backingStore.add(series) }
        override suspend fun delete(sourceId: Long, url: String) {}
    }

    private class FakeSearchClient(
        private var available: Boolean = true,
    ) : SamsungSearchClient(
        delegate = FakeSearchProviderDelegate(),
        schema = SamsungSearchSchema.fake(ByteArray(0)),
    ) {
        var deleteAllCalls = 0
            private set
        var bulkInsertCalls = mutableListOf<List<ContentValues>>()
            private set
        var lastInsertedCount = 0
            private set
        var registerSchemaResult = true
        var deleteAllResult = true
        var bulkInsertResult = true

        override fun isAvailable(): Boolean = available

        override fun registerSchema(): Boolean = registerSchemaResult

        override fun deleteAll(): Boolean {
            deleteAllCalls++
            return deleteAllResult
        }

        override fun bulkInsert(documents: List<ContentValues>): Boolean {
            bulkInsertCalls.add(documents)
            lastInsertedCount += documents.size
            return bulkInsertResult
        }
    }

    private class FakeSearchProviderDelegate : SearchProviderDelegate {
        override fun getType(uri: Uri): String? = null
        override fun call(authority: Uri, method: String, arg: String?, extras: Bundle?): Bundle? = null
        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? = null
        override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int = values.size
        override fun delete(uri: Uri, where: String?, selectionArgs: Array<String?>?): Int = 0
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeDao: FakeSeriesDao
    private lateinit var fakeClient: FakeSearchClient
    private lateinit var syncer: SearchIndexSyncer

    private fun fakeEntity(
        sourceId: Long = 1L,
        url: String = "https://test.invalid/series/1",
        title: String = "Test Series",
    ) = SeriesEntity(
        sourceId = sourceId,
        url = url,
        title = title,
        author = null,
        artist = null,
        description = null,
        coverUrl = null,
        genresJson = "[]",
        status = "UNKNOWN",
        type = "NOVEL",
    )

    @Before
    fun setUp() {
        fakeDao = FakeSeriesDao()
        fakeClient = FakeSearchClient()
        syncer = SearchIndexSyncer(fakeDao, fakeClient)
    }

    // --- rebuildIndex ---

    @Test
    fun rebuildIndex_clears_and_inserts_all_indexable_series() = testScope.runTest {
        val series = listOf(
            fakeEntity(sourceId = 1, url = "https://test.invalid/series/1", title = "Alpha"),
            fakeEntity(sourceId = 2, url = "https://test.invalid/series/2", title = "Beta"),
        )
        fakeDao.emitIndexable(series)

        val result = syncer.rebuildIndex()

        assertTrue(result)
        assertEquals("deleteAll should be called once", 1, fakeClient.deleteAllCalls)
        assertEquals("bulkInsert should be called once with 2 documents", 1, fakeClient.bulkInsertCalls.size)
        assertEquals("2 documents should be inserted", 2, fakeClient.lastInsertedCount)
    }

    @Test
    fun rebuildIndex_with_empty_list_clears_index_but_inserts_nothing() = testScope.runTest {
        fakeDao.emitIndexable(emptyList())

        val result = syncer.rebuildIndex()

        assertTrue(result)
        assertEquals("deleteAll should be called once", 1, fakeClient.deleteAllCalls)
        assertEquals("bulkInsert should be called once with 0 documents", 1, fakeClient.bulkInsertCalls.size)
        assertEquals("0 documents should be inserted", 0, fakeClient.lastInsertedCount)
    }

    @Test
    fun rebuildIndex_returns_false_when_samsung_search_is_unavailable() = testScope.runTest {
        fakeClient = FakeSearchClient(available = false)
        syncer = SearchIndexSyncer(fakeDao, fakeClient)

        fakeDao.emitIndexable(listOf(fakeEntity()))
        val result = syncer.rebuildIndex()

        assertFalse(result)
        assertEquals("deleteAll should not be called", 0, fakeClient.deleteAllCalls)
        assertEquals("bulkInsert should not be called", 0, fakeClient.bulkInsertCalls.size)
    }

    @Test
    fun rebuildIndex_returns_false_when_deleteAll_fails() = testScope.runTest {
        fakeClient.deleteAllResult = false
        fakeDao.emitIndexable(listOf(fakeEntity()))

        val result = syncer.rebuildIndex()

        assertFalse(result)
        assertEquals(1, fakeClient.deleteAllCalls)
        assertEquals("bulkInsert should not be called after deleteAll failure", 0, fakeClient.bulkInsertCalls.size)
    }

    @Test
    fun rebuildIndex_returns_false_when_bulkInsert_fails() = testScope.runTest {
        fakeClient.bulkInsertResult = false
        fakeDao.emitIndexable(listOf(fakeEntity()))

        val result = syncer.rebuildIndex()

        assertFalse(result)
        assertEquals(1, fakeClient.deleteAllCalls)
        assertEquals(1, fakeClient.bulkInsertCalls.size)
    }

    // --- rebuildIndex(ensureRegistered = true) ---

    @Test
    fun rebuildIndex_ensureRegistered_registers_before_rebuilding() = testScope.runTest {
        fakeDao.emitIndexable(listOf(fakeEntity()))

        val result = syncer.rebuildIndex(ensureRegistered = true)

        assertTrue(result)
        assertEquals(1, fakeClient.deleteAllCalls)
        assertEquals(1, fakeClient.bulkInsertCalls.size)
    }

    @Test
    fun rebuildIndex_ensureRegistered_returns_false_when_unavailable() = testScope.runTest {
        fakeClient = FakeSearchClient(available = false)
        syncer = SearchIndexSyncer(fakeDao, fakeClient)

        fakeDao.emitIndexable(listOf(fakeEntity()))
        val result = syncer.rebuildIndex(ensureRegistered = true)

        assertFalse(result)
        assertEquals("deleteAll should not be called", 0, fakeClient.deleteAllCalls)
    }

    @Test
    fun rebuildIndex_ensureRegistered_returns_false_when_registration_fails() = testScope.runTest {
        fakeClient.registerSchemaResult = false
        fakeDao.emitIndexable(listOf(fakeEntity()))

        val result = syncer.rebuildIndex(ensureRegistered = true)

        assertFalse(result)
        assertEquals("deleteAll should not be called", 0, fakeClient.deleteAllCalls)
    }

    @Test
    fun rebuildIndex_ensureRegistered_skips_registration_when_false() = testScope.runTest {
        fakeDao.emitIndexable(listOf(fakeEntity()))

        // Without ensureRegistered, registerSchema is never called
        val result = syncer.rebuildIndex(ensureRegistered = false)

        assertTrue(result)
        assertEquals(1, fakeClient.deleteAllCalls)
    }

    // --- startObserving ---

    @Test
    fun startObserving_triggers_rebuild_when_flow_emits() = testScope.runTest {
        val series = listOf(fakeEntity())

        syncer.startObserving(testScope)
        fakeDao.emitIndexable(series)
        advanceUntilIdle()

        assertEquals("deleteAll should be called once", 1, fakeClient.deleteAllCalls)
        assertEquals("1 document should be inserted", 1, fakeClient.lastInsertedCount)
    }

    @Test
    fun startObserving_cancels_previous_observation_on_re_call() = testScope.runTest {
        syncer.startObserving(testScope)
        syncer.startObserving(testScope) // second call cancels first

        fakeDao.emitIndexable(listOf(fakeEntity()))
        advanceUntilIdle()

        // Should still work — only one active observation
        assertEquals("deleteAll should be called once", 1, fakeClient.deleteAllCalls)
    }

    // --- toContentValues mapping ---

    @Test
    fun toContentValues_maps_entity_fields_correctly() {
        val entity = fakeEntity(
            sourceId = 42,
            url = "https://test.invalid/series/42",
            title = "Mapped Series",
        )

        val cv = entity.toContentValues()

        assertEquals("42:https://test.invalid/series/42", cv.getAsString("_id"))
        assertEquals("Mapped Series", cv.getAsString("title"))
        assertEquals("readerparser://series/42/https%3A%2F%2Ftest.invalid%2Fseries%2F42", cv.getAsString("source_url"))
    }
}

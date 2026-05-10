package com.opus.readerparser.data.repository

import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.data.local.database.mappers.toEntity
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.fakes.FakeSource
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for [SeriesRepositoryImpl] using hand-rolled fakes.
 *
 * Verifies that the repository correctly delegates to [SourceRegistry] for
 * network operations and to [SeriesDao] for persistence, and that source
 * exceptions propagate without being caught or wrapped.
 */
class SeriesRepositoryImplTest {

    private class FakeSeriesDao : SeriesDao {

        private val store = mutableListOf<SeriesEntity>()

        override fun observeLibrary(): Flow<List<SeriesEntity>> =
            flowOf(store.filter { it.inLibrary })

        override suspend fun getByUrl(sourceId: Long, url: String): SeriesEntity? =
            store.find { it.sourceId == sourceId && it.url == url }

        override suspend fun upsert(series: SeriesEntity) {
            val idx = store.indexOfFirst { it.sourceId == series.sourceId && it.url == series.url }
            if (idx >= 0) store[idx] = series else store.add(series)
        }

        override suspend fun upsertAll(series: List<SeriesEntity>) {
            series.forEach { upsert(it) }
        }

        override suspend fun addToLibrary(sourceId: Long, url: String, addedAt: Long) {
            val idx = store.indexOfFirst { it.sourceId == sourceId && it.url == url }
            if (idx >= 0) {
                store[idx] = store[idx].copy(inLibrary = true, addedAt = addedAt)
            }
        }

        override suspend fun removeFromLibrary(sourceId: Long, url: String) {
            val idx = store.indexOfFirst { it.sourceId == sourceId && it.url == url }
            if (idx >= 0) {
                store[idx] = store[idx].copy(inLibrary = false, addedAt = null)
            }
        }

        override suspend fun delete(sourceId: Long, url: String) {
            store.removeAll { it.sourceId == sourceId && it.url == url }
        }
    }

    // ---- Test fixtures ----
    private val fakeSource = FakeSource(name = "TestSource", lang = "en", type = ContentType.NOVEL)

    private val sourceRegistry = SourceRegistry(mapOf(fakeSource.id to fakeSource))

    private val fakeDao = FakeSeriesDao()

    private val repository = SeriesRepositoryImpl(sourceRegistry, fakeDao)

    private val testSeries = TestFixtures.testSeries(sourceId = fakeSource.id)

    // -----------------------------------------------------------------
    // observeLibrary
    // -----------------------------------------------------------------

    @Test
    fun `observeLibrary emits only inLibrary entries from DAO`() = runTest {
        val inLibrary = testSeries.toEntity().copy(inLibrary = true, addedAt = 1000L)
        val notInLibrary = testSeries.copy(url = "https://test.invalid/series/other").toEntity()
        fakeDao.upsert(inLibrary)
        fakeDao.upsert(notInLibrary)

        val result = repository.observeLibrary().first()

        assertEquals(1, result.size)
        assertEquals(testSeries.url, result[0].url)
    }

    @Test
    fun `observeLibrary returns empty list when nothing is in library`() = runTest {
        val result = repository.observeLibrary().first()
        assertTrue(result.isEmpty())
    }

    // -----------------------------------------------------------------
    // fetchPopular
    // -----------------------------------------------------------------

    @Test
    fun `fetchPopular delegates to Source and returns SeriesPage`() = runTest {
        val expected = SeriesPage(listOf(testSeries), hasNextPage = true)
        fakeSource.popularResult = expected

        val result = repository.fetchPopular(fakeSource.id, 2)

        assertEquals(expected, result)
        assertEquals(listOf(2), fakeSource.getPopularCalls)
    }

    // -----------------------------------------------------------------
    // fetchLatest
    // -----------------------------------------------------------------

    @Test
    fun `fetchLatest delegates to Source and returns SeriesPage`() = runTest {
        val expected = SeriesPage(listOf(testSeries), hasNextPage = false)
        fakeSource.latestResult = expected

        val result = repository.fetchLatest(fakeSource.id, 1)

        assertEquals(expected, result)
        assertEquals(listOf(1), fakeSource.getLatestCalls)
    }

    // -----------------------------------------------------------------
    // search
    // -----------------------------------------------------------------

    @Test
    fun `search delegates to Source with query, page, and filters`() = runTest {
        val filters = FilterList()
        val expected = SeriesPage(listOf(testSeries), hasNextPage = false)
        fakeSource.searchResult = expected

        val result = repository.search(fakeSource.id, "query", 3, filters)

        assertEquals(expected, result)
        assertEquals(listOf(Triple("query", 3, filters)), fakeSource.searchCalls)
    }

    // -----------------------------------------------------------------
    // refreshDetails
    // -----------------------------------------------------------------

    @Test
    fun `refreshDetails calls Source getSeriesDetails, upserts to DAO, returns enriched Series`() =
        runTest {
            val enriched = testSeries.copy(
                author = "Enriched Author",
                description = "Enriched description",
            )
            fakeSource.seriesDetailsResult = { enriched }

            val result = repository.refreshDetails(testSeries)

            assertEquals(enriched, result)
            assertEquals(listOf(testSeries), fakeSource.getSeriesDetailsCalls)

            // Verify the enriched series was persisted
            val stored = fakeDao.getByUrl(enriched.sourceId, enriched.url)
            assertEquals(enriched.author, stored!!.author)
            assertEquals(enriched.description, stored!!.description)
        }

    // -----------------------------------------------------------------
    // addToLibrary
    // -----------------------------------------------------------------

    @Test
    fun `addToLibrary sets inLibrary true and addedAt on existing series`() = runTest {
        // series must already exist in DB (in practice, refreshDetails inserts it first)
        fakeDao.upsert(testSeries.toEntity())

        repository.addToLibrary(testSeries)

        val stored = fakeDao.getByUrl(testSeries.sourceId, testSeries.url)
        assertNotNull("expected series to exist in DAO", stored)
        assertTrue(stored!!.inLibrary)
        assertNotNull(stored.addedAt)
        assertTrue(stored.addedAt!! > 0L)
    }

    @Test
    fun `addToLibrary preserves existing series fields`() = runTest {
        fakeDao.upsert(testSeries.toEntity())

        repository.addToLibrary(testSeries)

        val stored = fakeDao.getByUrl(testSeries.sourceId, testSeries.url)!!
        assertEquals(testSeries.title, stored.title)
        assertEquals(testSeries.sourceId, stored.sourceId)
        assertEquals(testSeries.url, stored.url)
    }

    @Test
    fun `addToLibrary is a no-op when series does not exist in DB`() = runTest {
        // Do NOT pre-insert — simulates calling addToLibrary before refreshDetails
        repository.addToLibrary(testSeries)

        // Row should still be absent; no crash, no insertion
        val stored = fakeDao.getByUrl(testSeries.sourceId, testSeries.url)
        assertNull(stored)
    }

    // -----------------------------------------------------------------
    // removeFromLibrary
    // -----------------------------------------------------------------

    @Test
    fun `removeFromLibrary delegates to DAO removeFromLibrary`() = runTest {
        // Pre-insert the series so the UPDATE-only addToLibrary has a row to operate on
        fakeDao.upsert(testSeries.toEntity())
        repository.addToLibrary(testSeries)
        // Now remove
        repository.removeFromLibrary(testSeries)

        // After removal, observeLibrary should emit empty
        val result = repository.observeLibrary().first()
        assertTrue(result.isEmpty())
    }

    // -----------------------------------------------------------------
    // Error propagation: Source exceptions propagate without being caught
    // -----------------------------------------------------------------

    @Test
    fun `fetchPopular propagates Source exception`() = runTest {
        val throwingSource = object : FakeSource(name = "ThrowingPopular", lang = "en", type = ContentType.NOVEL) {
            override suspend fun getPopular(page: Int): SeriesPage =
                throw RuntimeException("Source error")
        }
        val registry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val repo = SeriesRepositoryImpl(registry, fakeDao)

        try {
            repo.fetchPopular(throwingSource.id, 1)
            fail("Expected RuntimeException to propagate")
        } catch (e: RuntimeException) {
            assertEquals("Source error", e.message)
        }
    }

    @Test
    fun `fetchLatest propagates Source exception`() = runTest {
        val throwingSource = object : FakeSource(name = "ThrowingLatest", lang = "en", type = ContentType.NOVEL) {
            override suspend fun getLatest(page: Int): SeriesPage =
                throw IllegalStateException("Latest failed")
        }
        val registry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val repo = SeriesRepositoryImpl(registry, fakeDao)

        try {
            repo.fetchLatest(throwingSource.id, 1)
            fail("Expected IllegalStateException to propagate")
        } catch (e: IllegalStateException) {
            assertEquals("Latest failed", e.message)
        }
    }

    @Test
    fun `search propagates Source exception`() = runTest {
        val throwingSource = object : FakeSource(name = "ThrowingSearch", lang = "en", type = ContentType.NOVEL) {
            override suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage =
                throw RuntimeException("Search error")
        }
        val registry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val repo = SeriesRepositoryImpl(registry, fakeDao)

        try {
            repo.search(throwingSource.id, "q", 1, FilterList())
            fail("Expected RuntimeException to propagate")
        } catch (e: RuntimeException) {
            assertEquals("Search error", e.message)
        }
    }

    @Test
    fun `refreshDetails propagates Source exception`() = runTest {
        val throwingSource = object : FakeSource(name = "ThrowingDetails", lang = "en", type = ContentType.NOVEL) {
            override suspend fun getSeriesDetails(series: Series): Series =
                throw RuntimeException("Details error")
        }
        val registry = SourceRegistry(mapOf(throwingSource.id to throwingSource))
        val repo = SeriesRepositoryImpl(registry, fakeDao)
        val series = TestFixtures.testSeries(sourceId = throwingSource.id)

        try {
            repo.refreshDetails(series)
            fail("Expected RuntimeException to propagate")
        } catch (e: RuntimeException) {
            assertEquals("Details error", e.message)
        }
    }

    @Test
    fun `fetchPopular throws for unknown sourceId`() = runTest {
        try {
            repository.fetchPopular(99999L, 1)
            fail("Expected an error for unknown source ID")
        } catch (_: Exception) {
            // Expected — SourceRegistry throws for unregistered IDs
        }
    }
}
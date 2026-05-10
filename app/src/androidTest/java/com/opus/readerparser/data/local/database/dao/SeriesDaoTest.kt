package com.opus.readerparser.data.local.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.AppDatabase
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeriesDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SeriesDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.seriesDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Helper factories ---

    private fun seriesEntity(
        sourceId: Long = 1L,
        url: String = "https://example.com/series/1",
        title: String = "Test Series",
        inLibrary: Boolean = false,
        addedAt: Long? = null,
        type: String = "NOVEL",
        status: String = "ONGOING",
    ) = SeriesEntity(
        sourceId = sourceId,
        url = url,
        title = title,
        author = null,
        artist = null,
        description = null,
        coverUrl = null,
        genresJson = "[]",
        status = status,
        type = type,
        inLibrary = inLibrary,
        addedAt = addedAt,
    )

    // --- observeLibrary ---

    @Test
    fun observeLibrary_returnsOnlyInLibrarySeries() = runTest {
        val inLib = seriesEntity(url = "https://example.com/1", inLibrary = true, addedAt = 1000L)
        val notInLib = seriesEntity(url = "https://example.com/2", inLibrary = false)
        dao.upsertAll(listOf(inLib, notInLib))

        dao.observeLibrary().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].url).isEqualTo("https://example.com/1")
            assertThat(items[0].inLibrary).isTrue()
        }
    }

    @Test
    fun observeLibrary_ordersByAddedAtDescending() = runTest {
        val older = seriesEntity(url = "https://example.com/1", inLibrary = true, addedAt = 1000L)
        val newer = seriesEntity(url = "https://example.com/2", inLibrary = true, addedAt = 2000L)
        dao.upsertAll(listOf(older, newer))

        dao.observeLibrary().test {
            val items = awaitItem()
            assertThat(items).hasSize(2)
            assertThat(items[0].url).isEqualTo("https://example.com/2")
            assertThat(items[1].url).isEqualTo("https://example.com/1")
        }
    }

    @Test
    fun observeLibrary_emitsUpdateWhenSeriesAddedToLibrary() = runTest {
        val series = seriesEntity(url = "https://example.com/1", inLibrary = false)
        dao.upsert(series)

        dao.observeLibrary().test {
            assertThat(awaitItem()).isEmpty()

            dao.addToLibrary(sourceId = 1L, url = "https://example.com/1", addedAt = 3000L)

            val updated = awaitItem()
            assertThat(updated).hasSize(1)
            assertThat(updated[0].inLibrary).isTrue()
            assertThat(updated[0].addedAt).isEqualTo(3000L)
        }
    }

    // --- getByUrl ---

    @Test
    fun getByUrl_returnsMatchingSeries() = runTest {
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/1")
        dao.upsert(series)

        val result = dao.getByUrl(sourceId = 1L, url = "https://example.com/1")
        assertThat(result).isNotNull()
        assertThat(result!!.title).isEqualTo("Test Series")
    }

    @Test
    fun getByUrl_returnsNullWhenNotFound() = runTest {
        val result = dao.getByUrl(sourceId = 99L, url = "https://example.com/nonexistent")
        assertThat(result).isNull()
    }

    @Test
    fun getByUrl_differentSourceIdSameUrl_returnsNull() = runTest {
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/1")
        dao.upsert(series)

        val result = dao.getByUrl(sourceId = 2L, url = "https://example.com/1")
        assertThat(result).isNull()
    }

    // --- upsert ---

    @Test
    fun upsert_insertsNewSeries() = runTest {
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/1", title = "New Series")
        dao.upsert(series)

        val result = dao.getByUrl(sourceId = 1L, url = "https://example.com/1")
        assertThat(result).isNotNull()
        assertThat(result!!.title).isEqualTo("New Series")
    }

    @Test
    fun upsert_replacesExistingSeries() = runTest {
        val original = seriesEntity(sourceId = 1L, url = "https://example.com/1", title = "Original")
        dao.upsert(original)

        val updated = original.copy(title = "Updated", status = "COMPLETED")
        dao.upsert(updated)

        val result = dao.getByUrl(sourceId = 1L, url = "https://example.com/1")
        assertThat(result).isNotNull()
        assertThat(result!!.title).isEqualTo("Updated")
        assertThat(result.status).isEqualTo("COMPLETED")
    }

    // --- upsertAll ---

    @Test
    fun upsertAll_insertsMultipleSeries() = runTest {
        val series = (1..3).map {
            seriesEntity(sourceId = 1L, url = "https://example.com/$it", title = "Series $it")
        }
        dao.upsertAll(series)

        assertThat(dao.getByUrl(1L, "https://example.com/1")).isNotNull()
        assertThat(dao.getByUrl(1L, "https://example.com/2")).isNotNull()
        assertThat(dao.getByUrl(1L, "https://example.com/3")).isNotNull()
    }

    @Test
    fun upsertAll_replacesExistingEntries() = runTest {
        val original = seriesEntity(sourceId = 1L, url = "https://example.com/1", title = "Original")
        dao.upsert(original)

        val updated = original.copy(title = "Updated")
        dao.upsertAll(listOf(updated))

        assertThat(dao.getByUrl(1L, "https://example.com/1")!!.title).isEqualTo("Updated")
    }

    // --- addToLibrary ---

    @Test
    fun addToLibrary_setsInLibraryTrueAndAddedAt() = runTest {
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/1", inLibrary = false)
        dao.upsert(series)

        dao.addToLibrary(sourceId = 1L, url = "https://example.com/1", addedAt = 5000L)

        val result = dao.getByUrl(sourceId = 1L, url = "https://example.com/1")
        assertThat(result!!.inLibrary).isTrue()
        assertThat(result.addedAt).isEqualTo(5000L)
    }

    // --- addToLibrary (regression: must NOT cascade-delete chapters) ---

    @Test
    fun addToLibrary_setsInLibraryTrue_andPreservesChapters() = runTest {
        // Arrange: insert the series and a chapter FK'd to it
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/series/1")
        dao.upsert(series)

        // Cross-DAO: verifying FK ON DELETE CASCADE does NOT fire — requires ChapterDao to seed and query chapters
        val chapterDao = database.chapterDao()
        val chapter = ChapterEntity(
            sourceId = 1L,
            url = "https://example.com/series/1/ch/1",
            seriesUrl = "https://example.com/series/1",
            name = "Chapter 1",
            number = 1f,
            uploadDate = null,
            read = false,
            progress = 0f,
            downloaded = false,
        )
        chapterDao.upsertAll(listOf(chapter))

        // Act: mark the series as in-library using the UPDATE-only query
        dao.addToLibrary(sourceId = 1L, url = "https://example.com/series/1", addedAt = 5000L)

        // Assert: series is in library
        val updatedSeries = dao.getByUrl(sourceId = 1L, url = "https://example.com/series/1")
        assertThat(updatedSeries).isNotNull()
        assertThat(updatedSeries!!.inLibrary).isTrue()
        assertThat(updatedSeries.addedAt).isEqualTo(5000L)

        // Assert: chapter was NOT cascade-deleted (regression guard for issue #5)
        val chapters = chapterDao.getChaptersForSeries(
            sourceId = 1L, seriesUrl = "https://example.com/series/1"
        )
        assertThat(chapters).hasSize(1)
        assertThat(chapters[0].url).isEqualTo("https://example.com/series/1/ch/1")
    }

    @Test
    fun addToLibrary_isNoOp_whenSeriesDoesNotExist() = runTest {
        // Verifies that the UPDATE-only query throws no exception when the row is absent.
        // Asserting null is a secondary check; the primary value is confirming no INSERT occurred.
        dao.addToLibrary(sourceId = 99L, url = "https://nonexistent.invalid/", addedAt = 1000L)

        val result = dao.getByUrl(sourceId = 99L, url = "https://nonexistent.invalid/")
        assertThat(result).isNull()
    }

    // --- removeFromLibrary ---

    @Test
    fun removeFromLibrary_setsInLibraryFalse() = runTest {
        val series = seriesEntity(
            sourceId = 1L,
            url = "https://example.com/1",
            inLibrary = true,
            addedAt = 5000L,
        )
        dao.upsert(series)

        dao.removeFromLibrary(sourceId = 1L, url = "https://example.com/1")

        val result = dao.getByUrl(sourceId = 1L, url = "https://example.com/1")
        assertThat(result!!.inLibrary).isFalse()
    }

    // --- cascade delete ---

    @Test
    fun deleteSeries_cascadesDeletesChapters() = runTest {
        val series = seriesEntity(sourceId = 1L, url = "https://example.com/series/1")
        dao.upsert(series)

        val chapter = com.opus.readerparser.data.local.database.entities.ChapterEntity(
            sourceId = 1L,
            url = "https://example.com/series/1/ch/1",
            seriesUrl = "https://example.com/series/1",
            name = "Chapter 1",
            number = 1f,
        )
        database.chapterDao().upsertAll(listOf(chapter))

        assertThat(database.chapterDao().getByUrl(1L, "https://example.com/series/1/ch/1")).isNotNull()

        dao.delete(sourceId = 1L, url = "https://example.com/series/1")

        assertThat(dao.getByUrl(1L, "https://example.com/series/1")).isNull()
        assertThat(database.chapterDao().getByUrl(1L, "https://example.com/series/1/ch/1")).isNull()
    }
}
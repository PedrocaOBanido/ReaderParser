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
class ChapterDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ChapterDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.chapterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Helper factories ---

    private fun seriesEntity(
        sourceId: Long = 1L,
        url: String = "https://example.com/series/1",
    ) = SeriesEntity(
        sourceId = sourceId,
        url = url,
        title = "Test Series",
        genresJson = "[]",
        status = "ONGOING",
        type = "NOVEL",
    )

    private fun chapterEntity(
        sourceId: Long = 1L,
        url: String = "https://example.com/series/1/ch/1",
        seriesUrl: String = "https://example.com/series/1",
        name: String = "Chapter 1",
        number: Float = 1f,
        read: Boolean = false,
        progress: Float = 0f,
        downloaded: Boolean = false,
    ) = ChapterEntity(
        sourceId = sourceId,
        url = url,
        seriesUrl = seriesUrl,
        name = name,
        number = number,
        read = read,
        progress = progress,
        downloaded = downloaded,
    )

    private suspend fun insertSeriesAndChapters() {
        database.seriesDao().upsert(seriesEntity())
        dao.upsertAll(listOf(
            chapterEntity(url = "https://example.com/series/1/ch/1", name = "Chapter 1", number = 1f),
            chapterEntity(url = "https://example.com/series/1/ch/2", name = "Chapter 2", number = 2f),
            chapterEntity(url = "https://example.com/series/1/ch/3", name = "Chapter 3", number = 3f),
        ))
    }

    // --- observeChapters ---

    @Test
    fun observeChapters_returnsChaptersForSeries() = runTest {
        insertSeriesAndChapters()

        dao.observeChapters(sourceId = 1L, seriesUrl = "https://example.com/series/1").test {
            val items = awaitItem()
            assertThat(items).hasSize(3)
        }
    }

    @Test
    fun observeChapters_ordersByNumberAscending() = runTest {
        insertSeriesAndChapters()

        dao.observeChapters(sourceId = 1L, seriesUrl = "https://example.com/series/1").test {
            val items = awaitItem()
            assertThat(items[0].number).isEqualTo(1f)
            assertThat(items[1].number).isEqualTo(2f)
            assertThat(items[2].number).isEqualTo(3f)
        }
    }

    @Test
    fun observeChapters_filtersBySourceIdAndSeriesUrl() = runTest {
        database.seriesDao().upsert(seriesEntity(sourceId = 1L, url = "https://example.com/series/1"))
        database.seriesDao().upsert(seriesEntity(sourceId = 2L, url = "https://example.com/series/2"))
        dao.upsertAll(listOf(
            chapterEntity(sourceId = 1L, url = "https://example.com/series/1/ch/1", seriesUrl = "https://example.com/series/1"),
            chapterEntity(sourceId = 2L, url = "https://example.com/series/2/ch/1", seriesUrl = "https://example.com/series/2"),
        ))

        dao.observeChapters(sourceId = 1L, seriesUrl = "https://example.com/series/1").test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].sourceId).isEqualTo(1L)
        }
    }

    @Test
    fun observeChapters_emitsUpdateWhenChapterModified() = runTest {
        insertSeriesAndChapters()

        dao.observeChapters(sourceId = 1L, seriesUrl = "https://example.com/series/1").test {
            awaitItem()

            dao.markRead(sourceId = 1L, url = "https://example.com/series/1/ch/1", read = true)

            val updated = awaitItem()
            assertThat(updated.first { it.url == "https://example.com/series/1/ch/1" }.read).isTrue()
        }
    }

    // --- upsertAll ---

    @Test
    fun upsertAll_insertsNewChapters() = runTest {
        database.seriesDao().upsert(seriesEntity())
        dao.upsertAll(listOf(
            chapterEntity(url = "https://example.com/series/1/ch/1", name = "Chapter 1"),
        ))

        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/1")).isNotNull()
    }

    @Test
    fun upsertAll_replacesExistingChapters() = runTest {
        database.seriesDao().upsert(seriesEntity())
        val original = chapterEntity(url = "https://example.com/series/1/ch/1", name = "Chapter 1")
        dao.upsertAll(listOf(original))

        val updated = original.copy(name = "Chapter 1: Revised")
        dao.upsertAll(listOf(updated))

        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/1")!!.name).isEqualTo("Chapter 1: Revised")
    }

    // --- markRead ---

    @Test
    fun markRead_setsReadTrue() = runTest {
        insertSeriesAndChapters()

        dao.markRead(sourceId = 1L, url = "https://example.com/series/1/ch/1", read = true)

        val chapter = dao.getByUrl(1L, "https://example.com/series/1/ch/1")
        assertThat(chapter!!.read).isTrue()
    }

    @Test
    fun markRead_setsReadFalse() = runTest {
        insertSeriesAndChapters()
        dao.markRead(sourceId = 1L, url = "https://example.com/series/1/ch/1", read = true)
        dao.markRead(sourceId = 1L, url = "https://example.com/series/1/ch/1", read = false)

        val chapter = dao.getByUrl(1L, "https://example.com/series/1/ch/1")
        assertThat(chapter!!.read).isFalse()
    }

    // --- setProgress ---

    @Test
    fun setProgress_updatesProgressValue() = runTest {
        insertSeriesAndChapters()

        dao.setProgress(sourceId = 1L, url = "https://example.com/series/1/ch/1", progress = 0.5f)

        val chapter = dao.getByUrl(1L, "https://example.com/series/1/ch/1")
        assertThat(chapter!!.progress).isEqualTo(0.5f)
    }

    @Test
    fun setProgress_doesNotAffectOtherFields() = runTest {
        insertSeriesAndChapters()
        dao.markRead(sourceId = 1L, url = "https://example.com/series/1/ch/1", read = true)
        dao.setProgress(sourceId = 1L, url = "https://example.com/series/1/ch/1", progress = 0.75f)

        val chapter = dao.getByUrl(1L, "https://example.com/series/1/ch/1")
        assertThat(chapter!!.read).isTrue()
        assertThat(chapter.progress).isEqualTo(0.75f)
    }

    // --- deleteBySeries ---

    @Test
    fun deleteBySeries_removesAllChaptersForSeries() = runTest {
        insertSeriesAndChapters()

        dao.deleteBySeries(sourceId = 1L, seriesUrl = "https://example.com/series/1")

        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/1")).isNull()
        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/2")).isNull()
        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/3")).isNull()
    }

    @Test
    fun deleteBySeries_doesNotAffectOtherSeries() = runTest {
        database.seriesDao().upsert(seriesEntity(sourceId = 1L, url = "https://example.com/series/1"))
        database.seriesDao().upsert(seriesEntity(sourceId = 2L, url = "https://example.com/series/2"))
        dao.upsertAll(listOf(
            chapterEntity(sourceId = 1L, url = "https://example.com/series/1/ch/1", seriesUrl = "https://example.com/series/1"),
            chapterEntity(sourceId = 2L, url = "https://example.com/series/2/ch/1", seriesUrl = "https://example.com/series/2"),
        ))

        dao.deleteBySeries(sourceId = 1L, seriesUrl = "https://example.com/series/1")

        assertThat(dao.getByUrl(1L, "https://example.com/series/1/ch/1")).isNull()
        assertThat(dao.getByUrl(2L, "https://example.com/series/2/ch/1")).isNotNull()
    }
}
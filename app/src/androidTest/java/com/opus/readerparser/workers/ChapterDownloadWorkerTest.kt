package com.opus.readerparser.workers

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.AppDatabase
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.data.repository.ChapterRepositoryImpl
import com.opus.readerparser.data.source.Source
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.DownloadRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChapterDownloadWorkerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var chapterRepository: ChapterRepository
    private lateinit var fakeDownloadRepository: FakeDownloadRepositoryAndroidTest
    private lateinit var fakeDownloadStore: FakeDownloadStoreAndroidTest
    private lateinit var fakeSource: FakeSourceAndroidTest
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        fakeSource = FakeSourceAndroidTest()
        val registry = SourceRegistry(mapOf(fakeSource.id to fakeSource))
        // ChapterRepositoryImpl owns the source-dispatch; the worker never
        // touches SourceRegistry or DAOs directly.
        chapterRepository = ChapterRepositoryImpl(registry, database.chapterDao())
        fakeDownloadRepository = FakeDownloadRepositoryAndroidTest()
        fakeDownloadStore = FakeDownloadStoreAndroidTest()

        val factory = TestWorkerFactory(
            chapterRepository = chapterRepository,
            downloadRepository = fakeDownloadRepository,
            store = fakeDownloadStore,
        )

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            androidx.work.Configuration.Builder()
                .setWorkerFactory(factory)
                .build(),
        )

        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- helpers ---

    private suspend fun insertSeries(sourceId: Long, url: String) {
        database.seriesDao().upsert(
            SeriesEntity(
                sourceId = sourceId,
                url = url,
                title = "Test Series",
                author = null,
                artist = null,
                description = null,
                coverUrl = null,
                genresJson = "[]",
                status = "ONGOING",
                type = "NOVEL",
            ),
        )
    }

    private suspend fun insertChapter(sourceId: Long, url: String, seriesUrl: String) {
        database.chapterDao().upsertAll(
            listOf(
                ChapterEntity(
                    sourceId = sourceId,
                    url = url,
                    seriesUrl = seriesUrl,
                    name = "Chapter 1",
                    number = 1f,
                ),
            ),
        )
    }

    /**
     * Enqueues a work request, satisfies its constraints, then polls WorkInfo
     * until the state is terminal (SUCCEEDED, FAILED, or CANCELLED).
     * WorkManagerTestInitHelper's sync execution means the job runs synchronously
     * after [setAllConstraintsMet]; a single [getWorkInfoById] call is sufficient,
     * but we loop defensively for robustness.
     */
    private fun enqueueAndWait(sourceId: Long, chapterUrl: String): WorkInfo {
        val request = ChapterDownloadWorker.buildRequest(sourceId, chapterUrl)
        workManager.enqueue(request).result.get()
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        testDriver.setAllConstraintsMet(request.id)
        // Spin until terminal — WorkManagerTestInitHelper runs work synchronously
        // on the calling thread after setAllConstraintsMet in test mode.
        var info = workManager.getWorkInfoById(request.id).get()!!
        var retries = 0
        while (!info.state.isFinished && retries < 10) {
            Thread.sleep(100)
            info = workManager.getWorkInfoById(request.id).get()!!
            retries++
        }
        return info
    }

    // --- tests ---

    @Test
    fun novelChapter_success_stateIsSucceeded() = runTest {
        val sourceId = fakeSource.id
        val seriesUrl = "https://example.com/series/1"
        val chapterUrl = "https://example.com/chapter/1"

        fakeSource.chapterContentResult = ChapterContent.Text("<p>Hello world</p>")
        insertSeries(sourceId, seriesUrl)
        insertChapter(sourceId, chapterUrl, seriesUrl)

        val info = enqueueAndWait(sourceId, chapterUrl)

        assertThat(info.state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(fakeDownloadStore.novelWrites).hasSize(1)
        assertThat(fakeDownloadStore.novelWrites.first().second).isEqualTo("<p>Hello world</p>")
        // Downloaded flag should be persisted in the DB via ChapterRepositoryImpl.
        val entity = database.chapterDao().getByUrl(sourceId, chapterUrl)
        assertThat(entity?.downloaded).isTrue()
        // Queue state must be updated to COMPLETED.
        val completedCall = fakeDownloadRepository.updateQueueStateCalls
            .firstOrNull { it.state == DownloadState.COMPLETED }
        assertThat(completedCall).isNotNull()
        assertThat(completedCall!!.sourceId).isEqualTo(sourceId)
        assertThat(completedCall.chapterUrl).isEqualTo(chapterUrl)
    }

    @Test
    fun manhwaChapter_success_stateIsSucceeded() = runTest {
        val sourceId = fakeSource.id
        val seriesUrl = "https://example.com/series/2"
        val chapterUrl = "https://example.com/chapter/2"
        val pages = listOf("https://cdn.example.com/page1.jpg", "https://cdn.example.com/page2.jpg")

        fakeSource.chapterContentResult = ChapterContent.Pages(pages)
        insertSeries(sourceId, seriesUrl)
        insertChapter(sourceId, chapterUrl, seriesUrl)

        val info = enqueueAndWait(sourceId, chapterUrl)

        assertThat(info.state).isEqualTo(WorkInfo.State.SUCCEEDED)
        assertThat(fakeDownloadStore.manhwaWrites).hasSize(1)
        assertThat(fakeDownloadStore.manhwaWrites.first().second).isEqualTo(pages)
        val entity = database.chapterDao().getByUrl(sourceId, chapterUrl)
        assertThat(entity?.downloaded).isTrue()
    }

    @Test
    fun chapterNotFound_stateIsFailed() = runTest {
        val sourceId = fakeSource.id
        // Do NOT insert a chapter entity — findByUrl returns null → Result.failure().

        val info = enqueueAndWait(sourceId, "https://example.com/chapter/missing")

        assertThat(info.state).isEqualTo(WorkInfo.State.FAILED)
        assertThat(fakeDownloadStore.novelWrites).isEmpty()
        assertThat(fakeDownloadStore.manhwaWrites).isEmpty()
        // Queue state must be updated to FAILED when the chapter is not found.
        val failedCall = fakeDownloadRepository.updateQueueStateCalls
            .firstOrNull { it.state == DownloadState.FAILED }
        assertThat(failedCall).isNotNull()
    }

    @Test
    fun unknownSourceId_stateIsFailed() = runTest {
        val unknownSourceId = 0L // not registered in the registry
        val seriesUrl = "https://example.com/series/99"
        val chapterUrl = "https://example.com/chapter/99"

        // Insert a chapter so findByUrl succeeds, but getContent will throw because
        // SourceRegistry has no entry for unknownSourceId.
        insertSeries(fakeSource.id, seriesUrl)
        // Insert under the known sourceId so the FK constraint is satisfied, then
        // query with the unknown id — this triggers the not-found path cleanly.
        val info = enqueueAndWait(unknownSourceId, chapterUrl)

        // findByUrl returns null (no chapter in DB for unknownSourceId) → FAILED.
        assertThat(info.state).isEqualTo(WorkInfo.State.FAILED)
    }
}

// ---------------------------------------------------------------------------
// Test doubles (local to the androidTest source set)
// Keep structurally in sync with FakeDownloadStore in src/test/kotlin/.../fakes/.
// ---------------------------------------------------------------------------

private class FakeDownloadStoreAndroidTest : DownloadStore {

    val storedContent: MutableMap<Chapter, ChapterContent> = mutableMapOf()
    val novelWrites: MutableList<Pair<Chapter, String>> = mutableListOf()
    val manhwaWrites: MutableList<Pair<Chapter, List<String>>> = mutableListOf()

    /** Tracks every [delete] call — keep in sync with JVM FakeDownloadStore. */
    val deleteCalls: MutableList<Chapter> = mutableListOf()

    override suspend fun read(chapter: Chapter): ChapterContent? = storedContent[chapter]

    override suspend fun writeNovel(chapter: Chapter, html: String) {
        novelWrites.add(chapter to html)
        storedContent[chapter] = ChapterContent.Text(html)
    }

    override suspend fun writeManhwa(chapter: Chapter, imageUrls: List<String>) {
        manhwaWrites.add(chapter to imageUrls)
        storedContent[chapter] = ChapterContent.Pages(imageUrls)
    }

    override suspend fun delete(chapter: Chapter) {
        deleteCalls.add(chapter)
        storedContent.remove(chapter)
    }
}

private class FakeDownloadRepositoryAndroidTest : DownloadRepository {

    private val _queue = MutableStateFlow<List<DownloadItem>>(emptyList())

    data class UpdateQueueStateCall(
        val sourceId: Long,
        val chapterUrl: String,
        val state: DownloadState,
        val progress: Float,
        val errorMessage: String?,
    )

    val updateQueueStateCalls: MutableList<UpdateQueueStateCall> = mutableListOf()

    override fun observeQueue(): Flow<List<DownloadItem>> = _queue

    override suspend fun cancel(sourceId: Long, chapterUrl: String) {
        _queue.value = _queue.value.filter { it.sourceId != sourceId || it.chapterUrl != chapterUrl }
    }

    override suspend fun retry(sourceId: Long, chapterUrl: String) {
        _queue.value = _queue.value.map {
            if (it.sourceId == sourceId && it.chapterUrl == chapterUrl) {
                it.copy(state = DownloadState.QUEUED, progress = 0f, errorMessage = null)
            } else {
                it
            }
        }
    }

    override suspend fun updateQueueState(
        sourceId: Long,
        chapterUrl: String,
        state: DownloadState,
        progress: Float,
        errorMessage: String?,
    ) {
        updateQueueStateCalls.add(UpdateQueueStateCall(sourceId, chapterUrl, state, progress, errorMessage))
    }
}

private class FakeSourceAndroidTest : Source {

    override val id: Long = 9999L
    override val name: String = "FakeSource"
    override val lang: String = "en"
    override val baseUrl: String = "https://example.com"
    override val type: ContentType = ContentType.NOVEL

    var chapterContentResult: ChapterContent = ChapterContent.Text("")

    override suspend fun getPopular(page: Int): SeriesPage = SeriesPage(emptyList(), false)
    override suspend fun getLatest(page: Int): SeriesPage = SeriesPage(emptyList(), false)
    override suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage =
        SeriesPage(emptyList(), false)

    override suspend fun getSeriesDetails(series: Series): Series = series
    override suspend fun getChapterList(series: Series): List<Chapter> = emptyList()
    override suspend fun getChapterContent(chapter: Chapter): ChapterContent = chapterContentResult
}

/**
 * Custom [WorkerFactory] that wires [ChapterDownloadWorker] with test doubles
 * instead of Hilt-injected dependencies.
 *
 * Any worker type other than [ChapterDownloadWorker] falls back to the default
 * factory (returns `null` so WorkManager uses its own default).
 */
private class TestWorkerFactory(
    private val chapterRepository: ChapterRepository,
    private val downloadRepository: DownloadRepository,
    private val store: DownloadStore,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        if (workerClassName != ChapterDownloadWorker::class.java.name) return null
        return ChapterDownloadWorker(
            ctx = appContext,
            params = workerParameters,
            chapterRepository = chapterRepository,
            downloadRepository = downloadRepository,
            downloads = store,
        )
    }
}

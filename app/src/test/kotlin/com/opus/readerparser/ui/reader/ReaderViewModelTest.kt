package com.opus.readerparser.ui.reader

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.fakes.FakeChapterRepository
import com.opus.readerparser.fakes.FakeDownloadEnqueuer
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import com.opus.readerparser.ui.reader.ReaderAction
import com.opus.readerparser.ui.reader.ReaderEffect
import com.opus.readerparser.ui.reader.ReaderUiState
import com.opus.readerparser.ui.reader.ReaderViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads text content into state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val html = "<p>Chapter body</p>"
        val (vm, repo) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text(html),
        )

        advanceUntilIdle()

        val state: ReaderUiState = vm.state.value
        assertThat(state.contentType).isEqualTo(ContentType.NOVEL)
        assertThat(state.html).isEqualTo(html)
        assertThat(state.isLoading).isFalse()
        assertThat(state.chapter).isEqualTo(chapter)
        assertThat(state.seriesChapters).containsExactly(chapter)
        assertThat(repo.markReadCalls).containsExactly(chapter to true)
    }

    @Test
    fun `init loads page content into state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.MANHWA)
        val chapter = testChapter(series)
        val pages = listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.MANHWA,
            contentResult = ChapterContent.Pages(pages),
        )

        advanceUntilIdle()

        val state: ReaderUiState = vm.state.value
        assertThat(state.contentType).isEqualTo(ContentType.MANHWA)
        assertThat(state.pages).isEqualTo(pages)
        assertThat(state.isLoading).isFalse()
        assertThat(state.chapter).isEqualTo(chapter)
        assertThat(state.seriesChapters).containsExactly(chapter)
    }

    @Test
    fun `init stores current series chapter list in state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val next = testChapter(series, url = "https://test.invalid/chapter/2", name = "Chapter 2", number = 2f)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
            chapters = listOf(
                ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
                ChapterWithState(next, read = false, downloaded = false, progress = 0f),
            ),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.seriesChapters).containsExactly(chapter, next).inOrder()
    }

    @Test
    fun `init shows error when chapter is not available`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val repo = object : ChapterRepository {
            override fun observeChapters(series: Series): Flow<List<ChapterWithState>> = flowOf(emptyList())

            override suspend fun refreshChapters(series: Series) {}

            override suspend fun findByUrl(sourceId: Long, url: String): Chapter? = null

            override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent {
                error("should not be called")
            }

            override suspend fun markRead(chapter: Chapter, read: Boolean) {}

            override suspend fun setProgress(chapter: Chapter, progress: Float) {}

            override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {}
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo("Chapter not available")
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.chapter).isNull()
        assertThat(vm.state.value.chapterUrl).isEqualTo(chapter.url)
    }

    @Test
    fun `Retry re-resolves chapter when current chapter is missing`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val resolved = testChapter(series, url = chapter.url, name = "Chapter 1 (resolved)")
        val repo = FakeChapterRepository().apply {
            setChapters(series.url, emptyList())
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo("Chapter not available")
        assertThat(vm.state.value.chapter).isNull()

        repo.setChapters(
            series.url,
            listOf(ChapterWithState(resolved, read = false, downloaded = false, progress = 0f)),
        )
        vm.onAction(ReaderAction.Retry)
        advanceUntilIdle()

        assertThat(vm.state.value.chapter).isEqualTo(resolved)
        assertThat(vm.state.value.html).isNotEmpty()
        assertThat(repo.getContentCalls).contains(resolved)
    }

    @Test
    fun `init marks text chapter as read immediately`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (_, repo) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
        )

        advanceUntilIdle()

        assertThat(repo.markReadCalls).containsExactly(chapter to true)
    }

    @Test
    fun `SetProgress clamps progress and skips duplicate writes`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (vm, repo) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
        )

        advanceUntilIdle()
        vm.onAction(ReaderAction.SetProgress(1.5f))
        advanceUntilIdle()
        vm.onAction(ReaderAction.SetProgress(1.8f))
        advanceUntilIdle()

        assertThat(vm.state.value.progress).isEqualTo(1f)
        assertThat(repo.setProgressCalls).containsExactly(chapter to 1f)
    }

    @Test
    fun `SetProgress retries after repository failure`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val attempts = mutableListOf<Float>()
        val repo = object : ChapterRepository {
            override fun observeChapters(series: Series) = flowOf(
                listOf(ChapterWithState(chapter, read = false, downloaded = false, progress = 0f)),
            )

            override suspend fun refreshChapters(series: Series) {}

            override suspend fun findByUrl(sourceId: Long, url: String) = chapter

            override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent =
                ChapterContent.Text("<p>Chapter body</p>")

            override suspend fun markRead(chapter: Chapter, read: Boolean) {}

            override suspend fun setProgress(chapter: Chapter, progress: Float) {
                attempts += progress
                if (attempts.size == 1) error("fail once")
            }

            override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {}
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()
        vm.onAction(ReaderAction.SetProgress(0.5f))
        advanceUntilIdle()
        vm.onAction(ReaderAction.SetProgress(0.5f))
        advanceUntilIdle()

        assertThat(attempts).containsExactly(0.5f, 0.5f).inOrder()
        assertThat(vm.state.value.progress).isEqualTo(0.5f)
    }

    @Test
    fun `init shows repository errors from chapter loading`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val repo = object : ChapterRepository {
            override fun observeChapters(series: Series): Flow<List<ChapterWithState>> = flow {
                error("boom")
            }

            override suspend fun refreshChapters(series: Series) {}

            override suspend fun findByUrl(sourceId: Long, url: String): Chapter? = null

            override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent {
                error("should not be called")
            }

            override suspend fun markRead(chapter: Chapter, read: Boolean) {}

            override suspend fun setProgress(chapter: Chapter, progress: Float) {}

            override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {}
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo("boom")
    }

    @Test
    fun `SetPage updates currentPage in state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.MANHWA)
        val chapter = testChapter(series)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.MANHWA,
            contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")),
        )

        advanceUntilIdle()
        vm.onAction(ReaderAction.SetPage(1))
        advanceUntilIdle()

        assertThat(vm.state.value.currentPage).isEqualTo(1)
    }

    @Test
    fun `SetPage to last page marks chapter as read`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.MANHWA)
        val chapter = testChapter(series)
        val pages = listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")
        val (vm, repo) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.MANHWA,
            contentResult = ChapterContent.Pages(pages),
        )

        advanceUntilIdle()
        vm.onAction(ReaderAction.SetPage(pages.lastIndex))
        advanceUntilIdle()

        assertThat(vm.state.value.currentPage).isEqualTo(pages.lastIndex)
        assertThat(repo.markReadCalls).containsExactly(chapter to true)
    }

    @Test
    fun `NextChapter emits NavigateToChapter when next exists for novel route`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val next = testChapter(series, url = "https://test.invalid/chapter/2", name = "Chapter 2", number = 2f)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
            chapters = listOf(
                ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
                ChapterWithState(next, read = false, downloaded = false, progress = 0f),
            ),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.NextChapter)
            assertThat(awaitItem()).isEqualTo(ReaderEffect.NavigateToChapter(next))
        }
    }

    @Test
    fun `NextChapter emits NavigateToChapter when next exists for manhwa route`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.MANHWA)
        val chapter = testChapter(series)
        val next = testChapter(series, url = "https://test.invalid/chapter/2", name = "Chapter 2", number = 2f)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.MANHWA,
            contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")),
            chapters = listOf(
                ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
                ChapterWithState(next, read = false, downloaded = false, progress = 0f),
            ),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.NextChapter)
            assertThat(awaitItem()).isEqualTo(ReaderEffect.NavigateToChapter(next))
        }
    }

    @Test
    fun `PreviousChapter does not navigate when at first chapter`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.PreviousChapter)
            expectNoEvents()
        }
    }

    @Test
    fun `OpenChapterList emits ShowChapterList effect`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.OpenChapterList)
            assertThat(awaitItem()).isEqualTo(ReaderEffect.ShowChapterList)
        }
    }

    @Test
    fun `wrong content type sets error in state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (vm, _) = createViewModel(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg")),
        )

        advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo("Unexpected content type")
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.chapter).isEqualTo(chapter)
    }

    @Test
    fun `mismatch recovery fetches from network when cache returns wrong type`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val forceNetworkCalls = mutableListOf<Boolean>()
        val repo = object : ChapterRepository {
            override fun observeChapters(series: Series) = flowOf(
                listOf(ChapterWithState(chapter, read = false, downloaded = false, progress = 0f)),
            )

            override suspend fun refreshChapters(series: Series) {}

            override suspend fun findByUrl(sourceId: Long, url: String) = chapter

            override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent {
                forceNetworkCalls += forceNetwork
                return if (forceNetwork) {
                    ChapterContent.Text("<p>Recovered</p>")
                } else {
                    ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg"))
                }
            }

            override suspend fun markRead(chapter: Chapter, read: Boolean) {}

            override suspend fun setProgress(chapter: Chapter, progress: Float) {}

            override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {}
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()

        assertThat(forceNetworkCalls).containsExactly(false, true).inOrder()
        assertThat(vm.state.value.html).isEqualTo("<p>Recovered</p>")
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `loading clears previous content-specific state`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val nextChapter = testChapter(series, url = "https://test.invalid/chapter/2", name = "Chapter 2", number = 2f)
        val gate = CompletableDeferred<Unit>()
        val repo = object : ChapterRepository {
            private var contentCalls = 0

            override fun observeChapters(series: Series) = flowOf(
                listOf(
                    ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
                    ChapterWithState(nextChapter, read = false, downloaded = false, progress = 0f),
                ),
            )

            override suspend fun refreshChapters(series: Series) {}

            override suspend fun findByUrl(sourceId: Long, url: String) = if (url == nextChapter.url) nextChapter else chapter

            override suspend fun getContent(chapter: Chapter, forceNetwork: Boolean): ChapterContent {
                contentCalls++
                return if (contentCalls == 1) {
                    ChapterContent.Text("<p>Chapter body</p>")
                } else {
                    gate.await()
                    ChapterContent.Text("<p>Chapter 2 body</p>")
                }
            }

            override suspend fun markRead(chapter: Chapter, read: Boolean) {}

            override suspend fun setProgress(chapter: Chapter, progress: Float) {}

            override suspend fun markDownloaded(chapter: Chapter, downloaded: Boolean) {}
        }
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )

        advanceUntilIdle()
        vm.onAction(ReaderAction.SetProgress(0.42f))
        advanceUntilIdle()

        assertThat(vm.state.value.html).isEqualTo("<p>Chapter body</p>")
        assertThat(vm.state.value.progress).isEqualTo(0.42f)

        vm.onAction(ReaderAction.Load(nextChapter))

        assertThat(vm.state.value.isLoading).isTrue()
        assertThat(vm.state.value.html).isEmpty()
        assertThat(vm.state.value.pages).isEmpty()
        assertThat(vm.state.value.progress).isEqualTo(0f)
        assertThat(vm.state.value.currentPage).isEqualTo(0)

        gate.complete(Unit)
        advanceUntilIdle()

        assertThat(vm.state.value.html).isEqualTo("<p>Chapter 2 body</p>")
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `DownloadChapter enqueues chapter and emits ShowSnackbar effect for novel route`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val (vm, _, downloadEnqueuer) = createViewModelWithDownloadEnqueuer(
            series = series,
            chapter = chapter,
            contentType = ContentType.NOVEL,
            contentResult = ChapterContent.Text("<p>Chapter body</p>"),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.DownloadChapter)
            val effect = awaitItem() as ReaderEffect.ShowSnackbar
            assertThat(effect.message).contains(chapter.name)
        }

        assertThat(downloadEnqueuer.enqueueChapterCalls).containsExactly(
            FakeDownloadEnqueuer.EnqueueChapterCall(chapter.sourceId, chapter.url),
        )
    }

    @Test
    fun `DownloadChapter enqueues chapter and emits ShowSnackbar effect for manhwa route`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.MANHWA)
        val chapter = testChapter(series)
        val (vm, _, downloadEnqueuer) = createViewModelWithDownloadEnqueuer(
            series = series,
            chapter = chapter,
            contentType = ContentType.MANHWA,
            contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")),
        )

        advanceUntilIdle()

        vm.effects.test {
            vm.onAction(ReaderAction.DownloadChapter)
            val effect = awaitItem() as ReaderEffect.ShowSnackbar
            assertThat(effect.message).contains(chapter.name)
        }

        assertThat(downloadEnqueuer.enqueueChapterCalls).containsExactly(
            FakeDownloadEnqueuer.EnqueueChapterCall(chapter.sourceId, chapter.url),
        )
    }

    @Test
    fun `Retry reloads the current chapter`() = runTest {
        val series = TestFixtures.testSeries(type = ContentType.NOVEL)
        val chapter = testChapter(series)
        val repo = FakeChapterRepository().apply {
            contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg"))
            setChapters(
                series.url,
                listOf(ChapterWithState(chapter, read = false, downloaded = false, progress = 0f)),
            )
        }
        val downloadEnqueuer = FakeDownloadEnqueuer()
        val vm = ReaderViewModel(
            savedState = savedStateHandle(series, chapter, ContentType.NOVEL),
            chapterRepository = repo,
            downloadEnqueuer = downloadEnqueuer,
        )

        advanceUntilIdle()
        assertThat(vm.state.value.error).isEqualTo("Unexpected content type")

        repo.contentResult = ChapterContent.Text("<p>Recovered chapter</p>")
        vm.onAction(ReaderAction.Retry)
        advanceUntilIdle()

        assertThat(repo.getContentCalls).containsExactly(chapter, chapter, chapter).inOrder()
        assertThat(vm.state.value.error).isNull()
        assertThat(vm.state.value.html).isEqualTo("<p>Recovered chapter</p>")
        assertThat(vm.state.value.chapter).isEqualTo(chapter)
    }

    private fun createViewModel(
        series: Series,
        chapter: Chapter,
        contentType: ContentType,
        contentResult: ChapterContent,
        chapters: List<ChapterWithState> = listOf(
            ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
        ),
    ): Pair<ReaderViewModel, FakeChapterRepository> {
        val chapterRepository = FakeChapterRepository().apply {
            this.contentResult = contentResult
            setChapters(series.url, chapters)
        }
        val downloadEnqueuer = FakeDownloadEnqueuer()
        return ReaderViewModel(
            savedState = savedStateHandle(series, chapter, contentType),
            chapterRepository = chapterRepository,
            downloadEnqueuer = downloadEnqueuer,
        ) to chapterRepository
    }

    private fun createViewModelWithDownloadEnqueuer(
        series: Series,
        chapter: Chapter,
        contentType: ContentType,
        contentResult: ChapterContent,
        chapters: List<ChapterWithState> = listOf(
            ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
        ),
    ): Triple<ReaderViewModel, FakeChapterRepository, FakeDownloadEnqueuer> {
        val chapterRepository = FakeChapterRepository().apply {
            this.contentResult = contentResult
            setChapters(series.url, chapters)
        }
        val downloadEnqueuer = FakeDownloadEnqueuer()
        return Triple(
            ReaderViewModel(
                savedState = savedStateHandle(series, chapter, contentType),
                chapterRepository = chapterRepository,
                downloadEnqueuer = downloadEnqueuer,
            ),
            chapterRepository,
            downloadEnqueuer,
        )
    }

    private fun savedStateHandle(series: Series, chapter: Chapter, contentType: ContentType): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                "sourceId" to series.sourceId,
                "seriesUrl" to series.url,
                "chapterUrl" to chapter.url,
                "contentType" to contentType.name,
            ),
        )

    private fun testChapter(
        series: Series,
        url: String = "https://test.invalid/chapter/1",
        name: String = "Chapter 1",
        number: Float = 1f,
    ): Chapter = TestFixtures.testChapter(
        seriesUrl = series.url,
        sourceId = series.sourceId,
        url = url,
        name = name,
        number = number,
    )
}

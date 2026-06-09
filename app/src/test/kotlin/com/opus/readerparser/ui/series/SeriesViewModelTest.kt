package com.opus.readerparser.ui.series

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.fakes.FakeChapterRepository
import com.opus.readerparser.fakes.FakeDownloadEnqueuer
import com.opus.readerparser.fakes.FakeSeriesRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SeriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val series = TestFixtures.testSeries()
    private val chapter = TestFixtures.testChapter()

    private lateinit var seriesRepo: FakeSeriesRepository
    private lateinit var chapterRepo: FakeChapterRepository
    private lateinit var downloadEnqueuer: FakeDownloadEnqueuer
    private lateinit var vm: SeriesViewModel

    @Before
    fun setUp() {
        seriesRepo = FakeSeriesRepository()
        chapterRepo = FakeChapterRepository()
        downloadEnqueuer = FakeDownloadEnqueuer()
        seriesRepo.refreshDetailsResult = { series }
        vm = SeriesViewModel(
            savedState = SavedStateHandle(
                mapOf("sourceId" to series.sourceId, "seriesUrl" to series.url)
            ),
            seriesRepository = seriesRepo,
            chapterRepository = chapterRepo,
            downloadEnqueuer = downloadEnqueuer,
        )
    }

    @Test
    fun `init triggers refresh and populates series in state`() = runTest {
        // With UnconfinedTestDispatcher init coroutines complete synchronously
        assertThat(vm.state.value.series).isEqualTo(series)
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `chapter list is exposed in descending chapter number order`() = runTest {
        val olderChapter = ChapterWithState(
            chapter = chapter.copy(url = "https://test.invalid/chapter/1", number = 1f, name = "Chapter 1"),
            read = false,
            downloaded = false,
            progress = 0f,
        )
        val newerChapter = ChapterWithState(
            chapter = chapter.copy(url = "https://test.invalid/chapter/2", number = 2f, name = "Chapter 2"),
            read = false,
            downloaded = false,
            progress = 0f,
        )

        vm.state.test {
            awaitItem() // settled state (series loaded)
            chapterRepo.setChapters(series.url, listOf(olderChapter, newerChapter))
            assertThat(awaitItem().chapters).containsExactly(newerChapter, olderChapter).inOrder()
        }
    }

    @Test
    fun `ToggleLibrary true calls addToLibrary`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(SeriesAction.ToggleLibrary(true))
            assertThat(awaitItem().inLibrary).isTrue()
        }
        assertThat(seriesRepo.addToLibraryCalls).containsExactly(series)
    }

    @Test
    fun `ToggleLibrary false calls removeFromLibrary`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(SeriesAction.ToggleLibrary(true))
            awaitItem() // inLibrary = true
            vm.onAction(SeriesAction.ToggleLibrary(false))
            assertThat(awaitItem().inLibrary).isFalse()
        }
        assertThat(seriesRepo.removeFromLibraryCalls).containsExactly(series)
    }

    @Test
    fun `OpenChapter emits NavigateToReader effect with series type`() = runTest {
        vm.effects.test {
            vm.onAction(SeriesAction.OpenChapter(chapter))
            val effect = awaitItem() as SeriesEffect.NavigateToReader
            assertThat(effect.chapter).isEqualTo(chapter)
            assertThat(effect.type).isEqualTo(series.type)
        }
    }

    @Test
    fun `Refresh action calls refreshDetails and refreshChapters again`() = runTest {
        // With UnconfinedTestDispatcher, refresh completes synchronously
        vm.onAction(SeriesAction.Refresh)
        assertThat(seriesRepo.refreshDetailsCalls).hasSize(2) // init + Refresh
        assertThat(chapterRepo.refreshChaptersCalls).hasSize(2)
    }

    @Test
    fun `OpenChapter routes to MANHWA reader when series type is MANHWA`() = runTest {
        val manhwaSeries = TestFixtures.testSeries(type = ContentType.MANHWA)
        seriesRepo.refreshDetailsResult = { manhwaSeries }
        val vmManhwa = SeriesViewModel(
            savedState = SavedStateHandle(
                mapOf("sourceId" to manhwaSeries.sourceId, "seriesUrl" to manhwaSeries.url)
            ),
            seriesRepository = seriesRepo,
            chapterRepository = chapterRepo,
            downloadEnqueuer = downloadEnqueuer,
        )
        vmManhwa.effects.test {
            vmManhwa.onAction(SeriesAction.OpenChapter(chapter.copy(seriesUrl = manhwaSeries.url)))
            val effect = awaitItem() as SeriesEffect.NavigateToReader
            assertThat(effect.type).isEqualTo(ContentType.MANHWA)
        }
    }

    @Test
    fun `refresh sets inLibrary from repository`() = runTest {
        seriesRepo.isInLibraryResult = { _, _ -> true }
        vm.onAction(SeriesAction.Refresh)
        assertThat(vm.state.value.inLibrary).isTrue()
    }

    // -----------------------------------------------------------------
    // Download actions
    // -----------------------------------------------------------------

    @Test
    fun `DownloadUnread enqueues unread chapters and shows snackbar`() = runTest {
        val ch1 = ChapterWithState(
            chapter = chapter.copy(url = "https://test.invalid/chapter/1", number = 1f, name = "Chapter 1"),
            read = true,
            downloaded = false,
            progress = 1f,
        )
        val ch2 = ChapterWithState(
            chapter = chapter.copy(url = "https://test.invalid/chapter/2", number = 2f, name = "Chapter 2"),
            read = false,
            downloaded = false,
            progress = 0f,
        )
        chapterRepo.setChapters(series.url, listOf(ch1, ch2))

        vm.effects.test {
            vm.onAction(SeriesAction.DownloadUnread)
            val effect = awaitItem() as SeriesEffect.ShowSnackbar
            // Only ch2 is unread
            assertThat(effect.message).contains("1")
        }
        assertThat(downloadEnqueuer.enqueueBatchCalls).hasSize(1)
        assertThat(downloadEnqueuer.enqueueBatchCalls.first().chapterUrls)
            .containsExactly("https://test.invalid/chapter/2")
    }

    @Test
    fun `DownloadUnread shows message when all chapters are read`() = runTest {
        val ch1 = ChapterWithState(
            chapter = chapter.copy(url = "https://test.invalid/chapter/1", number = 1f),
            read = true,
            downloaded = false,
            progress = 1f,
        )
        chapterRepo.setChapters(series.url, listOf(ch1))

        vm.effects.test {
            vm.onAction(SeriesAction.DownloadUnread)
            val effect = awaitItem() as SeriesEffect.ShowSnackbar
            assertThat(effect.message).isEqualTo("No unread chapters to download")
        }
        assertThat(downloadEnqueuer.enqueueBatchCalls).isEmpty()
    }

    @Test
    fun `ShowRangePicker sets showRangePicker true`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(SeriesAction.ShowRangePicker)
            assertThat(awaitItem().showRangePicker).isTrue()
        }
    }

    @Test
    fun `DownloadRange enqueues selected chapters and dismisses dialog`() = runTest {
        val chapters = listOf(
            ChapterWithState(chapter.copy(url = "c1", number = 1f), read = false, downloaded = false, progress = 0f),
            ChapterWithState(chapter.copy(url = "c2", number = 2f), read = false, downloaded = false, progress = 0f),
            ChapterWithState(chapter.copy(url = "c3", number = 3f), read = false, downloaded = false, progress = 0f),
        )
        chapterRepo.setChapters(series.url, chapters)

        vm.state.test {
            awaitItem() // settled
            vm.onAction(SeriesAction.ShowRangePicker)
            awaitItem() // showRangePicker = true
            vm.onAction(SeriesAction.DownloadRange(startIndex = 0, endIndex = 1))
            val updated = awaitItem()
            assertThat(updated.showRangePicker).isFalse()
        }
        vm.effects.test {
            // The ShowSnackbar effect should have been sent (may need to skip state updates)
            val effect = expectMostRecentItem() as SeriesEffect.ShowSnackbar
            assertThat(effect.message).contains("2")
        }
        assertThat(downloadEnqueuer.enqueueBatchCalls).hasSize(1)
        assertThat(downloadEnqueuer.enqueueBatchCalls.first().chapterUrls).hasSize(2)
    }
}

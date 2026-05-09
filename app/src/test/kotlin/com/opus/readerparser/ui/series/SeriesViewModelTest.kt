package com.opus.readerparser.ui.series

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.fakes.FakeChapterRepository
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
    private lateinit var vm: SeriesViewModel

    @Before
    fun setUp() {
        seriesRepo = FakeSeriesRepository()
        chapterRepo = FakeChapterRepository()
        seriesRepo.refreshDetailsResult = { series }
        vm = SeriesViewModel(
            savedState = SavedStateHandle(
                mapOf("sourceId" to series.sourceId, "seriesUrl" to series.url)
            ),
            seriesRepository = seriesRepo,
            chapterRepository = chapterRepo,
        )
    }

    @Test
    fun `init triggers refresh and populates series in state`() = runTest {
        // With UnconfinedTestDispatcher init coroutines complete synchronously
        assertThat(vm.state.value.series).isEqualTo(series)
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `chapter list updates reflected in state`() = runTest {
        val chapterWithState = ChapterWithState(chapter, read = false, downloaded = false, progress = 0f)

        vm.state.test {
            awaitItem() // settled state (series loaded)
            chapterRepo.setChapters(series.url, listOf(chapterWithState))
            assertThat(awaitItem().chapters).containsExactly(chapterWithState)
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
}

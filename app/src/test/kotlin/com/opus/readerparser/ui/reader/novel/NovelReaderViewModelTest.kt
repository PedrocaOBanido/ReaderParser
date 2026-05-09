package com.opus.readerparser.ui.reader.novel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.fakes.FakeChapterRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NovelReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val series = TestFixtures.testSeries()
    private val chapter = TestFixtures.testChapter()
    private val html = "<p>Chapter body</p>"

    private lateinit var chapterRepo: FakeChapterRepository
    private lateinit var vm: NovelReaderViewModel

    @Before
    fun setUp() {
        chapterRepo = FakeChapterRepository()
        chapterRepo.contentResult = ChapterContent.Text(html)
        chapterRepo.setChapters(
            series.url,
            listOf(ChapterWithState(chapter, read = false, downloaded = false, progress = 0f))
        )
        vm = NovelReaderViewModel(
            savedState = SavedStateHandle(
                mapOf(
                    "sourceId" to series.sourceId,
                    "seriesUrl" to series.url,
                    "chapterUrl" to chapter.url,
                )
            ),
            chapterRepository = chapterRepo,
        )
    }

    @Test
    fun `init loads chapter html into state`() = runTest {
        // With UnconfinedTestDispatcher init completes synchronously before test body
        assertThat(vm.state.value.html).isEqualTo(html)
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.chapter).isEqualTo(chapter)
    }

    @Test
    fun `init marks chapter as read`() = runTest {
        assertThat(chapterRepo.markReadCalls).containsExactly(chapter to true)
    }

    @Test
    fun `SetProgress calls repository and updates state`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(NovelReaderAction.SetProgress(0.42f))
            assertThat(awaitItem().progress).isEqualTo(0.42f)
        }
        assertThat(chapterRepo.setProgressCalls).containsExactly(chapter to 0.42f)
    }

    @Test
    fun `NextChapter emits NavigateToChapter when next exists`() = runTest {
        val next = TestFixtures.testChapter(url = "https://test.invalid/chapter/2", number = 2f)
        chapterRepo.setChapters(
            series.url,
            listOf(
                ChapterWithState(chapter, read = false, downloaded = false, progress = 0f),
                ChapterWithState(next, read = false, downloaded = false, progress = 0f),
            )
        )
        val freshVm = NovelReaderViewModel(
            savedState = SavedStateHandle(
                mapOf(
                    "sourceId" to series.sourceId,
                    "seriesUrl" to series.url,
                    "chapterUrl" to chapter.url,
                )
            ),
            chapterRepository = chapterRepo,
        )
        assertThat(freshVm.state.value.hasNextChapter).isTrue()

        freshVm.effects.test {
            freshVm.onAction(NovelReaderAction.NextChapter)
            val effect = awaitItem() as NovelReaderEffect.NavigateToChapter
            assertThat(effect.chapter).isEqualTo(next)
        }
    }

    @Test
    fun `OpenChapterList emits ShowChapterList effect`() = runTest {
        vm.effects.test {
            vm.onAction(NovelReaderAction.OpenChapterList)
            assertThat(awaitItem()).isEqualTo(NovelReaderEffect.ShowChapterList)
        }
    }

    @Test
    fun `wrong content type sets error in state`() = runTest {
        // Pages content returned for a novel reader triggers the error path
        chapterRepo.contentResult = ChapterContent.Pages(listOf("https://cdn.invalid/p1.jpg"))
        val errorVm = NovelReaderViewModel(
            savedState = SavedStateHandle(
                mapOf(
                    "sourceId" to series.sourceId,
                    "seriesUrl" to series.url,
                    "chapterUrl" to chapter.url,
                )
            ),
            chapterRepository = chapterRepo,
        )
        assertThat(errorVm.state.value.error).isEqualTo("Unexpected content type")
        assertThat(errorVm.state.value.isLoading).isFalse()
    }
}

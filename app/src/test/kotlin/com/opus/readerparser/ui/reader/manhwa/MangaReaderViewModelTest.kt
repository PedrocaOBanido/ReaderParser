package com.opus.readerparser.ui.reader.manhwa

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.fakes.FakeChapterRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MangaReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val series = TestFixtures.testSeries(type = ContentType.MANHWA)
    private val chapter = TestFixtures.testChapter()
    private val pages = listOf("https://cdn.invalid/p1.jpg", "https://cdn.invalid/p2.jpg")

    private lateinit var chapterRepo: FakeChapterRepository
    private lateinit var vm: MangaReaderViewModel

    @Before
    fun setUp() {
        chapterRepo = FakeChapterRepository()
        chapterRepo.contentResult = ChapterContent.Pages(pages)
        chapterRepo.setChapters(
            series.url,
            listOf(ChapterWithState(chapter, read = false, downloaded = false, progress = 0f))
        )
        vm = MangaReaderViewModel(
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
    fun `init loads pages into state`() = runTest {
        assertThat(vm.state.value.pages).isEqualTo(pages)
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.chapter).isEqualTo(chapter)
    }

    @Test
    fun `SetPage updates currentPage in state`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(MangaReaderAction.SetPage(1))
            assertThat(awaitItem().currentPage).isEqualTo(1)
        }
    }

    @Test
    fun `SetPage to last page marks chapter as read`() = runTest {
        vm.state.test {
            awaitItem() // settled state
            vm.onAction(MangaReaderAction.SetPage(pages.lastIndex))
            awaitItem() // currentPage updated
        }
        assertThat(chapterRepo.markReadCalls).containsExactly(chapter to true)
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
        val freshVm = MangaReaderViewModel(
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
            freshVm.onAction(MangaReaderAction.NextChapter)
            val effect = awaitItem() as MangaReaderEffect.NavigateToChapter
            assertThat(effect.chapter).isEqualTo(next)
        }
    }

    @Test
    fun `OpenChapterList emits ShowChapterList effect`() = runTest {
        vm.effects.test {
            vm.onAction(MangaReaderAction.OpenChapterList)
            assertThat(awaitItem()).isEqualTo(MangaReaderEffect.ShowChapterList)
        }
    }

    @Test
    fun `PreviousChapter does not navigate when at first chapter`() = runTest {
        vm.effects.test {
            vm.onAction(MangaReaderAction.PreviousChapter)
            expectNoEvents()
        }
    }
}

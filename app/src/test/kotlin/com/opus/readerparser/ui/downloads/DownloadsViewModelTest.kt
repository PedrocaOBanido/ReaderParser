package com.opus.readerparser.ui.downloads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.fakes.FakeDownloadRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DownloadsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeDownloadRepository
    private lateinit var vm: DownloadsViewModel

    private fun item(chapterUrl: String, state: DownloadState = DownloadState.QUEUED) = DownloadItem(
        sourceId = 1L,
        chapterUrl = chapterUrl,
        state = state,
        progress = 0f,
        errorMessage = null,
    )

    @Before
    fun setUp() {
        repo = FakeDownloadRepository()
        vm = DownloadsViewModel(repo)
    }

    @Test
    fun `init emits empty download list`() = runTest {
        assertThat(vm.state.value.downloads).isEmpty()
    }

    @Test
    fun `queue updates reflected in state`() = runTest {
        vm.state.test {
            awaitItem() // empty

            repo.setQueue(listOf(item("https://test.invalid/ch/1")))
            assertThat(awaitItem().downloads).hasSize(1)
        }
    }

    @Test
    fun `Cancel removes item from state and calls repository`() = runTest {
        val chapterUrl = "https://test.invalid/ch/1"
        vm.state.test {
            awaitItem() // empty

            repo.setQueue(listOf(item(chapterUrl)))
            awaitItem() // one item

            vm.onAction(DownloadsAction.Cancel(1L, chapterUrl))
            assertThat(awaitItem().downloads).isEmpty()
        }
        assertThat(repo.cancelCalls).containsExactly(1L to chapterUrl)
    }

    @Test
    fun `Retry resets failed item state and calls repository`() = runTest {
        val chapterUrl = "https://test.invalid/ch/1"
        vm.state.test {
            awaitItem() // empty

            repo.setQueue(listOf(item(chapterUrl, DownloadState.FAILED)))
            awaitItem() // failed item

            vm.onAction(DownloadsAction.Retry(1L, chapterUrl))
            val updated = awaitItem()
            assertThat(updated.downloads.first().state).isEqualTo(DownloadState.QUEUED)
        }
        assertThat(repo.retryCalls).containsExactly(1L to chapterUrl)
    }
}

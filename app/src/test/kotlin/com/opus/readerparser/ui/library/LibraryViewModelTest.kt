package com.opus.readerparser.ui.library

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.fakes.FakeSeriesRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeSeriesRepository
    private lateinit var vm: LibraryViewModel

    @Before
    fun setUp() {
        repo = FakeSeriesRepository()
        vm = LibraryViewModel(repo)
    }

    @Test
    fun `init emits empty library`() = runTest {
        assertThat(vm.state.value.series).isEmpty()
    }

    @Test
    fun `library updates reflected in state`() = runTest {
        val series = TestFixtures.testSeries(title = "My Novel")
        vm.state.test {
            awaitItem() // initial empty
            repo.addToLibrary(series)
            assertThat(awaitItem().series).containsExactly(series)
        }
    }

    @Test
    fun `SetSortBy TITLE sorts alphabetically`() = runTest {
        val a = TestFixtures.testSeries(title = "Zebra", url = "https://test.invalid/z")
        val b = TestFixtures.testSeries(title = "Apple", url = "https://test.invalid/a")

        vm.state.test {
            awaitItem() // initial empty
            repo.addToLibrary(a)
            awaitItem() // [Zebra]
            repo.addToLibrary(b)
            awaitItem() // [Zebra, Apple]

            vm.onAction(LibraryAction.SetSortBy(LibrarySortBy.TITLE))
            val sorted = awaitItem()
            assertThat(sorted.series.map { it.title }).containsExactly("Apple", "Zebra").inOrder()
        }
    }

    @Test
    fun `SetFilterUnreadOnly updates flag in state`() = runTest {
        vm.state.test {
            awaitItem()
            vm.onAction(LibraryAction.SetFilterUnreadOnly(true))
            assertThat(awaitItem().filterUnreadOnly).isTrue()
        }
    }

    @Test
    fun `RemoveFromLibrary calls repository and updates state`() = runTest {
        val series = TestFixtures.testSeries()
        vm.state.test {
            awaitItem() // empty
            repo.addToLibrary(series)
            awaitItem() // [series]
            vm.onAction(LibraryAction.RemoveFromLibrary(series))
            assertThat(awaitItem().series).isEmpty()
        }
        assertThat(repo.removeFromLibraryCalls).containsExactly(series)
    }

    @Test
    fun `OpenSeries emits NavigateToSeries effect`() = runTest {
        val series = TestFixtures.testSeries()
        vm.effects.test {
            vm.onAction(LibraryAction.OpenSeries(series))
            assertThat(awaitItem()).isEqualTo(LibraryEffect.NavigateToSeries(series))
        }
    }
}

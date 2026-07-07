package com.opus.readerparser.ui.library

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.fakes.FakeSeriesRepository
import com.opus.readerparser.domain.model.LibrarySearchResult
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    // -----------------------------------------------------------------
    // Search tests
    // -----------------------------------------------------------------

    @Test
    fun `SetSearchQuery updates searchQuery in state`() = runTest {
        vm.onAction(LibraryAction.SetSearchQuery("query"))
        advanceUntilIdle()
        assertThat(vm.state.value.searchQuery).isEqualTo("query")
    }

    @Test
    fun `blank search keeps observed library sorted locally`() = runTest {
        val zebra = TestFixtures.testSeries(title = "Zebra", url = "https://test.invalid/z")
        val apple = TestFixtures.testSeries(title = "Apple", url = "https://test.invalid/a")

        repo.addToLibrary(zebra)
        advanceUntilIdle()
        repo.addToLibrary(apple)
        advanceUntilIdle()

        vm.onAction(LibraryAction.SetSortBy(LibrarySortBy.TITLE))
        advanceUntilIdle()
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Apple", "Zebra").inOrder()
        assertThat(repo.searchLibraryCalls).isEmpty()
    }

    @Test
    fun `non blank search uses samsung search results`() = runTest {
        val first = TestFixtures.testSeries(title = "Zebra", url = "https://test.invalid/z")
        val second = TestFixtures.testSeries(title = "Apple", url = "https://test.invalid/a")
        repo.searchLibraryHandler = { LibrarySearchResult.Success(listOf(first, second)) }

        vm.onAction(LibraryAction.SetSearchQuery("search me"))
        advanceUntilIdle()

        assertThat(repo.searchLibraryCalls).containsExactly("search me")
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Zebra", "Apple").inOrder()
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `active search refreshes after remove from library`() = runTest {
        val series = TestFixtures.testSeries(title = "Solo Leveling", url = "https://test.invalid/solo")
        repo.addToLibrary(series)
        advanceUntilIdle()
        repo.searchLibraryHandler = {
            if (repo.searchLibraryCalls.size == 1) {
                LibrarySearchResult.Success(listOf(series))
            } else {
                LibrarySearchResult.Success(emptyList())
            }
        }

        vm.onAction(LibraryAction.SetSearchQuery("Solo"))
        advanceUntilIdle()
        assertThat(vm.state.value.series).containsExactly(series)

        vm.onAction(LibraryAction.RemoveFromLibrary(series))
        advanceUntilIdle()

        assertThat(repo.removeFromLibraryCalls).containsExactly(series)
        assertThat(repo.searchLibraryCalls).hasSize(2)
        assertThat(vm.state.value.series).isEmpty()
    }

    @Test
    fun `active search refreshes after search invalidation`() = runTest {
        val series = TestFixtures.testSeries(title = "Solo Leveling", url = "https://test.invalid/solo")
        repo.addToLibrary(series)
        advanceUntilIdle()

        repo.searchLibraryHandler = {
            if (repo.searchLibraryCalls.size == 1) {
                LibrarySearchResult.Success(listOf(series))
            } else {
                LibrarySearchResult.Success(emptyList())
            }
        }

        vm.onAction(LibraryAction.SetSearchQuery("Solo"))
        advanceUntilIdle()
        assertThat(vm.state.value.series).containsExactly(series)

        repo.emitLibrarySearchInvalidation()
        advanceUntilIdle()

        assertThat(repo.searchLibraryCalls).hasSize(2)
        assertThat(vm.state.value.series).isEmpty()
    }

    @Test
    fun `latest search query wins`() = runTest {
        val first = TestFixtures.testSeries(title = "First", url = "https://test.invalid/first")
        val second = TestFixtures.testSeries(title = "Second", url = "https://test.invalid/second")

        repo.searchLibraryHandler = { query ->
            when (query) {
                "first" -> {
                    delay(100)
                    LibrarySearchResult.Success(listOf(first))
                }
                else -> LibrarySearchResult.Success(listOf(second))
            }
        }

        vm.onAction(LibraryAction.SetSearchQuery("first"))
        vm.onAction(LibraryAction.SetSearchQuery("second"))
        advanceUntilIdle()

        assertThat(vm.state.value.series.map { it.title }).containsExactly("Second")
    }

    @Test
    fun `provider failure shows error distinct from empty results`() = runTest {
        repo.searchLibraryHandler = { LibrarySearchResult.Failure("provider failed") }

        vm.onAction(LibraryAction.SetSearchQuery("query"))
        advanceUntilIdle()

        assertThat(vm.state.value.series).isEmpty()
        assertThat(vm.state.value.error).isEqualTo("provider failed")
    }

    @Test
    fun `unexpected search exception shows error state`() = runTest {
        repo.searchLibraryHandler = { throw IllegalStateException("boom") }

        vm.onAction(LibraryAction.SetSearchQuery("query"))
        advanceUntilIdle()

        assertThat(vm.state.value.series).isEmpty()
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isEqualTo("boom")
    }

    @Test
    fun `empty search results do not set error`() = runTest {
        repo.searchLibraryHandler = { LibrarySearchResult.Success(emptyList()) }

        vm.onAction(LibraryAction.SetSearchQuery("query"))
        advanceUntilIdle()

        assertThat(vm.state.value.series).isEmpty()
        assertThat(vm.state.value.error).isNull()
    }
}

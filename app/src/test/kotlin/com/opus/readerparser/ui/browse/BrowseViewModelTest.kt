package com.opus.readerparser.ui.browse

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.domain.model.SourceInfo
import com.opus.readerparser.fakes.FakeSeriesRepository
import com.opus.readerparser.fakes.FakeSourceRepository
import com.opus.readerparser.fakes.SearchCall
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sourceRepo: FakeSourceRepository
    private lateinit var seriesRepo: FakeSeriesRepository

    private val source1 = SourceInfo(1L, "SourceA", "en", ContentType.MANHWA)
    private val source2 = SourceInfo(2L, "SourceB", "en", ContentType.NOVEL)

    @Before
    fun setUp() {
        sourceRepo = FakeSourceRepository()
        seriesRepo = FakeSeriesRepository()
    }

    private fun buildVm(): BrowseViewModel = BrowseViewModel(sourceRepo, seriesRepo)

    @Test
    fun `init loads sources and selects first source without fetching`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)

        val vm = buildVm()

        assertThat(vm.state.value.sources).containsExactly(source1, source2).inOrder()
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source1.id)
        assertThat(seriesRepo.fetchPopularCalls).isEmpty()
        assertThat(seriesRepo.fetchLatestCalls).isEmpty()
        assertThat(seriesRepo.searchCalls).isEmpty()
    }

    @Test
    fun `select source invalidates stale popular responses and mode changes auto fetch latest`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.fetchPopularHandler = { sourceId, _ ->
            if (sourceId == source1.id) {
                gate.await()
                SeriesPage(listOf(TestFixtures.testSeries(title = "Stale popular")), hasNextPage = false)
            } else {
                SeriesPage(listOf(TestFixtures.testSeries(title = "Fresh popular")), hasNextPage = false)
            }
        }
        seriesRepo.fetchLatestHandler = { _, _ ->
            SeriesPage(listOf(TestFixtures.testSeries(title = "Latest")), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SelectSource(source1.id))
        runCurrent()

        vm.onAction(BrowseAction.SelectSource(source2.id))
        runCurrent()

        gate.complete(Unit)
        advanceUntilIdle()

        assertThat(seriesRepo.fetchPopularCalls).containsExactly(source1.id to 1, source2.id to 1).inOrder()
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source2.id)
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Fresh popular")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `select source while loading clears loading immediately`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.searchHandler = { _, query, _, _ ->
            gate.await()
            SeriesPage(listOf(TestFixtures.testSeries(title = query)), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)
        runCurrent()

        assertThat(vm.state.value.isLoading).isTrue()

        vm.onAction(BrowseAction.SelectSource(source2.id))

        assertThat(vm.state.value.selectedSourceId).isEqualTo(source2.id)
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isNull()

        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `set mode invalidates stale popular responses and auto fetches latest`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.fetchPopularHandler = { _, _ ->
            gate.await()
            SeriesPage(listOf(TestFixtures.testSeries(title = "Stale popular")), hasNextPage = false)
        }
        seriesRepo.fetchLatestHandler = { _, _ ->
            SeriesPage(listOf(TestFixtures.testSeries(title = "Latest")), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetMode(BrowseMode.POPULAR))
        runCurrent()

        vm.onAction(BrowseAction.SetMode(BrowseMode.LATEST))
        advanceUntilIdle()

        gate.complete(Unit)
        advanceUntilIdle()

        assertThat(seriesRepo.fetchPopularCalls).containsExactly(source1.id to 1)
        assertThat(seriesRepo.fetchLatestCalls).containsExactly(source1.id to 1)
        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.LATEST)
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Latest")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `set mode while loading clears loading immediately`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.fetchPopularHandler = { _, _ ->
            gate.await()
            SeriesPage(listOf(TestFixtures.testSeries(title = "Stale popular")), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetMode(BrowseMode.POPULAR))
        runCurrent()

        assertThat(vm.state.value.isLoading).isTrue()

        vm.onAction(BrowseAction.SetMode(BrowseMode.SEARCH))

        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.SEARCH)
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isNull()

        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `search mode waits for explicit submission`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)

        val vm = buildVm()
        vm.onAction(BrowseAction.SetMode(BrowseMode.SEARCH))
        vm.onAction(BrowseAction.SelectSource(source2.id))
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        advanceUntilIdle()

        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.SEARCH)
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source2.id)
        assertThat(seriesRepo.fetchPopularCalls).isEmpty()
        assertThat(seriesRepo.fetchLatestCalls).isEmpty()
        assertThat(seriesRepo.searchCalls).isEmpty()
    }

    @Test
    fun `selecting a source auto fetches the current latest mode`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)
        seriesRepo.fetchLatestHandler = { sourceId, _ ->
            SeriesPage(listOf(TestFixtures.testSeries(title = "Latest $sourceId")), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetMode(BrowseMode.LATEST))
        runCurrent()

        vm.onAction(BrowseAction.SelectSource(source2.id))
        advanceUntilIdle()

        assertThat(seriesRepo.fetchLatestCalls).containsExactly(source1.id to 1, source2.id to 1).inOrder()
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source2.id)
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Latest ${source2.id}")
    }

    @Test
    fun `explicit search triggers fetch and shows loading while in flight`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.searchHandler = { _, query, _, _ ->
            gate.await()
            SeriesPage(listOf(TestFixtures.testSeries(title = query)), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)
        runCurrent()

        assertThat(vm.state.value.isLoading).isTrue()
        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.SEARCH)
        assertThat(seriesRepo.searchCalls.last().sourceId).isEqualTo(source1.id)
        assertThat(seriesRepo.searchCalls.last().query).isEqualTo("dragon")
        assertThat(seriesRepo.searchCalls.last().page).isEqualTo(1)

        gate.complete(Unit)
        advanceUntilIdle()

        assertThat(vm.state.value.series.map { it.title }).containsExactly("dragon")
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `blank search clears loading without launching a request`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val gate = CompletableDeferred<Unit>()
        seriesRepo.searchHandler = { _, query, _, _ ->
            gate.await()
            SeriesPage(listOf(TestFixtures.testSeries(title = query)), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)
        runCurrent()

        assertThat(vm.state.value.isLoading).isTrue()

        vm.onAction(BrowseAction.SetSearchQuery("   "))
        vm.onAction(BrowseAction.Search)

        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.SEARCH)
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isNull()
        assertThat(seriesRepo.searchCalls).hasSize(1)

        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `retry reissues the submitted request after failure`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        var attempts = 0
        seriesRepo.searchHandler = { _, _, _, _ ->
            attempts += 1
            if (attempts == 1) {
                throw IllegalStateException("boom")
            }
            SeriesPage(listOf(TestFixtures.testSeries(title = "Recovered")), hasNextPage = false)
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)
        advanceUntilIdle()

        assertThat(vm.state.value.error).isEqualTo("boom")
        assertThat(vm.state.value.series).isEmpty()

        vm.onAction(BrowseAction.Retry)
        advanceUntilIdle()

        assertThat(seriesRepo.searchCalls).hasSize(2)
        assertThat(seriesRepo.searchCalls.all { it.sourceId == source1.id && it.query == "dragon" && it.page == 1 }).isTrue()
        assertThat(vm.state.value.series.map { it.title }).containsExactly("Recovered")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `stale response from an earlier search is ignored`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        seriesRepo.searchHandler = { _, query, _, _ ->
            when (query) {
                "first" -> {
                    delay(100)
                    SeriesPage(listOf(TestFixtures.testSeries(title = "First")), hasNextPage = false)
                }
                else -> SeriesPage(listOf(TestFixtures.testSeries(title = "Second")), hasNextPage = false)
            }
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("first"))
        vm.onAction(BrowseAction.Search)
        vm.onAction(BrowseAction.SetSearchQuery("second"))
        vm.onAction(BrowseAction.Search)
        advanceUntilIdle()

        assertThat(vm.state.value.series.map { it.title }).containsExactly("Second")
        assertThat(vm.state.value.error).isNull()
    }

    @Test
    fun `LoadMore uses the submitted query even after editing search text`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val first = TestFixtures.testSeries(title = "Page 1", url = "https://test.invalid/p1")
        val second = TestFixtures.testSeries(title = "Page 2", url = "https://test.invalid/p2")
        seriesRepo.searchHandler = { _, _, page, _ ->
            when (page) {
                1 -> SeriesPage(listOf(first), hasNextPage = true)
                2 -> SeriesPage(listOf(second), hasNextPage = false)
                else -> error("unexpected page $page")
            }
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)
        advanceUntilIdle()

        vm.onAction(BrowseAction.SetSearchQuery("dragon edited"))

        vm.onAction(BrowseAction.LoadMore)
        advanceUntilIdle()

        assertThat(vm.state.value.series).containsExactly(first, second).inOrder()
        assertThat(vm.state.value.currentPage).isEqualTo(2)
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source1.id)
        assertThat(seriesRepo.searchCalls).containsExactly(
            SearchCall(source1.id, "dragon", 1, FilterList()),
            SearchCall(source1.id, "dragon", 2, FilterList()),
        ).inOrder()
    }

    @Test
    fun `stale page response is ignored after a newer search`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val pageTwoGate = CompletableDeferred<Unit>()
        seriesRepo.searchHandler = { _, query, page, _ ->
            when {
                query == "first" && page == 1 -> SeriesPage(
                    listOf(TestFixtures.testSeries(title = "First Page 1")),
                    hasNextPage = true,
                )
                query == "first" && page == 2 -> {
                    pageTwoGate.await()
                    SeriesPage(listOf(TestFixtures.testSeries(title = "First Page 2")), hasNextPage = false)
                }
                else -> SeriesPage(listOf(TestFixtures.testSeries(title = "Second Page 1")), hasNextPage = false)
            }
        }

        val vm = buildVm()
        vm.onAction(BrowseAction.SetSearchQuery("first"))
        vm.onAction(BrowseAction.Search)
        advanceUntilIdle()

        vm.onAction(BrowseAction.LoadMore)
        vm.onAction(BrowseAction.SetSearchQuery("second"))
        vm.onAction(BrowseAction.Search)
        pageTwoGate.complete(Unit)
        advanceUntilIdle()

        assertThat(vm.state.value.series.map { it.title }).containsExactly("Second Page 1")
        assertThat(vm.state.value.currentPage).isEqualTo(1)
    }

    @Test
    fun `OpenSeries emits NavigateToSeries effect`() = runTest {
        sourceRepo.sourceList = emptyList()
        val vm = buildVm()
        val series = TestFixtures.testSeries()

        vm.effects.test {
            vm.onAction(BrowseAction.OpenSeries(series))
            assertThat(awaitItem()).isEqualTo(BrowseEffect.NavigateToSeries(series))
        }
    }
}

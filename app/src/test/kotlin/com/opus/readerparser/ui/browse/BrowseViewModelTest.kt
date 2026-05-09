package com.opus.readerparser.ui.browse

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.domain.model.SourceInfo
import com.opus.readerparser.fakes.FakeSeriesRepository
import com.opus.readerparser.fakes.FakeSourceRepository
import com.opus.readerparser.testutil.MainDispatcherRule
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    fun `init loads sources and auto-selects first`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)
        val vm = buildVm()
        assertThat(vm.state.value.sources).containsExactly(source1, source2).inOrder()
        assertThat(vm.state.value.selectedSourceId).isEqualTo(source1.id)
    }

    @Test
    fun `init fetches popular page 1 for first source`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val page = TestFixtures.testSeriesPage()
        seriesRepo.popularResult = page
        val vm = buildVm()
        assertThat(vm.state.value.series).isEqualTo(page.series)
        assertThat(seriesRepo.fetchPopularCalls).containsExactly(source1.id to 1)
    }

    @Test
    fun `SelectSource resets pagination and fetches page 1`() = runTest {
        sourceRepo.sourceList = listOf(source1, source2)
        val vm = buildVm()

        vm.onAction(BrowseAction.SelectSource(source2.id))

        assertThat(vm.state.value.selectedSourceId).isEqualTo(source2.id)
        assertThat(vm.state.value.currentPage).isEqualTo(1)
        assertThat(seriesRepo.fetchPopularCalls.last()).isEqualTo(source2.id to 1)
    }

    @Test
    fun `SetMode LATEST switches mode and fetches`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val vm = buildVm()

        vm.onAction(BrowseAction.SetMode(BrowseMode.LATEST))

        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.LATEST)
        assertThat(seriesRepo.fetchLatestCalls).isNotEmpty()
    }

    @Test
    fun `LoadMore appends next page when hasNextPage`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val s1 = TestFixtures.testSeries(url = "https://test.invalid/1")
        val s2 = TestFixtures.testSeries(url = "https://test.invalid/2")
        seriesRepo.popularResult = SeriesPage(listOf(s1), hasNextPage = true)
        val vm = buildVm()

        seriesRepo.popularResult = SeriesPage(listOf(s2), hasNextPage = false)
        vm.onAction(BrowseAction.LoadMore)

        assertThat(vm.state.value.series).containsExactly(s1, s2).inOrder()
        assertThat(vm.state.value.currentPage).isEqualTo(2)
        assertThat(vm.state.value.hasNextPage).isFalse()
    }

    @Test
    fun `Search resets to page 1 and fetches search results`() = runTest {
        sourceRepo.sourceList = listOf(source1)
        val vm = buildVm()
        val results = TestFixtures.testSeriesPage()
        seriesRepo.searchResult = results

        vm.onAction(BrowseAction.SetSearchQuery("dragon"))
        vm.onAction(BrowseAction.Search)

        assertThat(vm.state.value.mode).isEqualTo(BrowseMode.SEARCH)
        assertThat(vm.state.value.series).isEqualTo(results.series)
        assertThat(seriesRepo.searchCalls.last().query).isEqualTo("dragon")
        assertThat(seriesRepo.searchCalls.last().page).isEqualTo(1)
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

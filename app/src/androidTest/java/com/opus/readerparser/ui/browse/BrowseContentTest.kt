package com.opus.readerparser.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SourceInfo
import com.opus.readerparser.ui.theme.ReaderParserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowseContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(state = BrowseUiState(isLoading = true), onAction = {})
            }
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(state = BrowseUiState(error = "Network error", retryAvailable = true), onAction = {})
            }
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Network error").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun populatedState_showsSeriesList() {
        val source = SourceInfo(id = 1L, name = "AsuraScans", lang = "en", type = ContentType.MANHWA)
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "Eleceed", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url2", title = "Omniscient Reader", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(sources = listOf(source), selectedSourceId = 1L, series = series),
                    onAction = {},
                )
            }
        }
        composeRule.onNodeWithTag("series_list").assertIsDisplayed()
        composeRule.onNodeWithText("Eleceed").assertIsDisplayed()
    }

    @Test
    fun paginationLoading_showsSpinnerNearLoadMore() {
        val source = SourceInfo(id = 1L, name = "AsuraScans", lang = "en", type = ContentType.MANHWA)
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "Eleceed", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(
                        sources = listOf(source),
                        selectedSourceId = 1L,
                        series = series,
                        hasNextPage = true,
                        isLoading = true,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag("pagination_loading").assertIsDisplayed()
    }

    @Test
    fun sharedCard_rendersCoverTitle_andInvokesOpenAction() {
        val series = Series(sourceId = 1L, url = "url1", title = "Click Me", type = ContentType.MANHWA)
        var opened: Series? = null
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(300.dp).requiredHeight(500.dp)) {
                    BrowseContent(
                        state = BrowseUiState(series = listOf(series), hasNextPage = true),
                        onAction = { action ->
                            if (action is BrowseAction.OpenSeries) opened = action.series
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Click Me").assertIsDisplayed()
        composeRule.onNodeWithText("Click Me").assertIsDisplayed()
        composeRule.onNodeWithTag("series_card").performClick()

        assertThat(opened).isEqualTo(series)
    }

    @Test
    fun expandedGrid_usesMoreThanOneColumn() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "First", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url2", title = "Second", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url3", title = "Third", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(520.dp).requiredHeight(600.dp)) {
                    BrowseContent(state = BrowseUiState(series = series), onAction = {})
                }
            }
        }

        val bounds = composeRule.onAllNodesWithTag("series_card").fetchSemanticsNodes().map { it.boundsInRoot }
        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].top).isEqualTo(bounds[0].top)
        assertThat(bounds[2].top).isEqualTo(bounds[0].top)
    }

    @Test
    fun compactGrid_keepsCardsReadable() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "First", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url2", title = "Second", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url3", title = "Third", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(280.dp).requiredHeight(1000.dp)) {
                    BrowseContent(state = BrowseUiState(series = series), onAction = {})
                }
            }
        }

        val bounds = composeRule.onAllNodesWithTag("series_card").fetchSemanticsNodes().map { it.boundsInRoot }
        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].top).isGreaterThan(bounds[0].top)
        assertThat(bounds[2].top).isGreaterThan(bounds[1].top)
    }

    @Test
    fun searchButton_dispatchesSearchAction() {
        var action: BrowseAction? = null
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(mode = BrowseMode.SEARCH, searchQuery = "dragon"),
                    onAction = { action = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search").performClick()
        assertThat(action).isEqualTo(BrowseAction.Search)
    }

    @Test
    fun retryButton_dispatchesRetryAction() {
        var action: BrowseAction? = null
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(error = "Network error", retryAvailable = true),
                    onAction = { action = it },
                )
            }
        }

        composeRule.onNodeWithText("Retry").performClick()
        assertThat(action).isEqualTo(BrowseAction.Retry)
    }

    @Test
    fun loadMore_dispatchesLoadMoreAction() {
        var action: BrowseAction? = null
        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(
                        series = listOf(Series(sourceId = 1L, url = "url1", title = "Eleceed", type = ContentType.MANHWA)),
                        hasNextPage = true,
                    ),
                    onAction = { action = it },
                )
            }
        }

        composeRule.onNodeWithText("Load more").performClick()
        assertThat(action).isEqualTo(BrowseAction.LoadMore)
    }

    @Test
    fun scrollToEnd_dispatchesLoadMoreAutomatically() {
        val actions = mutableListOf<BrowseAction>()
        val series = List(12) { index ->
            Series(sourceId = 1L, url = "url$index", title = "Series $index", type = ContentType.MANHWA)
        }

        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(400.dp).requiredHeight(800.dp)) {
                    BrowseContent(
                        state = BrowseUiState(series = series, hasNextPage = true),
                        onAction = actions::add,
                    )
                }
            }
        }

        assertThat(actions.any { it is BrowseAction.LoadMore }).isFalse()
        composeRule.onNodeWithTag("series_list").performScrollToIndex(series.lastIndex)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            actions.any { it is BrowseAction.LoadMore }
        }
        assertThat(actions.any { it is BrowseAction.LoadMore }).isTrue()
    }

    @Test
    fun manualLoadMoreButton_remainsClickable() {
        var loadMoreClicked = false

        composeRule.setContent {
            ReaderParserTheme {
                BrowseContent(
                    state = BrowseUiState(
                        series = listOf(
                            Series(sourceId = 1L, url = "url1", title = "Series 1", type = ContentType.MANHWA),
                        ),
                        hasNextPage = true,
                        isLoading = true,
                    ),
                    onAction = { if (it is BrowseAction.LoadMore) loadMoreClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Load more").assertIsDisplayed().performClick()
        assertThat(loadMoreClicked).isTrue()
    }
}

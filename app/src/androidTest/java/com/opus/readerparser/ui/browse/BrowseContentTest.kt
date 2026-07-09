package com.opus.readerparser.ui.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SourceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            BrowseContent(state = BrowseUiState(isLoading = true), onAction = {})
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            BrowseContent(state = BrowseUiState(error = "Network error"), onAction = {})
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun populatedState_showsSeriesList() {
        val source = SourceInfo(id = 1L, name = "AsuraScans", lang = "en", type = ContentType.MANHWA)
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "Eleceed", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url2", title = "Omniscient Reader", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            BrowseContent(
                state = BrowseUiState(sources = listOf(source), selectedSourceId = 1L, series = series),
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("series_list").assertIsDisplayed()
        composeRule.onNodeWithText("Eleceed").assertIsDisplayed()
    }

    @Test
    fun clickSeries_dispatchesOpenSeriesAction() {
        val series = Series(sourceId = 1L, url = "url1", title = "Click Me", type = ContentType.MANHWA)
        var opened: Series? = null
        composeRule.setContent {
            BrowseContent(
                state = BrowseUiState(series = listOf(series)),
                onAction = { action ->
                    if (action is BrowseAction.OpenSeries) opened = action.series
                },
            )
        }
        composeRule.onNodeWithText("Click Me").performClick()
        assertEquals(series, opened)
    }

    @Test
    fun scrollToEnd_dispatchesLoadMoreAutomatically() {
        val actions = mutableListOf<BrowseAction>()
        val series = List(12) { index ->
            Series(sourceId = 1L, url = "url$index", title = "Series $index", type = ContentType.MANHWA)
        }

        composeRule.setContent {
            Box(modifier = Modifier.size(400.dp, 800.dp)) {
                BrowseContent(
                    state = BrowseUiState(series = series, hasNextPage = true),
                    onAction = actions::add,
                )
            }
        }

        assertTrue(actions.none { it is BrowseAction.LoadMore })
        composeRule.onNodeWithTag("series_list").performScrollToIndex(series.lastIndex)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            actions.any { it is BrowseAction.LoadMore }
        }
        assertTrue(actions.any { it is BrowseAction.LoadMore })
    }

    @Test
    fun manualLoadMoreButton_remainsClickable() {
        var loadMoreClicked = false

        composeRule.setContent {
            BrowseContent(
                state = BrowseUiState(series = listOf(
                    Series(sourceId = 1L, url = "url1", title = "Series 1", type = ContentType.MANHWA),
                ), hasNextPage = true, isLoading = true),
                onAction = { if (it is BrowseAction.LoadMore) loadMoreClicked = true },
            )
        }

        composeRule.onNodeWithText("Load more").assertIsDisplayed().performClick()
        assertTrue(loadMoreClicked)
    }
}

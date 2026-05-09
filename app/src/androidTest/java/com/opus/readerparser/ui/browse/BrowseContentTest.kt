package com.opus.readerparser.ui.browse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SourceInfo
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
        assert(opened == series)
    }
}

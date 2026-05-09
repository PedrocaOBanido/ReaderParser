package com.opus.readerparser.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performLongClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            LibraryContent(state = LibraryUiState(isLoading = true), onAction = {})
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            LibraryContent(state = LibraryUiState(error = "Something went wrong"), onAction = {})
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun populatedState_showsSeriesList() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "The Wandering Inn", type = ContentType.NOVEL),
            Series(sourceId = 1L, url = "url2", title = "Solo Leveling", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            LibraryContent(state = LibraryUiState(series = series), onAction = {})
        }
        composeRule.onNodeWithTag("series_list").assertIsDisplayed()
        composeRule.onNodeWithText("The Wandering Inn").assertIsDisplayed()
        composeRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
    }

    @Test
    fun longPressSeries_dispatchesRemoveAction() {
        val series = Series(sourceId = 1L, url = "url1", title = "Test Series", type = ContentType.NOVEL)
        var removedSeries: Series? = null
        composeRule.setContent {
            LibraryContent(
                state = LibraryUiState(series = listOf(series)),
                onAction = { action ->
                    if (action is LibraryAction.RemoveFromLibrary) {
                        removedSeries = action.series
                    }
                },
            )
        }
        composeRule.onNodeWithText("Test Series").performLongClick()
        assert(removedSeries == series)
    }
}

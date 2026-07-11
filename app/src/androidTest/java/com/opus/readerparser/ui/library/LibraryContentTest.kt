package com.opus.readerparser.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.ui.theme.ReaderParserTheme
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
            ReaderParserTheme {
                LibraryContent(state = LibraryUiState(isLoading = true), onAction = {})
            }
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            ReaderParserTheme {
                LibraryContent(state = LibraryUiState(error = "Something went wrong"), onAction = {})
            }
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun unreadFilter_isAbsent() {
        val actions = mutableListOf<LibraryAction>()
        composeRule.setContent {
            ReaderParserTheme {
                LibraryContent(
                    state = LibraryUiState(
                        series = listOf(
                            Series(sourceId = 1L, url = "url1", title = "Test Series", type = ContentType.NOVEL),
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onAllNodesWithText("Unread").assertCountEquals(0)
        composeRule.onNodeWithTag("search_field").performTextInput("Library query")
        composeRule.onNodeWithContentDescription("Sort").performClick()
        composeRule.onNodeWithText("Title").performClick()
        composeRule.onNodeWithTag("series_card").performTouchInput { longClick() }

        assertThat(actions.any { it is LibraryAction.SetSearchQuery }).isTrue()
        assertThat(actions.any { it is LibraryAction.SetSortBy }).isTrue()
        assertThat(actions.any { it is LibraryAction.RemoveFromLibrary }).isTrue()
    }

    @Test
    fun populatedState_showsSeriesList() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "The Wandering Inn", type = ContentType.NOVEL),
            Series(sourceId = 1L, url = "url2", title = "Solo Leveling", type = ContentType.MANHWA),
        )
        composeRule.setContent {
            ReaderParserTheme {
                LibraryContent(state = LibraryUiState(series = series), onAction = {})
            }
        }
        composeRule.onNodeWithTag("series_list").assertIsDisplayed()
        composeRule.onNodeWithText("The Wandering Inn").assertIsDisplayed()
        composeRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
    }

    @Test
    fun sharedCard_rendersCoverTitle_andInvokesOpenAction() {
        val series = Series(sourceId = 1L, url = "url1", title = "Test Series", type = ContentType.NOVEL)
        var openedSeries: Series? = null
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(300.dp).requiredHeight(500.dp)) {
                    LibraryContent(
                        state = LibraryUiState(series = listOf(series)),
                        onAction = { action ->
                            if (action is LibraryAction.OpenSeries) {
                                openedSeries = action.series
                            }
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Test Series").assertIsDisplayed()
        composeRule.onNodeWithText("Test Series").assertIsDisplayed()
        composeRule.onNodeWithTag("series_card").performClick()

        assertThat(openedSeries).isEqualTo(series)
    }

    @Test
    fun compactGrid_rendersCardsInSingleColumn() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "First", type = ContentType.NOVEL),
            Series(sourceId = 1L, url = "url2", title = "Second", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url3", title = "Third", type = ContentType.NOVEL),
        )
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(280.dp).requiredHeight(1000.dp)) {
                    LibraryContent(state = LibraryUiState(series = series), onAction = {})
                }
            }
        }

        val bounds = composeRule.onAllNodesWithTag("series_card").fetchSemanticsNodes().map { it.boundsInRoot }
        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].top).isGreaterThan(bounds[0].top)
        assertThat(bounds[2].top).isGreaterThan(bounds[1].top)
    }

    @Test
    fun expandedGrid_usesAdditionalColumns() {
        val series = listOf(
            Series(sourceId = 1L, url = "url1", title = "First", type = ContentType.NOVEL),
            Series(sourceId = 1L, url = "url2", title = "Second", type = ContentType.MANHWA),
            Series(sourceId = 1L, url = "url3", title = "Third", type = ContentType.NOVEL),
        )
        composeRule.setContent {
            ReaderParserTheme {
                Box(modifier = Modifier.requiredWidth(520.dp).requiredHeight(600.dp)) {
                    LibraryContent(state = LibraryUiState(series = series), onAction = {})
                }
            }
        }

        val bounds = composeRule.onAllNodesWithTag("series_card").fetchSemanticsNodes().map { it.boundsInRoot }
        assertThat(bounds).hasSize(3)
        assertThat(bounds[1].top).isEqualTo(bounds[0].top)
        assertThat(bounds[2].top).isEqualTo(bounds[0].top)
    }
}

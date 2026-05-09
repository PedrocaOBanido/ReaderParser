package com.opus.readerparser.ui.reader.manhwa

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.Chapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaReaderContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            MangaReaderContent(
                state = MangaReaderUiState(isLoading = true),
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            MangaReaderContent(
                state = MangaReaderUiState(error = "Failed to load pages"),
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Failed to load pages").assertIsDisplayed()
    }

    @Test
    fun pagesState_showsPager() {
        composeRule.setContent {
            MangaReaderContent(
                state = MangaReaderUiState(
                    pages = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
                    currentPage = 0,
                ),
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("pages_pager").assertIsDisplayed()
    }

    @Test
    fun openChapterList_dispatchesAction() {
        var actionDispatched = false
        composeRule.setContent {
            MangaReaderContent(
                state = MangaReaderUiState(
                    chapter = Chapter(
                        seriesUrl = "s",
                        sourceId = 1L,
                        url = "c1",
                        name = "Chapter 1",
                        number = 1f,
                    ),
                    pages = listOf("https://example.com/p1.jpg"),
                ),
                onAction = { action ->
                    if (action is MangaReaderAction.OpenChapterList) actionDispatched = true
                },
            )
        }
        composeRule.onNodeWithContentDescription("Chapter list").performClick()
        assert(actionDispatched)
    }
}

package com.opus.readerparser.ui.reader.novel

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
class NovelReaderContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            NovelReaderContent(
                state = NovelReaderUiState(isLoading = true),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            NovelReaderContent(
                state = NovelReaderUiState(error = "Chapter not found"),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter not found").assertIsDisplayed()
    }

    @Test
    fun contentState_showsWebView() {
        composeRule.setContent {
            NovelReaderContent(
                state = NovelReaderUiState(html = "<p>Hello world</p>"),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("novel_webview").assertIsDisplayed()
    }

    @Test
    fun nextChapterButton_dispatchesAction() {
        var actionDispatched = false
        composeRule.setContent {
            NovelReaderContent(
                state = NovelReaderUiState(
                    chapter = Chapter(
                        seriesUrl = "s",
                        sourceId = 1L,
                        url = "c1",
                        name = "Chapter 1",
                        number = 1f,
                    ),
                    hasNextChapter = true,
                    html = "<p>Content</p>",
                ),
                isDarkTheme = false,
                onAction = { action ->
                    if (action is NovelReaderAction.NextChapter) actionDispatched = true
                },
            )
        }
        composeRule.onNodeWithTag("loading").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Next chapter").performClick()
        assert(actionDispatched)
    }
}

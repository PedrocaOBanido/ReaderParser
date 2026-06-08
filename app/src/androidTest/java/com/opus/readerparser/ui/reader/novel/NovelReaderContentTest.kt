package com.opus.readerparser.ui.reader.novel

import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.testutil.FakeDownloadEnqueuer
import com.opus.readerparser.testutil.ReaderScreenTestChapterRepository
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
        composeRule.onAllNodesWithTag("loading").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Next chapter").performClick()
        assert(actionDispatched)
    }

    @Test
    fun chapterListButton_opensSheetAndNavigatesToSelectedChapter() {
        val currentChapter = Chapter(
            seriesUrl = "https://example.com/series/current",
            sourceId = 1L,
            url = "https://example.com/chapter/1",
            name = "Chapter 1",
            number = 1f,
        )
        val nextChapter = currentChapter.copy(
            url = "https://example.com/chapter/2",
            name = "Chapter 2",
            number = 2f,
        )
        val viewModel = NovelReaderViewModel(
            savedState = SavedStateHandle(
                mapOf(
                    "sourceId" to currentChapter.sourceId,
                    "seriesUrl" to currentChapter.seriesUrl,
                    "chapterUrl" to currentChapter.url,
                ),
            ),
            chapterRepository = ReaderScreenTestChapterRepository(
                chapters = listOf(
                    ChapterWithState(currentChapter, read = false, downloaded = false, progress = 0f),
                    ChapterWithState(nextChapter, read = false, downloaded = false, progress = 0f),
                ),
                content = ChapterContent.Text("<p>Content</p>"),
            ),
            downloadEnqueuer = FakeDownloadEnqueuer(),
        )
        var navigatedChapter: Chapter? = null

        composeRule.setContent {
            NovelReaderScreen(
                onBack = {},
                onNavigateToChapter = { navigatedChapter = it },
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithContentDescription("Chapter list").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reader_chapter_list").assertIsDisplayed()
        composeRule.onNodeWithText(currentChapter.name).assertIsDisplayed()
        composeRule.onNodeWithText(nextChapter.name).assertIsDisplayed()
        composeRule.onNodeWithText("Current").assertIsDisplayed()

        composeRule.onNodeWithTag("reader_chapter_item_1").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { navigatedChapter == nextChapter }
    }
}

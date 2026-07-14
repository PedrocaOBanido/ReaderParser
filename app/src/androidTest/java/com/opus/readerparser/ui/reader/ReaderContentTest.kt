package com.opus.readerparser.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.testutil.FakeCoilRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val fakeCoilRule = FakeCoilRule()

    private val chapter = Chapter(
        seriesUrl = "https://example.com/series/test",
        sourceId = 1L,
        url = "https://example.com/chapter/1",
        name = "Chapter 1",
        number = 1f,
    )

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(contentType = ContentType.NOVEL, isLoading = true),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(contentType = ContentType.NOVEL, error = "Failed to load"),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Failed to load").assertIsDisplayed()
    }

    @Test
    fun novelContent_showsWebView() {
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Hello world</p>",
                    chapter = chapter,
                ),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithTag("novel_webview").assertIsDisplayed()
    }

    @Test
    fun manhwaContent_showsPagesList() {
        composeRule.setContent {
            Box(Modifier.size(400.dp, 800.dp)) {
                ReaderContent(
                    state = ReaderUiState(
                        contentType = ContentType.MANHWA,
                        pages = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
                        chapter = chapter,
                    ),
                    isDarkTheme = false,
                    onAction = {},
                    imageLoader = fakeCoilRule.imageLoader,
                )
            }
        }
        composeRule.onNodeWithTag("pages_list").assertIsDisplayed()
    }

    @Test
    fun novelProgress_showsPercentage() {
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Content</p>",
                    chapter = chapter,
                    progress = 0.42f,
                ),
                isDarkTheme = false,
                onAction = {},
            )
        }
        composeRule.onNodeWithText("42%").assertIsDisplayed()
    }

    @Test
    fun manhwaProgress_showsPageCount() {
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.MANHWA,
                    pages = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
                    chapter = chapter,
                    currentPage = 1,
                ),
                isDarkTheme = false,
                onAction = {},
                imageLoader = fakeCoilRule.imageLoader,
            )
        }
        composeRule.onNodeWithText("2 / 2").assertIsDisplayed()
    }

    @Test
    fun nextChapterButton_dispatchesAction() {
        var dispatched = false
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Content</p>",
                    chapter = chapter,
                    hasNextChapter = true,
                ),
                isDarkTheme = false,
                onAction = { if (it is ReaderAction.NextChapter) dispatched = true },
            )
        }
        composeRule.onNodeWithContentDescription("Next chapter").performClick()
        assertThat(dispatched).isTrue()
    }

    @Test
    fun previousChapterButton_dispatchesAction() {
        var dispatched = false
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Content</p>",
                    chapter = chapter,
                    hasPreviousChapter = true,
                ),
                isDarkTheme = false,
                onAction = { if (it is ReaderAction.PreviousChapter) dispatched = true },
            )
        }
        composeRule.onNodeWithContentDescription("Previous chapter").performClick()
        assertThat(dispatched).isTrue()
    }

    @Test
    fun chapterListButton_dispatchesAction() {
        var dispatched = false
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Content</p>",
                    chapter = chapter,
                ),
                isDarkTheme = false,
                onAction = { if (it is ReaderAction.OpenChapterList) dispatched = true },
            )
        }
        composeRule.onNodeWithContentDescription("Chapter list").performClick()
        assertThat(dispatched).isTrue()
    }

    @Test
    fun downloadButton_dispatchesAction() {
        var dispatched = false
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    html = "<p>Content</p>",
                    chapter = chapter,
                ),
                isDarkTheme = false,
                onAction = { if (it is ReaderAction.DownloadChapter) dispatched = true },
            )
        }
        composeRule.onNodeWithContentDescription("Download chapter").performClick()
        assertThat(dispatched).isTrue()
    }

    @Test
    fun retryButton_dispatchesRetryAction() {
        var dispatched = false
        composeRule.setContent {
            ReaderContent(
                state = ReaderUiState(
                    contentType = ContentType.NOVEL,
                    error = "Failed to load",
                ),
                isDarkTheme = false,
                onAction = { if (it is ReaderAction.Retry) dispatched = true },
            )
        }
        composeRule.onNodeWithText("Retry").performClick()
        assertThat(dispatched).isTrue()
    }
}

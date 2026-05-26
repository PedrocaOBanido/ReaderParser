package com.opus.readerparser.ui.reader.manhwa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.testutil.FakeCoilRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MangaReaderContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val fakeCoilRule = FakeCoilRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            MangaReaderContent(
                state = MangaReaderUiState(isLoading = true),
                onAction = {},
                imageLoader = fakeCoilRule.imageLoader,
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
                imageLoader = fakeCoilRule.imageLoader,
            )
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Failed to load pages").assertIsDisplayed()
    }

    @Test
    fun pagesState_showsContinuousPageList_withManyPages() {
        val pageCount = 20
        composeRule.setContent {
            Box(Modifier.size(400.dp, 800.dp)) {
                MangaReaderContent(
                    state = MangaReaderUiState(
                        pages = List(pageCount) { index -> "https://example.com/p${index + 1}.jpg" },
                        currentPage = 0,
                    ),
                    onAction = {},
                    imageLoader = fakeCoilRule.imageLoader,
                )
            }
        }
        composeRule.onNodeWithTag("pages_list").assertIsDisplayed()
        // Page 1 is initially visible (at index 0)
        composeRule.onNodeWithContentDescription("Page 1").assertIsDisplayed()
        // Scroll to middle and end to prove each page is actually composed
        composeRule.onNodeWithTag("pages_list").performScrollToIndex(9)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Page 10").assertIsDisplayed()
        composeRule.onNodeWithTag("pages_list").performScrollToIndex(pageCount - 1)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Page $pageCount").assertIsDisplayed()
    }

    @Test
    fun pageIndicator_showsCurrentPageFromState() {
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
                    pages = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
                    currentPage = 1,
                ),
                onAction = {},
                imageLoader = fakeCoilRule.imageLoader,
            )
        }

        composeRule.onNodeWithText("2 / 2").assertIsDisplayed()
    }

    @Test
    fun endOfChapter_dispatchesSetPageWithLastIndex_whenScrolledToEnd() {
        val actions = mutableListOf<MangaReaderAction>()
        val pageCount = 10
        val lastIndex = pageCount - 1

        composeRule.setContent {
            Box(Modifier.size(400.dp, 800.dp)) {
                MangaReaderContent(
                    state = MangaReaderUiState(
                        pages = List(pageCount) { index -> "https://example.com/p${index + 1}.jpg" },
                        currentPage = 0,
                    ),
                    onAction = actions::add,
                    imageLoader = fakeCoilRule.imageLoader,
                )
            }
        }

        composeRule.onNodeWithTag("pages_list").performScrollToIndex(lastIndex)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            actions.any { it is MangaReaderAction.SetPage && it.page == lastIndex }
        }
    }

    @Test
    fun incrementalScroll_dispatchesSetPageWithIntermediateIndex() {
        val actions = mutableListOf<MangaReaderAction>()
        val pageCount = 10
        val middleIndex = pageCount / 2

        composeRule.setContent {
            Box(Modifier.size(400.dp, 800.dp)) {
                MangaReaderContent(
                    state = MangaReaderUiState(
                        pages = List(pageCount) { index -> "https://example.com/p${index + 1}.jpg" },
                        currentPage = 0,
                    ),
                    onAction = actions::add,
                    imageLoader = fakeCoilRule.imageLoader,
                )
            }
        }

        composeRule.onNodeWithTag("pages_list").performScrollToIndex(middleIndex)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            actions.any { it is MangaReaderAction.SetPage && it.page == middleIndex }
        }
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
                imageLoader = fakeCoilRule.imageLoader,
                onAction = { action ->
                    if (action is MangaReaderAction.OpenChapterList) actionDispatched = true
                },
            )
        }
        composeRule.onNodeWithContentDescription("Chapter list").performClick()
        assert(actionDispatched)
    }
}

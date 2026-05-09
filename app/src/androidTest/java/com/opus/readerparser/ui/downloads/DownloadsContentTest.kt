package com.opus.readerparser.ui.downloads

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            DownloadsContent(state = DownloadsUiState(isLoading = true), onAction = {})
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        composeRule.setContent {
            DownloadsContent(state = DownloadsUiState(error = "Load error"), onAction = {})
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Load error").assertIsDisplayed()
    }

    @Test
    fun populatedState_showsDownloadsList() {
        val items = listOf(
            DownloadItem(
                sourceId = 1L,
                chapterUrl = "c1",
                chapterName = "Chapter 1",
                seriesTitle = "Solo Leveling",
                state = DownloadState.COMPLETED,
                progress = 1f,
            ),
        )
        composeRule.setContent {
            DownloadsContent(state = DownloadsUiState(downloads = items), onAction = {})
        }
        composeRule.onNodeWithTag("downloads_list").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter 1").assertIsDisplayed()
        composeRule.onNodeWithText("Solo Leveling").assertIsDisplayed()
    }

    @Test
    fun retryButton_dispatchesRetryAction() {
        val item = DownloadItem(
            sourceId = 1L,
            chapterUrl = "c1",
            chapterName = "Chapter 1",
            seriesTitle = "Solo Leveling",
            state = DownloadState.FAILED,
            progress = 0f,
        )
        var retried = false
        composeRule.setContent {
            DownloadsContent(
                state = DownloadsUiState(downloads = listOf(item)),
                onAction = { action ->
                    if (action is DownloadsAction.Retry) retried = true
                },
            )
        }
        composeRule.onNodeWithText("Retry").performClick()
        assert(retried)
    }
}

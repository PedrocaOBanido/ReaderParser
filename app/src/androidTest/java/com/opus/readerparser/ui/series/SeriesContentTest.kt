package com.opus.readerparser.ui.series

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeriesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingState_showsProgressIndicator() {
        composeRule.setContent {
            SeriesContent(state = SeriesUiState(isLoading = true), onAction = {})
        }
        composeRule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessageAndRetry() {
        composeRule.setContent {
            SeriesContent(state = SeriesUiState(error = "Load failed"), onAction = {})
        }
        composeRule.onNodeWithTag("error_message").assertIsDisplayed()
        composeRule.onNodeWithText("Load failed").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun populatedState_showsChapterList() {
        val series = Series(
            sourceId = 1L,
            url = "https://example.com/s",
            title = "Test Series",
            type = ContentType.MANHWA,
        )
        val chapters = listOf(
            ChapterWithState(
                chapter = Chapter(seriesUrl = series.url, sourceId = 1L, url = "c1", name = "Chapter 1", number = 1f),
                read = false,
                downloaded = false,
                progress = 0f,
            ),
        )
        composeRule.setContent {
            SeriesContent(state = SeriesUiState(series = series, chapters = chapters), onAction = {})
        }
        composeRule.onNodeWithTag("chapter_list").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter 1").assertIsDisplayed()
    }

    @Test
    fun clickChapter_dispatchesOpenChapterAction() {
        val series = Series(
            sourceId = 1L,
            url = "https://example.com/s",
            title = "Test Series",
            type = ContentType.MANHWA,
        )
        val chapter = Chapter(seriesUrl = series.url, sourceId = 1L, url = "c1", name = "Chapter 42", number = 42f)
        var openedChapter: Chapter? = null
        composeRule.setContent {
            SeriesContent(
                state = SeriesUiState(
                    series = series,
                    chapters = listOf(
                        ChapterWithState(chapter = chapter, read = false, downloaded = false, progress = 0f),
                    ),
                ),
                onAction = { action ->
                    if (action is SeriesAction.OpenChapter) openedChapter = action.chapter
                },
            )
        }
        composeRule.onNodeWithText("Chapter 42").performClick()
        assert(openedChapter == chapter)
    }
}

package com.opus.readerparser.ui.reader.novel

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.ui.components.ReaderChapterListSheet

@Composable
fun NovelReaderScreen(
    onBack: () -> Unit,
    onNavigateToChapter: (Chapter) -> Unit = {},
    viewModel: NovelReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDarkTheme = isSystemInDarkTheme()
    var showChapterList by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NovelReaderEffect.NavigateToChapter -> onNavigateToChapter(effect.chapter)
                is NovelReaderEffect.ShowChapterList -> showChapterList = true
                is NovelReaderEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    NovelReaderContent(
        state = state,
        isDarkTheme = isDarkTheme,
        onAction = viewModel::onAction,
    )

    if (showChapterList) {
        ReaderChapterListSheet(
            chapters = state.seriesChapters,
            currentChapterUrl = state.chapter?.url,
            onChapterSelected = { chapter ->
                showChapterList = false
                viewModel.onAction(NovelReaderAction.SelectChapter(chapter))
            },
            onDismissRequest = { showChapterList = false },
        )
    }
}

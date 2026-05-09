package com.opus.readerparser.ui.reader.novel

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Chapter

@Composable
fun NovelReaderScreen(
    onBack: () -> Unit,
    onNavigateToChapter: (Chapter) -> Unit = {},
    viewModel: NovelReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NovelReaderEffect.NavigateToChapter -> onNavigateToChapter(effect.chapter)
                is NovelReaderEffect.ShowChapterList -> {
                    // TODO: show chapter list bottom sheet
                }
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
}

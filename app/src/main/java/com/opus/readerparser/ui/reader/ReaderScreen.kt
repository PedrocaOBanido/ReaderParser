package com.opus.readerparser.ui.reader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.ui.components.ReaderChapterListSheet
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onNavigateToChapter: (Chapter) -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDarkTheme = isSystemInDarkTheme()
    var showChapterList by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReaderEffect.NavigateToChapter -> onNavigateToChapter(effect.chapter)
                is ReaderEffect.ShowChapterList -> showChapterList = true
                is ReaderEffect.ShowError -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is ReaderEffect.ShowSnackbar -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    ReaderContent(
        state = state,
        isDarkTheme = isDarkTheme,
        onAction = viewModel::onAction,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    )

    if (showChapterList) {
        ReaderChapterListSheet(
            chapters = state.seriesChapters,
            currentChapterUrl = state.chapter?.url,
            onChapterSelected = { chapter ->
                showChapterList = false
                viewModel.onAction(ReaderAction.SelectChapter(chapter))
            },
            onDismissRequest = { showChapterList = false },
        )
    }
}

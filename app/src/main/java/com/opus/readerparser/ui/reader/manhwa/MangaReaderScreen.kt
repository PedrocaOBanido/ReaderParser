package com.opus.readerparser.ui.reader.manhwa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Chapter

@Composable
fun MangaReaderScreen(
    onBack: () -> Unit,
    onNavigateToChapter: (Chapter) -> Unit = {},
    viewModel: MangaReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MangaReaderEffect.NavigateToChapter -> onNavigateToChapter(effect.chapter)
                is MangaReaderEffect.ShowChapterList -> {
                    // TODO: show chapter list bottom sheet
                }
                is MangaReaderEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    MangaReaderContent(state = state, onAction = viewModel::onAction)
}

package com.opus.readerparser.ui.reader.manhwa

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
fun MangaReaderScreen(
    onBack: () -> Unit,
    onNavigateToChapter: (Chapter) -> Unit = {},
    viewModel: MangaReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChapterList by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MangaReaderEffect.NavigateToChapter -> onNavigateToChapter(effect.chapter)
                is MangaReaderEffect.ShowChapterList -> showChapterList = true
                is MangaReaderEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    MangaReaderContent(state = state, onAction = viewModel::onAction)

    if (showChapterList) {
        ReaderChapterListSheet(
            chapters = state.seriesChapters,
            currentChapterUrl = state.chapter?.url,
            onChapterSelected = { chapter ->
                showChapterList = false
                viewModel.onAction(MangaReaderAction.SelectChapter(chapter))
            },
            onDismissRequest = { showChapterList = false },
        )
    }
}

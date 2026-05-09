package com.opus.readerparser.ui.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.ContentType

@Composable
fun SeriesScreen(
    onNavigateToNovelReader: (sourceId: Long, seriesUrl: String, chapterUrl: String) -> Unit,
    onNavigateToMangaReader: (sourceId: Long, seriesUrl: String, chapterUrl: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SeriesEffect.NavigateToReader -> {
                    val chapter = effect.chapter
                    when (effect.type) {
                        ContentType.NOVEL -> onNavigateToNovelReader(
                            chapter.sourceId,
                            chapter.seriesUrl,
                            chapter.url,
                        )
                        ContentType.MANHWA -> onNavigateToMangaReader(
                            chapter.sourceId,
                            chapter.seriesUrl,
                            chapter.url,
                        )
                    }
                }
                is SeriesEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    SeriesContent(state = state, onAction = viewModel::onAction)
}

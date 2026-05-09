package com.opus.readerparser.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Series

@Composable
fun LibraryScreen(
    onNavigateToSeries: (Series) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.NavigateToSeries -> onNavigateToSeries(effect.series)
                is LibraryEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    LibraryContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToSettings = onNavigateToSettings,
    )
}

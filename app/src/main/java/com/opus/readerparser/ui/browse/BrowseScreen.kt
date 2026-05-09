package com.opus.readerparser.ui.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opus.readerparser.domain.model.Series

@Composable
fun BrowseScreen(
    onNavigateToSeries: (Series) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BrowseEffect.NavigateToSeries -> onNavigateToSeries(effect.series)
                is BrowseEffect.ShowError -> {
                    // TODO: show snackbar with effect.message
                }
            }
        }
    }

    BrowseContent(state = state, onAction = viewModel::onAction)
}

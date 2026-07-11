package com.opus.readerparser.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.ui.components.SeriesCatalogGrid
import com.opus.readerparser.ui.theme.ReaderParserTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default") },
                                onClick = {
                                    onAction(LibraryAction.SetSortBy(LibrarySortBy.DEFAULT))
                                    showSortMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Title") },
                                onClick = {
                                    onAction(LibraryAction.SetSortBy(LibrarySortBy.TITLE))
                                    showSortMenu = false
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(LibraryAction.SetSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("search_field"),
                placeholder = { Text("Search library…") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onAction(LibraryAction.SetSearchQuery("")) }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onAction(LibraryAction.SetSearchQuery(state.searchQuery)) },
                ),
                colors = OutlinedTextFieldDefaults.colors(),
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("loading"),
                        )
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.error,
                                modifier = Modifier.testTag("error_message"),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    state.series.isEmpty() -> {
                        Text(
                            text = if (state.searchQuery.isNotEmpty())
                                "No series match \"${state.searchQuery}\"."
                            else
                                "Your library is empty.\nBrowse sources to add series.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        SeriesCatalogGrid(
                            series = state.series,
                            onSeriesClick = { series -> onAction(LibraryAction.OpenSeries(series)) },
                            onSeriesLongClick = { series -> onAction(LibraryAction.RemoveFromLibrary(series)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentLoadingPreview() {
    ReaderParserTheme {
        LibraryContent(
            state = LibraryUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentErrorPreview() {
    ReaderParserTheme {
        LibraryContent(
            state = LibraryUiState(error = "Failed to load library"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentPopulatedPreview() {
    val sample = listOf(
        Series(sourceId = 1L, url = "https://example.com/s1", title = "The Wandering Inn", type = ContentType.NOVEL),
        Series(sourceId = 1L, url = "https://example.com/s2", title = "Solo Leveling", type = ContentType.MANHWA),
    )
    ReaderParserTheme {
        LibraryContent(
            state = LibraryUiState(series = sample),
            onAction = {},
        )
    }
}

package com.opus.readerparser.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.ui.theme.ReaderParserTheme

private val GridContentPadding = PaddingValues(8.dp)
private val GridItemSpacing = 8.dp

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
                    FilterChip(
                        selected = state.filterUnreadOnly,
                        onClick = { onAction(LibraryAction.SetFilterUnreadOnly(!state.filterUnreadOnly)) },
                        label = { Text("Unread") },
                        leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null) },
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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
                        text = "Your library is empty.\nBrowse sources to add series.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = GridContentPadding,
                        horizontalArrangement = Arrangement.spacedBy(GridItemSpacing),
                        verticalArrangement = Arrangement.spacedBy(GridItemSpacing),
                        modifier = Modifier.testTag("series_list"),
                    ) {
                        items(state.series, key = { "${it.sourceId}|${it.url}" }) { series ->
                            LibrarySeriesCard(
                                series = series,
                                onClick = { onAction(LibraryAction.OpenSeries(series)) },
                                onLongClick = { onAction(LibraryAction.RemoveFromLibrary(series)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySeriesCard(
    series: Series,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column {
            AsyncImage(
                model = series.coverUrl,
                contentDescription = series.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(Color.LightGray),
                error = ColorPainter(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f),
            )
            Text(
                text = series.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
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

package com.opus.readerparser.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.opus.readerparser.domain.model.SourceInfo
import com.opus.readerparser.ui.theme.ReaderParserTheme

private val GridContentPadding = PaddingValues(8.dp)
private val GridItemSpacing = 8.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseContent(
    state: BrowseUiState,
    onAction: (BrowseAction) -> Unit,
) {
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    val selectedSource = state.sources.find { it.id == state.selectedSourceId }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Browse") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Source selector
            if (state.sources.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = sourceMenuExpanded,
                    onExpandedChange = { sourceMenuExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    OutlinedTextField(
                        value = selectedSource?.name ?: "Select a source",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = sourceMenuExpanded,
                        onDismissRequest = { sourceMenuExpanded = false },
                    ) {
                        state.sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text("${source.name} (${source.lang})") },
                                onClick = {
                                    onAction(BrowseAction.SelectSource(source.id))
                                    sourceMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Mode tabs
            val tabs = listOf(BrowseMode.POPULAR, BrowseMode.LATEST, BrowseMode.SEARCH)
            TabRow(selectedTabIndex = tabs.indexOf(state.mode)) {
                tabs.forEach { mode ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { onAction(BrowseAction.SetMode(mode)) },
                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // Search bar
            if (state.mode == BrowseMode.SEARCH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { onAction(BrowseAction.SetSearchQuery(it)) },
                        label = { Text("Search") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    IconButton(onClick = { onAction(BrowseAction.Search) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.series.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("loading"),
                        )
                    }
                    state.error != null && state.series.isEmpty() -> {
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
                            TextButton(onClick = { /* retry would re-trigger current mode */ }) {
                                Text("Retry")
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(GridItemSpacing),
                            horizontalArrangement = Arrangement.spacedBy(GridItemSpacing),
                            verticalArrangement = Arrangement.spacedBy(GridItemSpacing),
                            modifier = Modifier.testTag("series_list"),
                        ) {
                            items(state.series, key = { "${it.sourceId}|${it.url}" }) { series ->
                                BrowseSeriesCard(
                                    series = series,
                                    onClick = { onAction(BrowseAction.OpenSeries(series)) },
                                )
                            }
                            if (state.hasNextPage) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Button(onClick = { onAction(BrowseAction.LoadMore) }) {
                                            Text("Load more")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseSeriesCard(
    series: Series,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
private fun BrowseContentLoadingPreview() {
    ReaderParserTheme {
        BrowseContent(
            state = BrowseUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowseContentErrorPreview() {
    ReaderParserTheme {
        BrowseContent(
            state = BrowseUiState(error = "Network error"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowseContentPopulatedPreview() {
    val sources = listOf(SourceInfo(id = 1L, name = "AsuraScans", lang = "en", type = ContentType.MANHWA))
    val series = listOf(
        Series(sourceId = 1L, url = "https://example.com/s1", title = "Reaper of the Drifting Moon", type = ContentType.MANHWA),
        Series(sourceId = 1L, url = "https://example.com/s2", title = "Eleceed", type = ContentType.MANHWA),
    )
    ReaderParserTheme {
        BrowseContent(
            state = BrowseUiState(sources = sources, selectedSourceId = 1L, series = series),
            onAction = {},
        )
    }
}

package com.opus.readerparser.ui.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterWithState
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.ui.theme.ReaderParserTheme

private val CoverWidth = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesContent(
    state: SeriesUiState,
    onAction: (SeriesAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var descriptionExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.series?.title ?: "") },
                actions = {
                    IconButton(onClick = { onAction(SeriesAction.Refresh) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.series != null) {
                Column(horizontalAlignment = Alignment.End) {
                    // Range download FAB (only when chapters are loaded)
                    if (state.chapters.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { onAction(SeriesAction.ShowRangePicker) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download range",
                            )
                        }
                    }
                    // Library toggle FAB (always visible when series is loaded)
                    FloatingActionButton(
                        onClick = { onAction(SeriesAction.ToggleLibrary(!state.inLibrary)) },
                    ) {
                        Icon(
                            imageVector = if (state.inLibrary) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (state.inLibrary) "Remove from library" else "Add to library",
                        )
                    }
                }
            }
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
                        TextButton(onClick = { onAction(SeriesAction.Refresh) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chapter_list"),
                    ) {
                        // Header
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    AsyncImage(
                                        model = state.series?.coverUrl,
                                        contentDescription = state.series?.title,
                                        contentScale = ContentScale.Crop,
                                        placeholder = ColorPainter(Color.LightGray),
                                        error = ColorPainter(Color.Gray),
                                        modifier = Modifier
                                            .width(CoverWidth)
                                            .aspectRatio(2f / 3f),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.series?.title ?: "",
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        if (state.series?.author != null) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = state.series.author,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = state.series?.status?.name?.lowercase()
                                                ?.replaceFirstChar { it.uppercase() } ?: "",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }

                                if (state.series?.description != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = state.series.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    TextButton(
                                        onClick = { descriptionExpanded = !descriptionExpanded },
                                    ) {
                                        Text(if (descriptionExpanded) "Show less" else "Show more")
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${state.chapters.size} chapters",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            }
                        }

                        // Download unread button
                        item {
                            TextButton(
                                onClick = { onAction(SeriesAction.DownloadUnread) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text("Download unread")
                            }
                        }

                        // Chapter list
                        items(state.chapters, key = { "${it.chapter.sourceId}|${it.chapter.url}" }) { cws ->
                            ChapterRow(
                                chapterWithState = cws,
                                onClick = { onAction(SeriesAction.OpenChapter(cws.chapter)) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Range picker dialog
    if (state.showRangePicker) {
        RangePickerDialog(
            chapterCount = state.chapters.size,
            onConfirm = { start, end ->
                onAction(SeriesAction.DownloadRange(start, end))
            },
            onDismiss = { onAction(SeriesAction.DismissRangePicker) },
        )
    }
}

@Composable
private fun RangePickerDialog(
    chapterCount: Int,
    onConfirm: (startIndex: Int, endIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Chapters are displayed in descending order in the list, but the range
    // picker works with ascending chapter numbers. We present 1-based indices
    // to the user and convert back to 0-based.
    var startIndex by remember { mutableIntStateOf(0) }
    var endIndex by remember { mutableIntStateOf((chapterCount - 1).coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download range") },
        text = {
            Column {
                Text(
                    text = "Select chapter range (1–$chapterCount)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("From", style = MaterialTheme.typography.labelMedium)
                        androidx.compose.material3.OutlinedTextField(
                            value = (startIndex + 1).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { idx ->
                                    startIndex = (idx - 1).coerceIn(0, chapterCount - 1)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To", style = MaterialTheme.typography.labelMedium)
                        androidx.compose.material3.OutlinedTextField(
                            value = (endIndex + 1).toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { idx ->
                                    endIndex = (idx - 1).coerceIn(0, chapterCount - 1)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(startIndex, endIndex) }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterRow(
    chapterWithState: ChapterWithState,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapterWithState.chapter.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (chapterWithState.read) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (chapterWithState.downloaded) {
                    Text(
                        text = "Downloaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesContentLoadingPreview() {
    ReaderParserTheme {
        SeriesContent(
            state = SeriesUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesContentErrorPreview() {
    ReaderParserTheme {
        SeriesContent(
            state = SeriesUiState(error = "Failed to load series"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesContentPopulatedPreview() {
    val series = Series(
        sourceId = 1L,
        url = "https://example.com/series",
        title = "Solo Leveling",
        author = "Chu-Gong",
        description = "In this world where Hunters with various special powers battle monsters from portals...",
        type = ContentType.MANHWA,
    )
    val chapters = listOf(
        ChapterWithState(
            chapter = Chapter(seriesUrl = series.url, sourceId = 1L, url = "c1", name = "Chapter 1", number = 1f),
            read = true,
            downloaded = false,
            progress = 1f,
        ),
        ChapterWithState(
            chapter = Chapter(seriesUrl = series.url, sourceId = 1L, url = "c2", name = "Chapter 2", number = 2f),
            read = false,
            downloaded = true,
            progress = 0f,
        ),
    )
    ReaderParserTheme {
        SeriesContent(
            state = SeriesUiState(series = series, chapters = chapters, inLibrary = true),
            onAction = {},
        )
    }
}

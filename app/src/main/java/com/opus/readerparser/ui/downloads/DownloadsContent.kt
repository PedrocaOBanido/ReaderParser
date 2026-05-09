package com.opus.readerparser.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opus.readerparser.domain.model.DownloadItem
import com.opus.readerparser.domain.model.DownloadState
import com.opus.readerparser.ui.theme.ReaderParserTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onAction: (DownloadsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Downloads") })
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
                state.downloads.isEmpty() -> {
                    Text(
                        text = "No downloads yet.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("downloads_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            state.downloads,
                            key = { "${it.sourceId}|${it.chapterUrl}" },
                        ) { item ->
                            DownloadItemRow(
                                item = item,
                                onCancel = { onAction(DownloadsAction.Cancel(item.sourceId, item.chapterUrl)) },
                                onRetry = { onAction(DownloadsAction.Retry(item.sourceId, item.chapterUrl)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemRow(
    item: DownloadItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.seriesTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.chapterName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (item.state == DownloadState.RUNNING) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StateBadge(state = item.state)

                Row {
                    when (item.state) {
                        DownloadState.QUEUED, DownloadState.RUNNING -> {
                            TextButton(onClick = onCancel) {
                                Text("Cancel")
                            }
                        }
                        DownloadState.FAILED -> {
                            OutlinedButton(onClick = onRetry) {
                                Text("Retry")
                            }
                        }
                        DownloadState.COMPLETED -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun StateBadge(state: DownloadState) {
    val label = when (state) {
        DownloadState.QUEUED -> "Queued"
        DownloadState.RUNNING -> "Downloading"
        DownloadState.COMPLETED -> "Done"
        DownloadState.FAILED -> "Failed"
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Preview(showBackground = true)
@Composable
private fun DownloadsContentLoadingPreview() {
    ReaderParserTheme {
        DownloadsContent(
            state = DownloadsUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsContentErrorPreview() {
    ReaderParserTheme {
        DownloadsContent(
            state = DownloadsUiState(error = "Failed to load downloads"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadsContentPopulatedPreview() {
    val items = listOf(
        DownloadItem(
            sourceId = 1L,
            chapterUrl = "c1",
            chapterName = "Chapter 1",
            seriesTitle = "Solo Leveling",
            state = DownloadState.COMPLETED,
            progress = 1f,
        ),
        DownloadItem(
            sourceId = 1L,
            chapterUrl = "c2",
            chapterName = "Chapter 2",
            seriesTitle = "Solo Leveling",
            state = DownloadState.RUNNING,
            progress = 0.45f,
        ),
        DownloadItem(
            sourceId = 1L,
            chapterUrl = "c3",
            chapterName = "Chapter 3",
            seriesTitle = "Solo Leveling",
            state = DownloadState.FAILED,
            progress = 0f,
        ),
    )
    ReaderParserTheme {
        DownloadsContent(
            state = DownloadsUiState(downloads = items),
            onAction = {},
        )
    }
}

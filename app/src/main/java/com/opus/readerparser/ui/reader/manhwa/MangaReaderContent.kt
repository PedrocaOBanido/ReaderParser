package com.opus.readerparser.ui.reader.manhwa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.opus.readerparser.ui.theme.ReaderParserTheme
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderContent(
    state: MangaReaderUiState,
    onAction: (MangaReaderAction) -> Unit,
    imageLoader: ImageLoader? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.chapter?.name ?: "",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onAction(MangaReaderAction.PreviousChapter) },
                        enabled = state.hasPreviousChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                    }
                },
                actions = {
                    Text(
                        text = if (state.pages.isNotEmpty()) {
                            "${state.currentPage + 1} / ${state.pages.size}"
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(
                        onClick = { onAction(MangaReaderAction.NextChapter) },
                        enabled = state.hasNextChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                    }
                    IconButton(onClick = { onAction(MangaReaderAction.OpenChapterList) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapter list")
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
                        TextButton(onClick = { /* retry would re-trigger Load action */ }) {
                            Text("Retry")
                        }
                    }
                }
                state.pages.isNotEmpty() -> {
                    key(state.chapter?.url) {
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = state.currentPage.coerceIn(0, state.pages.lastIndex),
                        )

                        LaunchedEffect(listState) {
                            snapshotFlow {
                                val layoutInfo = listState.layoutInfo
                                val items = layoutInfo.visibleItemsInfo
                                if (items.isEmpty()) return@snapshotFlow null
                                val viewportCenter =
                                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                                items.minBy {
                                    abs(it.offset + it.size / 2 - viewportCenter)
                                }.index
                            }
                                .filterNotNull()
                                .distinctUntilChanged()
                                .collect { page -> onAction(MangaReaderAction.SetPage(page)) }
                        }

                        val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                        val errorPainter = ColorPainter(MaterialTheme.colorScheme.errorContainer)

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pages_list"),
                        ) {
                            itemsIndexed(
                                items = state.pages,
                                key = { index, pageUrl -> "$index-$pageUrl" },
                            ) { index, pageUrl ->
                                AsyncImage(
                                    model = pageUrl,
                                    contentDescription = "Page ${index + 1}",
                                    imageLoader = imageLoader
                                        ?: SingletonImageLoader.get(LocalPlatformContext.current),
                                    contentScale = ContentScale.FillWidth,
                                    placeholder = placeholderPainter,
                                    error = errorPainter,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "No pages available",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MangaReaderContentLoadingPreview() {
    ReaderParserTheme {
        MangaReaderContent(
            state = MangaReaderUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MangaReaderContentErrorPreview() {
    ReaderParserTheme {
        MangaReaderContent(
            state = MangaReaderUiState(error = "Failed to load chapter"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MangaReaderContentWithPagesPreview() {
    ReaderParserTheme {
        MangaReaderContent(
            state = MangaReaderUiState(
                pages = listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"),
                currentPage = 0,
                hasNextChapter = true,
            ),
            onAction = {},
        )
    }
}

package com.opus.readerparser.ui.reader.manhwa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.opus.readerparser.ui.theme.ReaderParserTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderContent(
    state: MangaReaderUiState,
    onAction: (MangaReaderAction) -> Unit,
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
                    val pagerState = rememberPagerState(
                        initialPage = state.currentPage,
                        pageCount = { state.pages.size },
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            onAction(MangaReaderAction.SetPage(page))
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("pages_pager"),
                    ) { page ->
                        AsyncImage(
                            model = state.pages[page],
                            contentDescription = "Page ${page + 1}",
                            contentScale = ContentScale.FillWidth,
                            placeholder = ColorPainter(Color.LightGray),
                            error = ColorPainter(Color.Gray),
                            modifier = Modifier.fillMaxWidth(),
                        )
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

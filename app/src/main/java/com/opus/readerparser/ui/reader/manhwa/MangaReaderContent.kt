package com.opus.readerparser.ui.reader.manhwa

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.State.Success
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.ui.theme.ReaderParserTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import coil3.request.SuccessResult
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaReaderContent(
    state: MangaReaderUiState,
    onAction: (MangaReaderAction) -> Unit,
    imageLoader: ImageLoader? = null,
) {
    var showControls by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { showControls = !showControls })
                },
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

            // Top Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = state.chapter?.name ?: "",
                            maxLines = 1,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
                )
            }

            // Bottom Bar Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    IconButton(
                        onClick = { onAction(MangaReaderAction.PreviousChapter) },
                        enabled = state.hasPreviousChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = if (state.pages.isNotEmpty()) {
                            "${state.currentPage + 1} / ${state.pages.size}"
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { onAction(MangaReaderAction.NextChapter) },
                        enabled = state.hasNextChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                    }
                    IconButton(onClick = { onAction(MangaReaderAction.OpenChapterList) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapter list")
                    }
                }
            }

            // Floating Page Counter when controls are hidden
            AnimatedVisibility(
                visible = !showControls && state.pages.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .navigationBarsPadding(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "${state.currentPage + 1} / ${state.pages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun MangaReaderPreview(content: @Composable () -> Unit) {
    val previewBitmaps = remember {
        listOf(
            Color(0xFFE9D5FF),
            Color(0xFFBFDBFE),
            Color(0xFFC7F9CC),
            Color(0xFFFDE68A),
            Color(0xFFFBCFE8),
        ).mapIndexed { index, color ->
            Bitmap.createBitmap(1200, 1600 + (index * 160), Bitmap.Config.ARGB_8888).apply {
                eraseColor(color.toArgb())
            }
        }
    }
    val previewHandler = remember(previewBitmaps) {
        AsyncImagePreviewHandler { _, request ->
            val image = previewBitmaps[abs(request.data.toString().hashCode()) % previewBitmaps.size].asImage()
            Success(image.asPainter(request.context), SuccessResult(image, request))
        }
    }

    ReaderParserTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            content()
        }
    }
}

private fun previewChapter(name: String) = Chapter(
    seriesUrl = "https://preview.example/series/the-artist-returns",
    sourceId = 1L,
    url = "https://preview.example/chapter/${name.hashCode()}",
    name = name,
    number = 126f,
)

@Preview(showBackground = true)
@Composable
private fun MangaReaderContentLoadingPreview() {
    MangaReaderPreview {
        MangaReaderContent(
            state = MangaReaderUiState(
                chapter = previewChapter("Chapter 126"),
                isLoading = true,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MangaReaderContentErrorPreview() {
    MangaReaderPreview {
        MangaReaderContent(
            state = MangaReaderUiState(
                chapter = previewChapter("Chapter 126"),
                error = "Failed to load chapter",
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MangaReaderContentWithPagesPreview() {
    MangaReaderPreview {
        MangaReaderContent(
            state = MangaReaderUiState(
                chapter = previewChapter("Chapter 126: The Duel at Dawn"),
                pages = List(6) { index -> "preview://page/${index + 1}" },
                currentPage = 2,
                hasPreviousChapter = true,
                hasNextChapter = true,
            ),
            onAction = {},
        )
    }
}

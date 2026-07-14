package com.opus.readerparser.ui.reader

import android.webkit.WebView
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.ui.theme.BackgroundDark
import com.opus.readerparser.ui.theme.BackgroundLight
import com.opus.readerparser.ui.theme.OnBackgroundDark
import com.opus.readerparser.ui.theme.OnBackgroundLight
import com.opus.readerparser.ui.theme.PrimaryDark
import com.opus.readerparser.ui.theme.PrimaryLight
import com.opus.readerparser.ui.theme.ReaderParserTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.abs

private fun Color.toHexString(): String =
    String.format("#%06X", this.toArgb() and 0xFFFFFF)

private fun buildNovelHtml(html: String, isDarkTheme: Boolean): String {
    val bgColor = if (isDarkTheme) BackgroundDark.toHexString() else BackgroundLight.toHexString()
    val textColor = if (isDarkTheme) OnBackgroundDark.toHexString() else OnBackgroundLight.toHexString()
    val linkColor = if (isDarkTheme) PrimaryDark.toHexString() else PrimaryLight.toHexString()

    return """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body {
                background-color: $bgColor;
                color: $textColor;
                font-family: Georgia, serif;
                font-size: 16px;
                line-height: 1.6;
                padding: 16px;
                margin: 0;
            }
            img { max-width: 100%; }
            a { color: $linkColor; }
        </style>
        </head>
        <body>$html</body>
        </html>
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderContent(
    state: ReaderUiState,
    isDarkTheme: Boolean,
    onAction: (ReaderAction) -> Unit,
    onBack: () -> Unit = {},
    imageLoader: ImageLoader? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showControls by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        TextButton(onClick = { onAction(ReaderAction.Retry) }) {
                            Text("Retry")
                        }
                    }
                }
                state.contentType == ContentType.NOVEL && state.html.isNotEmpty() -> {
                    NovelWebView(
                        html = state.html,
                        isDarkTheme = isDarkTheme,
                        progress = state.progress,
                        onProgressChanged = { progress ->
                            onAction(ReaderAction.SetProgress(progress))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("novel_webview"),
                    )
                }
                state.contentType == ContentType.MANHWA && state.pages.isNotEmpty() -> {
                    key(state.chapter?.url) {
                        ManhwaPageList(
                            pages = state.pages,
                            currentPage = state.currentPage,
                            onPageChanged = { page -> onAction(ReaderAction.SetPage(page)) },
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                else -> {
                    Text(
                        text = "No content available",
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
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
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
                        onClick = { onAction(ReaderAction.PreviousChapter) },
                        enabled = state.hasPreviousChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = when (state.contentType) {
                            ContentType.NOVEL -> if (state.progress > 0f) "${(state.progress * 100).toInt()}%" else ""
                            ContentType.MANHWA -> if (state.pages.isNotEmpty()) "${state.currentPage + 1} / ${state.pages.size}" else ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { onAction(ReaderAction.NextChapter) },
                        enabled = state.hasNextChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                    }
                    IconButton(onClick = { onAction(ReaderAction.OpenChapterList) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapter list")
                    }
                    IconButton(onClick = { onAction(ReaderAction.DownloadChapter) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download chapter")
                    }
                }
            }

            // Floating progress indicator when controls are hidden
            AnimatedVisibility(
                visible = !showControls && (state.pages.isNotEmpty() || state.progress > 0f),
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
                        text = when (state.contentType) {
                            ContentType.NOVEL -> "${(state.progress * 100).toInt()}%"
                            ContentType.MANHWA -> "${state.currentPage + 1} / ${state.pages.size}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NovelWebView(
    html: String,
    isDarkTheme: Boolean,
    progress: Float,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val styledHtml = remember(html, isDarkTheme) { buildNovelHtml(html, isDarkTheme) }
    var lastLoadedHtml by remember { mutableStateOf("") }
    var pendingProgress by remember { mutableFloatStateOf(-1f) }
    var restoreProgress by remember { mutableFloatStateOf(-1f) }
    var progressDebounceJob by remember { mutableStateOf<Job?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = {
            WebView(context).apply {
                webViewRef = this
                settings.apply {
                    javaScriptEnabled = false
                    builtInZoomControls = false
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (restoreProgress > 0f && view != null) {
                            view.post {
                                val contentHeightPx = view.contentHeight * view.resources.displayMetrics.density
                                val viewHeightPx = view.height.toFloat()
                                val targetScroll = ((contentHeightPx - viewHeightPx).coerceAtLeast(0f) * restoreProgress).toInt()
                                view.scrollTo(0, targetScroll)
                                restoreProgress = -1f
                            }
                        }
                    }
                }
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    val contentHeightPx = contentHeight * resources.displayMetrics.density
                    val viewHeightPx = height.toFloat()
                    val maxScroll = (contentHeightPx - viewHeightPx).coerceAtLeast(0f)
                    val currentProgress = if (maxScroll > 0f) {
                        scrollY.toFloat() / maxScroll
                    } else {
                        0f
                    }
                    val clamped = currentProgress.coerceIn(0f, 1f)
                    pendingProgress = clamped
                    progressDebounceJob?.cancel()
                    progressDebounceJob = scope.launch {
                        delay(300)
                        onProgressChanged(clamped)
                    }
                }
            }
        },
        update = { webView ->
            if (styledHtml != lastLoadedHtml) {
                lastLoadedHtml = styledHtml
                restoreProgress = progress
                pendingProgress = -1f
                progressDebounceJob?.cancel()
                progressDebounceJob = null
                webView.loadDataWithBaseURL(
                    "https://app.local",
                    styledHtml,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            progressDebounceJob?.cancel()
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }
}

@Composable
private fun ManhwaPageList(
    pages: List<String>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    imageLoader: ImageLoader? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentPage.coerceIn(0, pages.lastIndex),
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
            .collect { page -> onPageChanged(page) }
    }

    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val errorPainter = ColorPainter(MaterialTheme.colorScheme.errorContainer)

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("pages_list"),
    ) {
        itemsIndexed(
            items = pages,
            key = { index, pageUrl -> "$index-$pageUrl" },
        ) { index, pageUrl ->
            AsyncImage(
                model = pageUrl,
                contentDescription = "Page ${index + 1}",
                imageLoader = imageLoader
                    ?: SingletonImageLoader.get(LocalPlatformContext.current),
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                placeholder = placeholderPainter,
                error = errorPainter,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderContentLoadingPreview() {
    ReaderParserTheme {
        ReaderContent(
            state = ReaderUiState(contentType = ContentType.NOVEL, isLoading = true),
            isDarkTheme = false,
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderContentErrorPreview() {
    ReaderParserTheme {
        ReaderContent(
            state = ReaderUiState(contentType = ContentType.MANHWA, error = "Failed to load chapter"),
            isDarkTheme = false,
            onAction = {},
        )
    }
}

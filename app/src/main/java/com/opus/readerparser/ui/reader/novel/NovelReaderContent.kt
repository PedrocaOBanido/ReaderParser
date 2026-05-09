package com.opus.readerparser.ui.reader.novel

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.opus.readerparser.ui.theme.BackgroundDark
import com.opus.readerparser.ui.theme.BackgroundLight
import com.opus.readerparser.ui.theme.OnBackgroundDark
import com.opus.readerparser.ui.theme.OnBackgroundLight
import com.opus.readerparser.ui.theme.PrimaryDark
import com.opus.readerparser.ui.theme.PrimaryLight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.opus.readerparser.ui.theme.ReaderParserTheme

private fun Color.toHexString(): String =
    String.format("#%06X", this.toArgb() and 0xFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelReaderContent(
    state: NovelReaderUiState,
    isDarkTheme: Boolean,
    onAction: (NovelReaderAction) -> Unit,
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
                        onClick = { onAction(NovelReaderAction.PreviousChapter) },
                        enabled = state.hasPreviousChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous chapter")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(NovelReaderAction.NextChapter) },
                        enabled = state.hasNextChapter,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next chapter")
                    }
                    IconButton(onClick = { onAction(NovelReaderAction.OpenChapterList) }) {
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
                        TextButton(onClick = { /* retry via Load action if chapter available */ }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    NovelWebView(
                        html = state.html,
                        isDarkTheme = isDarkTheme,
                        onProgressChanged = { progress ->
                            onAction(NovelReaderAction.SetProgress(progress))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("novel_webview"),
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
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bgColor = if (isDarkTheme) BackgroundDark.toHexString() else BackgroundLight.toHexString()
    val textColor = if (isDarkTheme) OnBackgroundDark.toHexString() else OnBackgroundLight.toHexString()
    val linkColor = if (isDarkTheme) PrimaryDark.toHexString() else PrimaryLight.toHexString()
    val styledHtml = remember(html, isDarkTheme) {
        """
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

    AndroidView(
        factory = {
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = false
                    builtInZoomControls = false
                }
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    val contentHeight = contentHeight * resources.displayMetrics.density
                    val viewHeight = height.toFloat()
                    val maxScroll = contentHeight - viewHeight
                    if (maxScroll > 0f) {
                        onProgressChanged((scrollY / maxScroll).coerceIn(0f, 1f))
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun NovelReaderContentLoadingPreview() {
    ReaderParserTheme {
        NovelReaderContent(
            state = NovelReaderUiState(isLoading = true),
            isDarkTheme = false,
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NovelReaderContentErrorPreview() {
    ReaderParserTheme {
        NovelReaderContent(
            state = NovelReaderUiState(error = "Failed to load chapter"),
            isDarkTheme = false,
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NovelReaderContentDarkPreview() {
    ReaderParserTheme {
        NovelReaderContent(
            state = NovelReaderUiState(
                html = "<p>In the beginning, there was darkness...</p>",
                hasPreviousChapter = true,
                hasNextChapter = true,
            ),
            isDarkTheme = true,
            onAction = {},
        )
    }
}

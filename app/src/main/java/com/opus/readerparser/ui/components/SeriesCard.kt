package com.opus.readerparser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.ui.theme.ReaderParserTheme

internal val SeriesCatalogGridContentPadding = PaddingValues(8.dp)
internal val SeriesCatalogGridItemSpacing = 8.dp
internal val SeriesCatalogGridMinSize = 150.dp
internal val SeriesCardAspectRatio = 2f / 3f
internal val SeriesCardTitlePadding = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .testTag("series_card")
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            AsyncImage(
                model = series.coverUrl,
                contentDescription = series.title,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(SeriesCardAspectRatio),
            )
            Text(
                text = series.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(SeriesCardTitlePadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeriesCardPreview() {
    ReaderParserTheme {
        SeriesCard(
            series = Series(
                sourceId = 1L,
                url = "https://example.com/series",
                title = "Solo Leveling",
                coverUrl = null,
                type = ContentType.MANHWA,
            ),
            onClick = {},
        )
    }
}

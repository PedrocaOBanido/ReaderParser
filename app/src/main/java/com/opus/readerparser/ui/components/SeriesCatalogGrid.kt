package com.opus.readerparser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.opus.readerparser.domain.model.Series

@Composable
fun SeriesCatalogGrid(
    series: List<Series>,
    onSeriesClick: (Series) -> Unit,
    modifier: Modifier = Modifier.testTag("series_list"),
    onSeriesLongClick: ((Series) -> Unit)? = null,
    gridState: LazyGridState? = null,
    content: LazyGridScope.() -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(SeriesCatalogGridMinSize),
        contentPadding = SeriesCatalogGridContentPadding,
        horizontalArrangement = Arrangement.spacedBy(SeriesCatalogGridItemSpacing),
        verticalArrangement = Arrangement.spacedBy(SeriesCatalogGridItemSpacing),
        state = gridState ?: rememberLazyGridState(),
        modifier = modifier,
    ) {
        items(series, key = { "${it.sourceId}|${it.url}" }) { item ->
            SeriesCard(
                series = item,
                onClick = { onSeriesClick(item) },
                onLongClick = onSeriesLongClick?.let { longClick -> { longClick(item) } },
            )
        }
        content()
    }
}

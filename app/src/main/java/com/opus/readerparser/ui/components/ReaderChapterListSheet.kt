package com.opus.readerparser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.opus.readerparser.domain.model.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderChapterListSheet(
    chapters: List<Chapter>,
    currentChapterUrl: String?,
    onChapterSelected: (Chapter) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val currentChapterIndex = chapters.indexOfFirst { it.url == currentChapterUrl }
    val listState = rememberLazyListState()

    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex >= 0) {
            listState.scrollToItem(currentChapterIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("reader_chapter_list_sheet"),
    ) {
        Text(
            text = "Chapters",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("reader_chapter_list_title"),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))

        if (chapters.isEmpty()) {
            Text(
                text = "No chapters available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.testTag("reader_chapter_list"),
            ) {
                itemsIndexed(
                    items = chapters,
                    key = { _, chapter -> "${chapter.sourceId}|${chapter.url}" },
                ) { index, chapter ->
                    val isCurrentChapter = chapter.url == currentChapterUrl
                    Card(
                        onClick = { onChapterSelected(chapter) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentChapter) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("reader_chapter_item_$index"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCurrentChapter) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                            if (isCurrentChapter) {
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

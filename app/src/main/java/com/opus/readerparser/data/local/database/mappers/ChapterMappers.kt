package com.opus.readerparser.data.local.database.mappers

import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterWithState

fun ChapterEntity.toDomain(): Chapter = Chapter(
    seriesUrl = seriesUrl,
    sourceId = sourceId,
    url = url,
    name = name,
    number = number,
    uploadDate = uploadDate,
)

fun Chapter.toEntity(seriesUrl: String = this.seriesUrl): ChapterEntity = ChapterEntity(
    sourceId = sourceId,
    url = url,
    seriesUrl = seriesUrl,
    name = name,
    number = number,
    uploadDate = uploadDate,
    read = false,
    progress = 0f,
    downloaded = false,
)

fun ChapterEntity.toChapterWithState(): ChapterWithState = ChapterWithState(
    chapter = toDomain(),
    read = read,
    downloaded = downloaded,
    progress = progress,
)
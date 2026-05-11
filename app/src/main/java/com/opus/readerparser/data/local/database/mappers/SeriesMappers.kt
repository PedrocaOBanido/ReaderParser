package com.opus.readerparser.data.local.database.mappers

import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesStatus

fun SeriesEntity.toDomain(): Series = Series(
    sourceId = sourceId,
    url = url,
    title = title,
    author = author,
    artist = artist,
    description = description,
    coverUrl = coverUrl,
    genres = GenreJson.jsonToGenres(genresJson),
    status = runCatching { SeriesStatus.valueOf(status) }.getOrDefault(SeriesStatus.UNKNOWN),
    type = runCatching { ContentType.valueOf(type) }.getOrDefault(ContentType.NOVEL),
)

fun Series.toEntity(): SeriesEntity = SeriesEntity(
    sourceId = sourceId,
    url = url,
    title = title,
    author = author,
    artist = artist,
    description = description,
    coverUrl = coverUrl,
    genresJson = GenreJson.genresToJson(genres),
    status = status.name,
    type = type.name,
)
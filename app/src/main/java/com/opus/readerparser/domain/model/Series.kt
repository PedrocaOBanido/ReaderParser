package com.opus.readerparser.domain.model

data class Series(
    val sourceId: Long,
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val status: SeriesStatus = SeriesStatus.UNKNOWN,
    val type: ContentType,
)

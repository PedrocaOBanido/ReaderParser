package com.opus.readerparser.domain.model

data class SeriesPage(
    val series: List<Series>,
    val hasNextPage: Boolean,
)

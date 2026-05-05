package com.opus.readerparser.domain.model

data class Chapter(
    val seriesUrl: String,
    val sourceId: Long,
    val url: String,
    val name: String,
    val number: Float,
    val uploadDate: Long? = null,
)

package com.opus.readerparser.domain.model

data class SourceInfo(
    val id: Long,
    val name: String,
    val lang: String,
    val type: ContentType,
)

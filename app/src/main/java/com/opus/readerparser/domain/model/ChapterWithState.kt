package com.opus.readerparser.domain.model

data class ChapterWithState(
    val chapter: Chapter,
    val read: Boolean,
    val downloaded: Boolean,
    val progress: Float,
)

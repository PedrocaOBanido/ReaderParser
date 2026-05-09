package com.opus.readerparser.data.local.database.entities

import androidx.room.Entity

@Entity(
    tableName = "download_queue",
    primaryKeys = ["sourceId", "chapterUrl"],
)
data class DownloadQueueEntity(
    val sourceId: Long,
    val chapterUrl: String,
    val state: String,
    val progress: Float = 0f,
    val errorMessage: String? = null,
)

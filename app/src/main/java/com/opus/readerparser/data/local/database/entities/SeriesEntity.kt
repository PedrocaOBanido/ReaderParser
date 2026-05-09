package com.opus.readerparser.data.local.database.entities

import androidx.room.Entity

@Entity(
    tableName = "series",
    primaryKeys = ["sourceId", "url"],
)
data class SeriesEntity(
    val sourceId: Long,
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val genresJson: String,
    val status: String,
    val type: String,
    val inLibrary: Boolean = false,
    val addedAt: Long? = null,
)

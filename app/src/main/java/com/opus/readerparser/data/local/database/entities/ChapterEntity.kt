package com.opus.readerparser.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chapters",
    primaryKeys = ["sourceId", "url"],
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["sourceId", "url"],
            childColumns = ["sourceId", "seriesUrl"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sourceId", "seriesUrl")],
)
data class ChapterEntity(
    val sourceId: Long,
    val url: String,
    val seriesUrl: String,
    val name: String,
    val number: Float,
    val uploadDate: Long? = null,
    val read: Boolean = false,
    val progress: Float = 0f,
    val downloaded: Boolean = false,
)

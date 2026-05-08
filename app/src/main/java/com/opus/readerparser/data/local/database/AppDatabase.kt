package com.opus.readerparser.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.opus.readerparser.data.local.database.dao.ChapterDao
import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import com.opus.readerparser.data.local.database.entities.SeriesEntity

@Database(
    entities = [SeriesEntity::class, ChapterEntity::class, DownloadQueueEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao
    abstract fun chapterDao(): ChapterDao
    abstract fun downloadQueueDao(): DownloadQueueDao
}

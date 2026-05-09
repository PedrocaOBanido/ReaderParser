package com.opus.readerparser.core.di

import android.content.Context
import androidx.room.Room
import com.opus.readerparser.data.local.database.AppDatabase
import com.opus.readerparser.data.local.database.dao.ChapterDao
import com.opus.readerparser.data.local.database.dao.DownloadQueueDao
import com.opus.readerparser.data.local.database.dao.SeriesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "reader.db").build()

    @Provides
    fun provideSeriesDao(db: AppDatabase): SeriesDao = db.seriesDao()

    @Provides
    fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideDownloadQueueDao(db: AppDatabase): DownloadQueueDao = db.downloadQueueDao()
}

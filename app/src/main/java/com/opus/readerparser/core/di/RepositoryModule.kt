package com.opus.readerparser.core.di

import com.opus.readerparser.data.local.prefs.SettingsRepositoryImpl
import com.opus.readerparser.data.repository.ChapterRepositoryImpl
import com.opus.readerparser.data.repository.DownloadRepositoryImpl
import com.opus.readerparser.data.repository.SeriesRepositoryImpl
import com.opus.readerparser.data.repository.SourceRepositoryImpl
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.DownloadRepository
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.SettingsRepository
import com.opus.readerparser.domain.SourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindSourceRepository(impl: SourceRepositoryImpl): SourceRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

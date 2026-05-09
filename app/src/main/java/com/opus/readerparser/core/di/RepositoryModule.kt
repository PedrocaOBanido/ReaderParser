package com.opus.readerparser.core.di

import com.opus.readerparser.data.repository.ChapterRepositoryImpl
import com.opus.readerparser.data.repository.SeriesRepositoryImpl
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.SeriesRepository
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
}

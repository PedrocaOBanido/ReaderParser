package com.opus.readerparser.core.di

import android.content.Context
import com.opus.readerparser.data.local.search.ContentResolverDelegate
import com.opus.readerparser.data.local.search.SearchProviderDelegate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideSearchDelegate(@ApplicationContext context: Context): SearchProviderDelegate =
        ContentResolverDelegate(context.contentResolver)
}

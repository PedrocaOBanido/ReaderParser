package com.opus.readerparser.core.di

import android.content.Context
import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.data.local.filesystem.DownloadStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualifier for the download root [File] to avoid conflicts with other [File] bindings. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadRoot

/**
 * Wires [DownloadStoreImpl] as the [DownloadStore] singleton and provides the
 * download root directory via the [DownloadRoot] qualifier.
 *
 * [DownloadStoreImpl] accepts a plain [File] root so JVM tests can inject a
 * [org.junit.rules.TemporaryFolder] without needing an Android context.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FilesystemModule {

    @Binds
    @Singleton
    abstract fun bindDownloadStore(impl: DownloadStoreImpl): DownloadStore

    companion object {

        @Provides
        @Singleton
        @DownloadRoot
        fun provideDownloadRoot(@ApplicationContext context: Context): File =
            File(context.filesDir, "downloads").also { it.mkdirs() }
    }
}

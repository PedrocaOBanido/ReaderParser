package com.opus.readerparser.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.opus.readerparser.data.local.prefs.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [DataStore]<[Preferences]> singleton used by [SettingsStore].
 *
 * Keeping the DataStore provision here (rather than in [RepositoryModule]) means
 * the JVM-testable [SettingsRepositoryImpl] never sees an Android [Context] directly;
 * it only depends on the injected [SettingsStore].
 */
@Module
@InstallIn(SingletonComponent::class)
object PrefsModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore
}

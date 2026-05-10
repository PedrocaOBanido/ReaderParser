package com.opus.readerparser.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Extension property providing the app-wide settings [DataStore].
 *
 * Declared `internal` so it is accessible within the same Gradle module
 * (including [com.opus.readerparser.core.di.PrefsModule]). The [preferencesDataStore]
 * delegate ensures a single instance is created per
 * [Context], even if the property is accessed concurrently.
 */
internal val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings")

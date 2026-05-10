package com.opus.readerparser.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin injectable wrapper around [DataStore]<[Preferences]>.
 *
 * Exists so that [SettingsRepositoryImpl] can be tested on the JVM without
 * an Android context: tests supply a fake [DataStore] at construction time
 * rather than using the [preferencesDataStore] delegate on [Context].
 */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** The underlying preferences [Flow]. Delegates directly to the [DataStore]. */
    val data: Flow<Preferences> get() = dataStore.data

    /**
     * Applies [transform] to the current [MutablePreferences] atomically.
     * Delegates directly to [DataStore.edit].
     */
    suspend fun edit(transform: suspend (MutablePreferences) -> Unit): Preferences =
        dataStore.edit(transform)
}

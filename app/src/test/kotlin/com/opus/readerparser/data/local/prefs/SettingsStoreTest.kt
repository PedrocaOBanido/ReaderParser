package com.opus.readerparser.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for [SettingsStore].
 *
 * Uses a hand-rolled [FakeDataStore] to verify that [SettingsStore] correctly
 * delegates [data] and [edit] to the underlying [DataStore] without any Android
 * dependency.
 */
class SettingsStoreTest {

    // ---- fake ---------------------------------------------------------------

    private class FakeDataStore : DataStore<Preferences> {
        private var current: Preferences = mutablePreferencesOf()

        override val data: Flow<Preferences>
            get() = flowOf(current)

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            current = transform(current)
            return current
        }
    }

    // Adapter so tests can call store.edit { } the same way production code does.
    // DataStore<Preferences>.edit is an extension function that calls updateData internally.
    // We expose it via SettingsStore.edit which calls dataStore.edit.
    // The real DataStore.edit extension uses MutablePreferences; our fake needs to support it.
    // We implement a minimal stand-in by overriding updateData on the fake.
    private class EditableFakeDataStore : DataStore<Preferences> {
        private var current: Preferences = mutablePreferencesOf()

        override val data: Flow<Preferences>
            get() = flowOf(current)

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            // DataStore.edit converts MutablePreferences back to Preferences internally.
            // Here we just cast; mutablePreferencesOf() IS a MutablePreferences.
            val mutable = current.toMutablePreferences()
            current = transform(mutable)
            return current
        }
    }

    // ---- tests --------------------------------------------------------------

    @Test
    fun `data delegates to underlying DataStore`() = runTest {
        val fakeDataStore = FakeDataStore()
        val store = SettingsStore(fakeDataStore)

        val prefs = store.data.first()
        assertEquals(fakeDataStore.data.first(), prefs)
    }

    @Test
    fun `edit updates underlying DataStore`() = runTest {
        val fakeDataStore = EditableFakeDataStore()
        val store = SettingsStore(fakeDataStore)

        val key = stringPreferencesKey("test_key")
        store.edit { it[key] = "hello" }

        val prefs = store.data.first()
        assertEquals("hello", prefs[key])
    }

    @Test
    fun `edit can overwrite an existing preference`() = runTest {
        val fakeDataStore = EditableFakeDataStore()
        val store = SettingsStore(fakeDataStore)

        val key = stringPreferencesKey("theme")
        store.edit { it[key] = "DARK" }
        store.edit { it[key] = "LIGHT" }

        assertEquals("LIGHT", store.data.first()[key])
    }
}

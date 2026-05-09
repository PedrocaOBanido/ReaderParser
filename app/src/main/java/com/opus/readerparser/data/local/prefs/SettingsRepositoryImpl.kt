package com.opus.readerparser.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opus.readerparser.domain.SettingsRepository
import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import com.opus.readerparser.domain.model.ManhwaLayout
import com.opus.readerparser.domain.model.ManhwaZoom
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NOVEL_FONT_SIZE = intPreferencesKey("novel_font_size")
        val NOVEL_FONT_FAMILY = stringPreferencesKey("novel_font_family")
        val MANHWA_LAYOUT = stringPreferencesKey("manhwa_layout")
        val MANHWA_ZOOM = stringPreferencesKey("manhwa_zoom")
    }

    override fun observeSettings(): Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM,
                novelFontSize = prefs[Keys.NOVEL_FONT_SIZE] ?: 16,
                novelFontFamily = prefs[Keys.NOVEL_FONT_FAMILY] ?: "Default",
                manhwaLayout = prefs[Keys.MANHWA_LAYOUT]?.let { ManhwaLayout.valueOf(it) } ?: ManhwaLayout.WEBTOON,
                manhwaZoom = prefs[Keys.MANHWA_ZOOM]?.let { ManhwaZoom.valueOf(it) } ?: ManhwaZoom.FIT_WIDTH,
            )
        }

    override suspend fun setTheme(theme: AppTheme) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme.name }
    }

    override suspend fun setNovelFontSize(size: Int) {
        context.settingsDataStore.edit { it[Keys.NOVEL_FONT_SIZE] = size }
    }

    override suspend fun setNovelFontFamily(family: String) {
        context.settingsDataStore.edit { it[Keys.NOVEL_FONT_FAMILY] = family }
    }

    override suspend fun setManhwaLayout(layout: ManhwaLayout) {
        context.settingsDataStore.edit { it[Keys.MANHWA_LAYOUT] = layout.name }
    }

    override suspend fun setManhwaZoom(zoom: ManhwaZoom) {
        context.settingsDataStore.edit { it[Keys.MANHWA_ZOOM] = zoom.name }
    }
}

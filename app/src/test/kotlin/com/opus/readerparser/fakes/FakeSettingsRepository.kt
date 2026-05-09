package com.opus.readerparser.fakes

import com.opus.readerparser.domain.SettingsRepository
import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import com.opus.readerparser.domain.model.ManhwaLayout
import com.opus.readerparser.domain.model.ManhwaZoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())

    override fun observeSettings(): Flow<AppSettings> = _settings

    override suspend fun setTheme(theme: AppTheme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    override suspend fun setNovelFontSize(size: Int) {
        _settings.value = _settings.value.copy(novelFontSize = size)
    }

    override suspend fun setNovelFontFamily(family: String) {
        _settings.value = _settings.value.copy(novelFontFamily = family)
    }

    override suspend fun setManhwaLayout(layout: ManhwaLayout) {
        _settings.value = _settings.value.copy(manhwaLayout = layout)
    }

    override suspend fun setManhwaZoom(zoom: ManhwaZoom) {
        _settings.value = _settings.value.copy(manhwaZoom = zoom)
    }
}

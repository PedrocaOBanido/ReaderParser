package com.opus.readerparser.ui.settings

import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import com.opus.readerparser.domain.model.ManhwaLayout
import com.opus.readerparser.domain.model.ManhwaZoom

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
)

sealed interface SettingsAction {
    data class SetTheme(val theme: AppTheme) : SettingsAction
    data class SetNovelFontSize(val size: Int) : SettingsAction
    data class SetNovelFontFamily(val family: String) : SettingsAction
    data class SetManhwaLayout(val layout: ManhwaLayout) : SettingsAction
    data class SetManhwaZoom(val zoom: ManhwaZoom) : SettingsAction
}

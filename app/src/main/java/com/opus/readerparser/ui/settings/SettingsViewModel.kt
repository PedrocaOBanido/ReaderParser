package com.opus.readerparser.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opus.readerparser.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetTheme -> viewModelScope.launch {
                settingsRepository.setTheme(action.theme)
            }
            is SettingsAction.SetNovelFontSize -> viewModelScope.launch {
                settingsRepository.setNovelFontSize(action.size)
            }
            is SettingsAction.SetNovelFontFamily -> viewModelScope.launch {
                settingsRepository.setNovelFontFamily(action.family)
            }
            is SettingsAction.SetManhwaLayout -> viewModelScope.launch {
                settingsRepository.setManhwaLayout(action.layout)
            }
            is SettingsAction.SetManhwaZoom -> viewModelScope.launch {
                settingsRepository.setManhwaZoom(action.zoom)
            }
        }
    }
}

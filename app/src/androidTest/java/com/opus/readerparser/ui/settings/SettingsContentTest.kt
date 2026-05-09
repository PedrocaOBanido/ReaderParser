package com.opus.readerparser.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultSettings_settingsListIsVisible() {
        composeRule.setContent {
            SettingsContent(state = SettingsUiState(settings = AppSettings()), onAction = {})
        }
        composeRule.onNodeWithTag("settings_list").assertIsDisplayed()
    }

    @Test
    fun themeOptions_areAllVisible() {
        composeRule.setContent {
            SettingsContent(state = SettingsUiState(settings = AppSettings()), onAction = {})
        }
        composeRule.onNodeWithText("System").assertIsDisplayed()
        composeRule.onNodeWithText("Light").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun selectDarkTheme_dispatchesSetThemeAction() {
        var selectedTheme: AppTheme? = null
        composeRule.setContent {
            SettingsContent(
                state = SettingsUiState(settings = AppSettings(theme = AppTheme.SYSTEM)),
                onAction = { action ->
                    if (action is SettingsAction.SetTheme) selectedTheme = action.theme
                },
            )
        }
        composeRule.onNodeWithText("Dark").performClick()
        assert(selectedTheme == AppTheme.DARK)
    }

    @Test
    fun fontSizeSlider_isVisible() {
        composeRule.setContent {
            SettingsContent(state = SettingsUiState(settings = AppSettings()), onAction = {})
        }
        composeRule.onNodeWithTag("font_size_slider").assertIsDisplayed()
    }
}

package com.opus.readerparser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import com.opus.readerparser.domain.model.ManhwaLayout
import com.opus.readerparser.domain.model.ManhwaZoom
import com.opus.readerparser.ui.theme.ReaderParserTheme

private val novelFontFamilies = listOf("Default", "Serif", "Monospace")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
    var fontFamilyExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_list"),
        ) {
            // Theme section
            item {
                SectionHeader("Appearance")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    AppTheme.entries.forEach { theme ->
                        RadioRow(
                            label = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = state.settings.theme == theme,
                            onClick = { onAction(SettingsAction.SetTheme(theme)) },
                        )
                    }
                }
            }

            item { SectionDivider() }

            // Novel reader section
            item {
                SectionHeader("Novel Reader")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Font size: ${state.settings.novelFontSize} sp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = state.settings.novelFontSize.toFloat(),
                        onValueChange = { onAction(SettingsAction.SetNovelFontSize(it.toInt())) },
                        valueRange = 12f..24f,
                        steps = 11,
                        modifier = Modifier.testTag("font_size_slider"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Font family",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    ExposedDropdownMenuBox(
                        expanded = fontFamilyExpanded,
                        onExpandedChange = { fontFamilyExpanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = state.settings.novelFontFamily,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontFamilyExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = fontFamilyExpanded,
                            onDismissRequest = { fontFamilyExpanded = false },
                        ) {
                            novelFontFamilies.forEach { family ->
                                DropdownMenuItem(
                                    text = { Text(family) },
                                    onClick = {
                                        onAction(SettingsAction.SetNovelFontFamily(family))
                                        fontFamilyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item { SectionDivider() }

            // Manhwa reader section
            item {
                SectionHeader("Manhwa Reader")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Layout",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    ManhwaLayout.entries.forEach { layout ->
                        RadioRow(
                            label = layout.name.lowercase().replace('_', ' ')
                                .replaceFirstChar { it.uppercase() },
                            selected = state.settings.manhwaLayout == layout,
                            onClick = { onAction(SettingsAction.SetManhwaLayout(layout)) },
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Zoom",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    ManhwaZoom.entries.forEach { zoom ->
                        RadioRow(
                            label = zoom.name.lowercase().replace('_', ' ')
                                .replaceFirstChar { it.uppercase() },
                            selected = state.settings.manhwaZoom == zoom,
                            onClick = { onAction(SettingsAction.SetManhwaZoom(zoom)) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentDefaultPreview() {
    ReaderParserTheme {
        SettingsContent(
            state = SettingsUiState(settings = AppSettings()),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentDarkThemeSelectedPreview() {
    ReaderParserTheme {
        SettingsContent(
            state = SettingsUiState(
                settings = AppSettings(
                    theme = AppTheme.DARK,
                    novelFontSize = 20,
                    novelFontFamily = "Serif",
                    manhwaLayout = ManhwaLayout.WEBTOON,
                    manhwaZoom = ManhwaZoom.FIT_HEIGHT,
                ),
            ),
            onAction = {},
        )
    }
}

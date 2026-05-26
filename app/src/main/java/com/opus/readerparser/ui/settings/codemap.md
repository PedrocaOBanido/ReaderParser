# app/src/main/java/com/opus/readerparser/ui/settings/

## Responsibility

Manages the app-wide settings screen — lets the user configure theme (system /
light / dark), novel reader font size and font family, and manhwa reader layout
(paged LTR / paged RTL / webtoon) and zoom (fit width / fit height / original).

## Design

Four files following the standard screen pattern:

| File | Role |
|---|---|
| `SettingsScreen.kt` | Wires `SettingsViewModel` via `hiltViewModel()`, collects state, delegates to `SettingsContent`. No effects collection needed — settings has no one-shot navigation or snackbar effects. Never previewed. |
| `SettingsContent.kt` | Stateless composable — `@Composable fun SettingsContent(state, onAction)`. Has `@Preview`s for default and dark-theme-selected states. |
| `SettingsUiState.kt` | `data class SettingsUiState` (settings, isLoading) + sealed `SettingsAction`. No `SettingsEffect` — settings mutations are fire-and-forget; visual feedback comes via the reactive state update. |
| `SettingsViewModel.kt` | `@HiltViewModel` — injects `SettingsRepository`, exposes `state: StateFlow<SettingsUiState>`. No `effects` channel. |

**State:** `SettingsUiState` wraps `AppSettings` (theme, novelFontSize,
novelFontFamily, manhwaLayout, manhwaZoom) plus an `isLoading` flag. The
ViewModel's `init` block collects `settingsRepository.observeSettings()` and
updates state on every emission.

**Action (no effect):** Five actions, each mapping to a top-level setting
field on `AppSettings`:

| Action | Repository method | UI control |
|---|---|---|
| `SetTheme(theme)` | `setTheme()` | Radio group — `AppTheme.entries` |
| `SetNovelFontSize(size)` | `setNovelFontSize()` | `Slider` (12–24 sp, 1-sp steps) |
| `SetNovelFontFamily(family)` | `setNovelFontFamily()` | `ExposedDropdownMenu` (Default / Serif / Monospace) |
| `SetManhwaLayout(layout)` | `setManhwaLayout()` | Radio group — `ManhwaLayout.entries` |
| `SetManhwaZoom(zoom)` | `setManhwaZoom()` | Radio group — `ManhwaZoom.entries` |

No effect sealed interface — there is no navigation trigger and no error state
for settings mutations. The reactive state flow provides all visual feedback.

**Sub-composables:** `SectionHeader`, `SectionDivider`
(`HorizontalDivider`), and `RadioRow` (a clickable `Row` with `RadioButton`
and label using `Modifier.selectable`). All private to `SettingsContent.kt`.

## Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ SettingsScreen                                                  │
│  collectAsStateWithLifecycle → SettingsContent(state, onAction)  │
│  (no effects channel — settings has no one-shot side effects)   │
└────────────────┬────────────────────────────────────────────────┘
                 │ onAction(SetTheme / SetNovelFontSize / …)
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│ SettingsViewModel                                               │
│  init { settingsRepository.observeSettings().collect { → state }}│
│  SetTheme        → settingsRepository.setTheme()                │
│  SetNovelFontSize→ settingsRepository.setNovelFontSize()        │
│  …               → …                                           │
└────────────────┬────────────────────────────────────────────────┘
                 │ observeSettings()         set*(…)
                 ▼                            │
┌─────────────────────────────────────────────────────────────────┐
│ SettingsRepository (domain interface, impl in data/local/prefs/)│
│  observeSettings(): Flow<AppSettings>                           │
│  setTheme(), setNovelFontSize(), setNovelFontFamily(),          │
│  setManhwaLayout(), setManhwaZoom()                             │
└─────────────────────────────────────────────────────────────────┘
```

The ViewModel's `init` block launches a coroutine that permanently collects
the `observeSettings()` flow. When the user adjusts a setting, the action
delegates to the corresponding `set*` method on the repository, which writes
to DataStore Preferences. The DataStore flow re-emits the new `AppSettings`,
which the ViewModel picks up through the ongoing collection and updates
`_state`. This means the state update is **eventually consistent** — the
ViewModel does not optimistically set the value; it waits for the repository
to confirm via the flow. The latency is typically sub-millisecond (DataStore
is in-process).

## Integration

- **Depends on:** `SettingsRepository` (domain interface), `AppSettings`,
  `AppTheme`, `ManhwaLayout`, `ManhwaZoom` (domain models). No knowledge of
  DataStore, Room, or Ktor.
- **No dependency on `SourceRegistry` or any concrete source** — all
  persistence is behind the repository.
- The `settings_list` test tag on `LazyColumn` and `font_size_slider` test tag
  enable Compose UI testing.
- Theme changes are applied reactively: the `ReaderParserTheme` composable
  (in `ui/theme/`) reads the current `AppSettings.theme` to decide
  `darkColorScheme` / `lightColorScheme`. This codemap folder does not
  apply the theme itself — it only surfaces the user's choice.
- The settings screen has no `LaunchedEffect` for effects collection because
  there are no one-shot side effects. Every user action produces an immediate
  visual response through the reactive state flow.

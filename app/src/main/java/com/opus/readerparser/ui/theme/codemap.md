# app/src/main/java/com/opus/readerparser/ui/theme/

## Responsibility

Defines the app's visual identity: Material 3 colour palette, typography, and
the top-level composable that wires them into `MaterialTheme`. Every screen,
component, and preview in the app is wrapped in `ReaderParserTheme`, making this
the single source of truth for light/dark scheme switching.

## Design

### Three files, one concern

| File | Contents |
|---|---|
| `Color.kt` | Raw `Color` constants for light and dark schemes (primary, secondary, tertiary, background, surface, error + their `on*` counterparts). No scheme wiring. |
| `Type.kt` | `AppTypography` (default `Typography()`) and `NovelReaderBodyStyle` — a `TextStyle` for the novel reader body, using `FontFamily.Serif`, `16.sp`, `26.sp` line height. |
| `Theme.kt` | Builds `lightColorScheme()` and `darkColorScheme()` from colour constants, then exposes `ReaderParserTheme(appTheme, content)`. |

### Light / dark colour scheme

Forty colours defined in `Color.kt`: 20 per scheme (10 paired tokens:
`Primary`/`OnPrimary`, `PrimaryContainer`/`OnPrimaryContainer`, etc.). The
`Theme.kt` wires them into `lightColorScheme { ... }` and
`darkColorScheme { ... }` blocks.

```
LightColorScheme:
  primary     ← PrimaryLight         primaryContainer ← PrimaryContainerLight
  secondary   ← SecondaryLight       secondaryContainer ← SecondaryContainerLight
  tertiary    ← TertiaryLight
  background  ← BackgroundLight      surface ← SurfaceLight
  error       ← ErrorLight
  onPrimary   ← OnPrimaryLight       onPrimaryContainer ← OnPrimaryContainerLight
  ... (20 tokens total)

DarkColorScheme:
  (mirror structure with PrimaryDark / OnPrimaryDark / etc.)
```

### `ReaderParserTheme` composable

```kotlin
@Composable
fun ReaderParserTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
)
```

- Accepts `AppTheme` (a domain model enum from `domain/model/`).
- Maps `AppTheme.LIGHT → false`, `AppTheme.DARK → true`, `AppTheme.SYSTEM → isSystemInDarkTheme()`.
- Passes the resolved `darkTheme` boolean to choose `DarkColorScheme` or `LightColorScheme`.
- Also passes `AppTypography` as the `typography` parameter.
- Defaults to `SYSTEM` so previews and the initial launch use the device setting.

### NovelReaderBodyStyle

A stand-alone `TextStyle` for the novel reader's body text. Font size and family
are overridden at runtime from `AppSettings` (stored in DataStore); this `TextStyle`
serves as the compile-time fallback default. Not wired into `AppTypography` because
it changes per-user at runtime.

## Flow

```
MainActivity
  → AppTheme from SettingsRepository (DataStore)
  → ReaderParserTheme(appTheme = loadedTheme) {
      AppNavGraph()
    }
      → MaterialTheme(colorScheme, typography = AppTypography)
        → All composables access tokens via MaterialTheme.colorScheme.*
```

Previews:
```
@Preview
fun LibraryContentLoadingPreview() {
    ReaderParserTheme { LibraryContent(state = …, onAction = {}) }
}
```

## Integration

- **Domain**: `AppTheme` (LIGHT / DARK / SYSTEM) is a domain model in
  `domain/model/`. The `SettingsViewModel` exposes the current value, which is
  read from `SettingsRepository` (backed by DataStore). The activity or top-level
  composable passes it to `ReaderParserTheme`.
- **Material 3**: The theme wraps `MaterialTheme` from `androidx.compose.material3`.
  All screen composables reference `MaterialTheme.colorScheme`, `.typography`,
  and other theme tokens; they never hardcode colours or font sizes.
- **Preview support**: Every `*Content.kt` preview wraps in `ReaderParserTheme()`
  with the default `AppTheme.SYSTEM`, ensuring previews match the device theme.
- **No Compose Navigation dependency**: The theme composable is agnostic of the
  navigation graph. It wraps the entire composable tree, not individual routes.
- **Runtime overrides**: When the user changes theme in Settings, the
  `SettingsViewModel` persists the value via `SettingsRepository`, and the
  activity re-observes the flow, passing the new `AppTheme` to
  `ReaderParserTheme`, which recomposes with the updated colour scheme.

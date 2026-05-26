# app/src/main/java/com/opus/readerparser/data/local/prefs/

## Responsibility

App-wide user preferences backed by Jetpack DataStore (Preferences). Provides
a reactive `Flow<AppSettings>` and atomic write operations for each individual
setting.

## Files

| File | Role |
|---|---|
| `DataStoreExt.kt` | Extension property `Context.settingsDataStore` — creates the named DataStore instance (`"settings"`) via the `preferencesDataStore` delegate. Internal visibility. |
| `SettingsStore.kt` | Thin injectable wrapper around `DataStore<Preferences>`. Exposes `data: Flow<Preferences>` and `edit {}` suspend function. Exists so that `SettingsRepositoryImpl` can be JVM-tested with a fake DataStore. |
| `SettingsRepositoryImpl.kt` | Implements `domain.SettingsRepository`. Maps raw `Preferences` to `AppSettings` domain model. |

## Design

**Three-layer indirection for testability:**
```
DataStore<Preferences>  ←  SettingsStore  ←  SettingsRepositoryImpl
   (Android-only)          (injectable)        (domain-facing)
```
- `DataStoreExt.kt` owns the `preferencesDataStore` delegate (Android `Context`
  required) and exposes it as an internal extension.
- `SettingsStore` is a testable wrapper — JVM tests inject an in-memory
  `DataStore` (via `preferencesDataStore`'s `fake()` or similar) without an
  Android context.
- `SettingsRepositoryImpl` implements the domain interface and does no Android
  imports.

**Preference keys** are defined as `private object Keys` inside
`SettingsRepositoryImpl`. Each maps to a typed `Preferences.Key`:

| Key | Type | Default |
|---|---|---|
| `theme` | String (`AppTheme.name`) | `AppTheme.SYSTEM` |
| `novel_font_size` | Int | 16 |
| `novel_font_family` | String | `"Default"` |
| `manhwa_layout` | String (`ManhwaLayout.name`) | `ManhwaLayout.WEBTOON` |
| `manhwa_zoom` | String (`ManhwaZoom.name`) | `ManhwaZoom.FIT_WIDTH` |

**Enum-based settings** are stored as their `.name` string and restored with
`valueOf()`. Defaults are provided when the preference is absent (first launch
or corruption).

**One setting per write method.** Each `set*()` method calls `store.edit { it[KEY] = value }`.
DataStore ensures atomicity — concurrent edits are serialized, and partial
writes are rolled back.

## Flow

```
SettingsViewModel
  → SettingsRepository (domain interface)
     → SettingsRepositoryImpl
        → SettingsStore.data: Flow<Preferences>
           → DataStore<Preferences>.data: Flow<Preferences>
              → reads from disk on each emission

Write path:
  ViewModel → repo.setTheme(SYSTEM)
     → SettingsRepositoryImpl → store.edit { it[Keys.THEME] = "SYSTEM" }
        → DataStore.edit {} (atomic, suspend)
```

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `domain/SettingsRepository.kt` | Implements | `SettingsRepositoryImpl` binds via `RepositoryModule` |
| `domain/model/AppSettings.kt` | Returns | `observeSettings()` maps raw prefs to `AppSettings` |
| `core/di/PrefsModule.kt` | Provides DataStore | `@Provides fun providePreferencesDataStore(context): DataStore<Preferences>` using `settingsDataStore` extension |
| `core/di/RepositoryModule.kt` | Wires repository | `@Binds SettingsRepositoryImpl → SettingsRepository` |

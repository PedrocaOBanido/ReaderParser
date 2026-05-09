# Active context

Last updated: 2026-05-09 (Phase 4 complete)

## Current phase

**Phase 4** — ViewModels ✅ **COMPLETE** (177 tests, 0 failures; build + lint clean)

All Phase 4 artifacts are untracked/uncommitted; commit them before starting Phase 5.

## What was just completed

**Phase 4 — ViewModels (7 screens + infrastructure)**

### New domain models
- `SourceInfo`, `DownloadState`, `DownloadItem`, `AppTheme`, `ManhwaLayout`, `ManhwaZoom`, `AppSettings`

### New domain interfaces
- `SourceRepository`, `DownloadRepository`, `SettingsRepository`

### New data implementations
- `SourceRepositoryImpl` — wraps `SourceRegistry.all()` → `SourceInfo`
- `DownloadRepositoryImpl` — wraps `DownloadQueueDao.observeAll()` + cancel/retry
- `SettingsRepositoryImpl` — wraps DataStore Preferences
- `DownloadQueueDao` extended with `observeAll()` (no schema change, no migration)
- `RepositoryModule` updated with 3 new `@Binds` entries

### New test fakes
- `FakeSourceRepository`, `FakeDownloadRepository`, `FakeSettingsRepository`

### ViewModels (7 screens — 2 production files + 1 test each)
| Screen | ViewModel | UiState | Test |
|---|---|---|---|
| Library | LibraryViewModel | LibraryUiState/Action/Effect | LibraryViewModelTest |
| Browse | BrowseViewModel | BrowseUiState/Action/Effect | BrowseViewModelTest |
| Series | SeriesViewModel | SeriesUiState/Action/Effect | SeriesViewModelTest |
| Novel reader | NovelReaderViewModel | NovelReaderUiState/Action/Effect | NovelReaderViewModelTest |
| Manga reader | MangaReaderViewModel | MangaReaderUiState/Action/Effect | MangaReaderViewModelTest |
| Downloads | DownloadsViewModel | DownloadsUiState/Action/Effect | DownloadsViewModelTest |
| Settings | SettingsViewModel | SettingsUiState/Action | SettingsViewModelTest |

### Key testing insight
With `UnconfinedTestDispatcher` (default in `MainDispatcherRule`), `viewModelScope.launch`
blocks in `init` run synchronously before the test body. Do NOT call `awaitItem()` twice to
"wait for loading" — init is already complete. Check `vm.state.value` for init results,
and call `awaitItem()` only for state changes triggered by explicit actions inside the test.

## What's next

**Phase 5 — Compose Screens**

For each of the 7 screens, add the remaining 2 files (Screen.kt + Content.kt):
1. `ui/library/LibraryScreen.kt` + `LibraryContent.kt`
2. `ui/browse/BrowseScreen.kt` + `BrowseContent.kt`
3. `ui/series/SeriesScreen.kt` + `SeriesContent.kt`
4. `ui/reader/novel/NovelReaderScreen.kt` + `NovelReaderContent.kt`
5. `ui/reader/manhwa/MangaReaderScreen.kt` + `MangaReaderContent.kt`
6. `ui/downloads/DownloadsScreen.kt` + `DownloadsContent.kt`
7. `ui/settings/SettingsScreen.kt` + `SettingsContent.kt`
8. Navigation graph (`ui/navigation/NavGraph.kt`, `Destinations.kt`)
9. Compose UI tests targeting `*Content` composables

## Active conventions in play

- TDD: tests first, then production code (red → green → refactor)
- No `runBlocking` — use `runTest` from `kotlinx-coroutines-test`
- Hand-rolled fakes for interfaces we control — not Mockito/MockK
- `com.opus.readerparser` package
- Unit tests in `app/src/test/kotlin/`, androidTests in `app/src/androidTest/`
- Commit prefixes: `feat:` `fix:` `refactor:` `ci:` `cd:` `docs:`
- Git for local ops, `gh` CLI for remote
- Identity is `(sourceId, url)` — never auto-increment IDs as FKs
- `fallbackToDestructiveMigration` forbidden in all build configs

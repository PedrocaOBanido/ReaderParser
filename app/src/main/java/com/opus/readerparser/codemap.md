# app/src/main/java/com/opus/readerparser/

## Responsibility

Root package of the ReaderParser Android application. Contains all production Kotlin source code organised into six coherent subsystems:

| Package         | Role |
|-----------------|------|
| `App.kt`        | `@HiltAndroidApp` Application — Hilt DI root + WorkManager config |
| `MainActivity.kt` | `@AndroidEntryPoint` launcher — sets Compose content via `AppNavGraph` |
| `ui/`           | Compose screens (library, browse, series, readers, downloads, settings), reusable components, theme, navigation |
| `domain/`       | Pure-Kotlin models + repository interfaces (zero Android imports) |
| `data/`         | Repository implementations, Room DB/DAOs/entities, filesystem store, DataStore prefs, network config, `Source` contract + registry |
| `sources/`      | Per-site Source plugins (each in its own subdirectory) |
| `workers/`      | WorkManager `CoroutineWorker` implementations (download, library update) |
| `core/`         | Hilt modules (`di/`), utilities (`util/`), result types (`result/`) |

The architecture follows a strict 6-layer model (UI → Presentation → Domain → Data → Source → Infrastructure).

## Design

- **Layered architecture** with strict dependency direction: UI → Presentation → Domain → Data → Source → Infrastructure. Lower layers never import higher layers.
- **Domain is Android-free.** `domain/model/` and domain interfaces compile against plain JVM. No `androidx.*`, `io.ktor.*`, `androidx.room.*` imports.
- **Source plugin system.** Each site is an `HtmlSource` subclass behind the `Source` interface. `SourceRegistry` maps `Long` source IDs to instances. Plugins are wired at compile time in `SourceModule`.
- **Repository pattern.** `SeriesRepository`, `ChapterRepository`, `DownloadRepository`, `SettingsRepository` interfaces in `domain/`; implementations in `data/repository/`. ViewModels talk only to repositories, never to `Source` directly.
- **State management per screen.** Each screen has 4 files: `*Screen.kt` (wires VM + effects), `*Content.kt` (stateless composable + Preview), `*ViewModel.kt` (StateFlow + Channel), `*UiState.kt` (UiState, Action, Effect).
- **Separate readers.** Novel reader (`ui/reader/novel/`) and manhwa reader (`ui/reader/manhwa/`) are distinct screens, reached via navigation dispatch on `ContentType`.
- **Identity strategy.** Series and chapter identity is `(sourceId, url)`. No auto-increment foreign keys.
- **Storage.** Room DB for library metadata and progress; app-private filesystem for downloaded content; DataStore Preferences for settings.

## Flow

```
App.kt (onCreate → Hilt init)
  └─ MainActivity.kt (setContent → ReaderParserTheme → AppNavGraph)
       ├─ LibraryScreen ── LibraryViewModel ── SeriesRepository + SettingsRepository
       ├─ BrowseScreen ─── BrowseViewModel ─── SourceRepository + SeriesRepository
       ├─ SeriesScreen ── SeriesViewModel ─── SeriesRepository + ChapterRepository
       ├─ NovelReader ─── NovelReaderViewModel ── ChapterRepository + SettingsRepository
       ├─ MangaReader ─── MangaReaderViewModel ── ChapterRepository + SettingsRepository
       ├─ DownloadsScreen ─ DownloadsViewModel ── DownloadRepository
       └─ SettingsScreen ── SettingsViewModel ── SettingsRepository
```

Data flows: **User action → ViewModel.onAction() → Repository → Source/DAO/Store → Repository → StateFlow → Compose UI**

Side effects (navigation, snackbar): **ViewModel → Channel<Effect> → LaunchedEffect(Unit) in *Screen → one-shot action**

Background flows: **ChapterDownloadWorker** (WorkManager) → SourceRegistry → Source → client.get() → DownloadStore; **LibraryUpdateWorker** → SeriesRepository → Source → DAO upsert

## Integration

- **Hilt.** `App` is `@HiltAndroidApp`. `MainActivity` is `@AndroidEntryPoint`. Hilt modules in `core/di/` provide: HTTP client (`NetworkModule`), Room DB (`DatabaseModule`), DataStore (`PrefsModule`), download paths (`FilesystemModule`), source registry (`SourceModule`), and all repository implementations (`RepositoryModule`).
- **Navigation.** `Destinations.kt` defines route constants; `NavGraph.kt` wires them to screen composables via `NavHost`. Navigation arguments include `sourceId`, `seriesUrl`, `chapterUrl`, and `type`.
- **Workers.** `ChapterDownloadWorker` and `LibraryUpdateWorker` are `@HiltWorker` classes, registered via Hilt's multibinding. WorkManager is configured through `App.workManagerConfiguration` using `HiltWorkerFactory`.
- **Test infrastructure.** JVM tests in `app/src/test/` mirror package structure with fakes in `.../fakes/`. Room migration tests in `app/src/androidTest/`. Compose UI tests target `*Content` composables via `createComposeRule()`.
- **Manifest.** `INTERNET` permission required for Ktor HTTP calls. `WorkManagerInitializer` removed from startup to let Hilt manage worker injection.

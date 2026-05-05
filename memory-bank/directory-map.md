# Directory map

Package root: `com.opus.readerparser` (under `app/src/main/java/`)

```
com.opus.readerparser/
├── App.kt                              @HiltAndroidApp entry point
│
├── domain/
│   ├── model/                          Pure data classes: Series, Chapter, ChapterContent, etc.
│   └── usecase/                        Cross-repo operations (sparingly)
│
├── data/
│   ├── source/                         Source interface, HtmlSource base class, SourceRegistry
│   ├── repository/                     Repository interfaces + impls (SeriesRepositoryImpl, etc.)
│   ├── local/
│   │   ├── database/                   AppDatabase, DAOs, entities, migrations, mappers
│   │   │   ├── dao/
│   │   │   ├── entities/
│   │   │   ├── mappers/
│   │   │   └── migrations/
│   │   ├── filesystem/                 DownloadStore, path helpers
│   │   └── prefs/                      SettingsStore (DataStore)
│   └── network/                        Ktor client config, JSON, cookie jar
│
├── sources/                            One subdirectory per site, each with one .kt file
│   └── <sitename>/<SiteName>.kt        Extends HtmlSource
│
├── ui/
│   ├── browse/                         BrowseScreen, BrowseViewModel, BrowseUiState
│   ├── components/                     Reusable composables: Cover, ChapterRow, StatusPill, etc.
│   ├── downloads/                      DownloadsScreen
│   ├── library/                        LibraryScreen
│   ├── navigation/                     NavGraph, Destinations
│   ├── reader/
│   │   ├── novel/                      NovelReaderScreen (separate from manhwa)
│   │   └── manhwa/                     MangaReaderScreen (separate from novel)
│   ├── series/                         SeriesScreen
│   ├── settings/                       SettingsScreen
│   └── theme/                          Color, Typography, Theme
│
├── workers/                            ChapterDownloadWorker, LibraryUpdateWorker
│
└── core/
    ├── di/                             Hilt modules: SourceModule, NetworkModule, DatabaseModule, etc.
    ├── result/                         Result types, error mapping
    └── util/                           Extensions, hashing, dates
```

## Test source sets

| Source set | Path | Purpose |
|---|---|---|
| Unit tests | `app/src/test/java/com/opus/readerparser/` | Domain models, ViewModels, repositories, sources |
| Test utilities | `app/src/test/java/com/opus/readerparser/testutil/` | Shared helpers available in all JVM tests |
| Fakes | `app/src/test/java/com/opus/readerparser/fakes/` | Hand-rolled test doubles for interfaces we control |
| Test resources | `app/src/test/resources/` | HTML fixtures, JSON fixtures |
| Instrumented tests | `app/src/androidTest/java/com/opus/readerparser/` | Compose UI tests, migration tests, worker tests |
| Room schemas | `app/schemas/` | Auto-exported by KSP (checked into VCS) |
| Journey tests | `journeys/` | XML specs for screen-to-screen flow tests on emulator |

## Infrastructure

| File | Purpose |
|---|---|
| `avd-config.json` | Shared AVD configuration (device, API, image) for pipeline and agents |
| `scripts/emulator` | AVD lifecycle management via `android emulator` |
| `scripts/run-journeys` | Journey test listing and execution guidance |
| `.github/workflows/journey.yml` | Manual-trigger CI workflow for journey tests on emulator |

## Key identity rules

- Series: `(sourceId, url)` — URLs are stable; row IDs are not
- Chapter: `(sourceId, url)` — FK to Series via `(sourceId, seriesUrl)`
- Source ID: `computeSourceId(name, lang, type)` → stable `Long` hash

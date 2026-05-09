# Active context

Last updated: 2026-05-08 (Phase 3 complete)

## Current phase

**Phase 3** — Repository layer ✅ **COMPLETE** (code on disk, androidTests pending emulator infra)

All Phase 3 artifacts are untracked/uncommitted; commit them as the next step before starting Phase 4.

## What was just completed

**Phase 2 — AsuraScans source plugin** (committed earlier)

- `sources/asurascans/AsuraScans.kt` — HtmlSource with real CSS selectors for popular, search, series detail, chapter pages
- HTML fixtures under `src/test/resources/fixtures/asurascans/`
- `AsuraScansTest.kt` — parser tests with MockEngine + HTML fixtures
- Registered in `core/di/SourceModule.kt`

**Phase 3 — Repository layer (T1–T13)**

- **Entities** (`data/local/database/entities/`): `SeriesEntity`, `ChapterEntity`, `DownloadQueueEntity`
- **DAOs** (`data/local/database/dao/`): `SeriesDao`, `ChapterDao`, `DownloadQueueDao` (Flow-based, suspend functions)
- **Mappers** (`data/local/database/mappers/`): bidirectional entity ↔ domain mappers for all three entities
- **AppDatabase** v1: Room schema export enabled, 3 DAOs registered
- **Domain interfaces** (`domain/`): `SeriesRepository`, `ChapterRepository`
- **Repository impls** (`data/repository/`): `SeriesRepositoryImpl`, `ChapterRepositoryImpl`
- **Domain model**: `ChapterWithState`
- **Hilt DI modules** (`core/di/`): `DatabaseModule`, `NetworkModule`, `RepositoryModule`
- **Fakes** (`test/.../fakes/`): `FakeSeriesRepository`, `FakeChapterRepository`
- **DAO tests** (`androidTest/.../dao/`): `SeriesDaoTest` (11 tests), `ChapterDaoTest` (13 tests), `DownloadQueueDaoTest` (10 tests)
- **Migration test** (`androidTest/.../database/`): `MigrationTest` — v1 sanity checks + `migrate1To2()` template
- **Unit tests** (`test/.../data/`): `SeriesRepositoryImplTest`, `ChapterRepositoryImplTest`

### Emulator blocker

`libpulse.so.0` is missing on this WSL instance — the AVD won't start.
To unblock androidTests (T12 DAO tests + T13 MigrationTest):

```bash
sudo apt-get install -y libpulse0
```

Until then, androidTests are written and sound but cannot be executed locally.

## What's next

**Phase 4 — ViewModels**

1. `ui/library/LibraryViewModel.kt` + `LibraryViewModelTest.kt`
2. `ui/browse/BrowseViewModel.kt` + `BrowseViewModelTest.kt`
3. `ui/series/SeriesViewModel.kt` + `SeriesViewModelTest.kt`
4. `ui/reader/novel/NovelReaderViewModel.kt` + `NovelReaderViewModelTest.kt`
5. `ui/reader/manhwa/MangaReaderViewModel.kt` + `MangaReaderViewModelTest.kt`
6. `ui/downloads/DownloadsViewModel.kt` + `DownloadsViewModelTest.kt`
7. `ui/settings/SettingsViewModel.kt` + `SettingsViewModelTest.kt`

Fakes are ready (`FakeSeriesRepository`, `FakeChapterRepository`). ViewModels use `MainDispatcherRule`.

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

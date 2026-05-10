# Progress

Phase-by-phase implementation status. Checked = done, empty = planned, ~ = in progress.

---

## Phase 0 — Test infrastructure setup

- [x] Add test version entries and library declarations to `gradle/libs.versions.toml`
- [x] Update `app/build.gradle.kts` with test dependencies and room schema config
- [x] Create test directory structure (`testutil/`, `fakes/`, `fixtures/`)
- [x] Create `testutil/MainDispatcherRule.kt`
- [x] Create `testutil/KtorMockHelpers.kt`
- [x] Fix Ktor 3.0.1 mock engine API compatibility
- [x] Fix AGP 9.x Kotlin source sets compatibility (`gradle.properties`)
- [x] Fix themes (Material Components → platform Material)
- [x] Set up CI/CD pipeline (pre-push hook, GitHub Actions, release workflow)
- [x] Create `testutil/TestFixtures.kt` (deferred to Phase 1)

## Phase 1 — Domain models & Source contract TDD

- [x] `domain/model/Series.kt` + `SeriesTest.kt`
- [x] `domain/model/Chapter.kt` + `ChapterTest.kt`
- [x] `domain/model/SeriesPage.kt` + `SeriesPageTest.kt`
- [x] `domain/model/FilterList.kt` + `FilterListTest.kt`
- [x] `domain/model/ChapterContent.kt` + `ChapterContentTest.kt`
- [x] `domain/model/SeriesStatus.kt` + `SeriesStatusTest.kt`
- [x] `domain/model/ContentType.kt` + `ContentTypeTest.kt`
- [x] `data/source/Source.kt` + `SourceContractTest.kt`
- [x] `data/source/HtmlSource.kt` + `HtmlSourceTest.kt`
- [x] `data/source/SourceRegistry.kt`
- [x] `testutil/TestFixtures.kt`
- [x] `fakes/FakeSource.kt`
- [x] `core/util/computeSourceId.kt` (or inline in Source.kt companion)

## Phase 2 — First source plugin (AsuraScans)

- [x] `src/test/resources/fixtures/asurascans/popular.html`
- [x] `src/test/resources/fixtures/asurascans/popular_empty.html`
- [x] `src/test/resources/fixtures/asurascans/search.html`
- [x] `src/test/resources/fixtures/asurascans/series.html`
- [x] `src/test/resources/fixtures/asurascans/chapter.html`
- [x] `sources/asurascans/AsuraScans.kt`
- [x] `sources/asurascans/AsuraScansTest.kt`
- [x] Register in `core/di/SourceModule.kt`

## Phase 3 — Repository layer

- [x] `domain/SeriesRepository.kt` (interface)
- [x] `domain/ChapterRepository.kt` (interface)
- [x] `data/repository/SeriesRepositoryImpl.kt`
- [x] `data/repository/ChapterRepositoryImpl.kt`
- [x] `fakes/FakeSeriesRepository.kt`
- [x] `fakes/FakeChapterRepository.kt`
- [x] `data/local/database/entities/SeriesEntity.kt`
- [x] `data/local/database/entities/ChapterEntity.kt`
- [x] `data/local/database/entities/DownloadQueueEntity.kt`
- [x] `data/local/database/dao/SeriesDao.kt`
- [x] `data/local/database/dao/ChapterDao.kt`
- [x] `data/local/database/dao/DownloadQueueDao.kt`
- [x] `data/local/database/mappers/` (bidirectional entity ↔ domain mappers)
- [x] `data/local/database/AppDatabase.kt` (v1, schema export enabled)
- [x] `core/di/DatabaseModule.kt`
- [x] `core/di/NetworkModule.kt`
- [x] `core/di/RepositoryModule.kt`
- [x] `domain/model/ChapterWithState.kt`
- [x] `data/repository/SeriesRepositoryImplTest.kt`
- [x] `data/repository/ChapterRepositoryImplTest.kt`
- [x] `androidTest/.../dao/SeriesDaoTest.kt` (11 tests — pending emulator)
- [x] `androidTest/.../dao/ChapterDaoTest.kt` (13 tests — pending emulator)
- [x] `androidTest/.../dao/DownloadQueueDaoTest.kt` (10 tests — pending emulator)
- [x] `androidTest/.../database/MigrationTest.kt` (pending emulator)

## Phase 4 — ViewModels

- [x] `ui/library/LibraryViewModel.kt` + `LibraryViewModelTest.kt`
- [x] `ui/browse/BrowseViewModel.kt` + `BrowseViewModelTest.kt`
- [x] `ui/series/SeriesViewModel.kt` + `SeriesViewModelTest.kt`
- [x] `ui/reader/novel/NovelReaderViewModel.kt` + `NovelReaderViewModelTest.kt`
- [x] `ui/reader/manhwa/MangaReaderViewModel.kt` + `MangaReaderViewModelTest.kt`
- [x] `ui/downloads/DownloadsViewModel.kt` + `DownloadsViewModelTest.kt`
- [x] `ui/settings/SettingsViewModel.kt` + `SettingsViewModelTest.kt`

## Phase 5 — Compose Content UI tests

- [x] `LibraryContentTest.kt` (androidTest)
- [x] `BrowseContentTest.kt` (androidTest)
- [x] `SeriesContentTest.kt` (androidTest)
- [x] `NovelReaderContentTest.kt` (androidTest)
- [x] `MangaReaderContentTest.kt` (androidTest)
- [x] `ComponentsTest.kt` (androidTest)

## Phase 6 — Worker tests

- [x] `workers/ChapterDownloadWorker.kt`
- [x] `workers/LibraryUpdateWorker.kt`
- [x] `ChapterDownloadWorkerTest.kt` (androidTest)
- [x] `data/local/filesystem/DownloadStore.kt` (interface; domain-safe, no Android/Ktor imports)
- [x] `fakes/FakeDownloadStore.kt`
- [x] `ChapterRepository` + `DownloadRepository` extended with `findByUrl`, `markDownloaded`, `updateQueueState`
- [x] `App.kt` wired as `Configuration.Provider`; `WorkManagerInitializer` removed from manifest

## Phase 7 — Edge cases & regression safety

- [x] `core/util/Hashing.kt` + `HashingTest.kt`
- [x] `data/local/filesystem/DownloadStoreImpl.kt` + `DownloadStoreImplTest.kt`
- [x] `data/local/prefs/DataStoreExt.kt` (internal `settingsDataStore` extension)
- [x] `data/local/prefs/SettingsStore.kt` + `SettingsStoreTest.kt`
- [x] `core/di/FilesystemModule.kt` (binds `DownloadStore → DownloadStoreImpl`)
- [x] `core/di/PrefsModule.kt` (provides `DataStore<Preferences>`)

## Phase 8 — Android CLI integration & journey tests

- [x] `avd-config.json` (shared AVD config for pipeline + agents)
- [x] `scripts/emulator` (AVD lifecycle: create, start, stop, list, delete)
- [x] `scripts/run-journeys` (journey listing + agent execution guidance)
- [x] `.github/workflows/journey.yml` (manual-trigger CI for journey tests)
- [x] `journeys/library.xml`, `journeys/browse.xml`, `journeys/series.xml`
- [x] `journeys/README.md` (format docs, agent execution instructions)
- [x] `scripts/setup-wsl.sh` updated to use `android sdk install`
- [x] `scripts/ci-check` updated with optional journey step
- [x] `AGENTS.md` §15 (when/how agents load android-cli skill)
- [x] `ui/AGENTS.md` + `sources/AGENTS.md` updated with skill references
- [x] `scripts/emulator` + `run-journeys` made executable (`chmod +x`)

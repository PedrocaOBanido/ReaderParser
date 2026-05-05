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

## Phase 2 — First source plugin (example manhwa)

- [ ] `src/test/resources/fixtures/examplemanhwa/popular.html`
- [ ] `src/test/resources/fixtures/examplemanhwa/popular_empty.html`
- [ ] `src/test/resources/fixtures/examplemanhwa/search.html`
- [ ] `src/test/resources/fixtures/examplemanhwa/series.html`
- [ ] `src/test/resources/fixtures/examplemanhwa/chapter.html`
- [ ] `sources/examplemanhwa/ExampleManhwa.kt`
- [ ] `sources/examplemanhwa/ExampleManhwaTest.kt`
- [ ] Register in `core/di/SourceModule.kt`

## Phase 3 — Repository layer

- [ ] `data/repository/SeriesRepository.kt` (interface)
- [ ] `data/repository/ChapterRepository.kt` (interface)
- [ ] `data/repository/SeriesRepositoryImpl.kt`
- [ ] `data/repository/ChapterRepositoryImpl.kt`
- [ ] `fakes/FakeSeriesRepository.kt`
- [ ] `fakes/FakeChapterRepository.kt`
- [ ] `fakes/TestDatabase.kt` (in-memory Room for JVM tests)
- [ ] `data/local/database/entities/` (Room entities)
- [ ] `data/local/database/dao/` (Room DAOs)
- [ ] `data/local/database/mappers/` (entity ↔ domain mappers)
- [ ] `data/local/database/AppDatabase.kt`
- [ ] `SeriesRepositoryImplTest.kt`
- [ ] `ChapterRepositoryImplTest.kt`
- [ ] `MigrationTest.kt` (androidTest)

## Phase 4 — ViewModels

- [ ] `ui/library/LibraryViewModel.kt` + `LibraryViewModelTest.kt`
- [ ] `ui/browse/BrowseViewModel.kt` + `BrowseViewModelTest.kt`
- [ ] `ui/series/SeriesViewModel.kt` + `SeriesViewModelTest.kt`
- [ ] `ui/reader/novel/NovelReaderViewModel.kt` + `NovelReaderViewModelTest.kt`
- [ ] `ui/reader/manhwa/MangaReaderViewModel.kt` + `MangaReaderViewModelTest.kt`
- [ ] `ui/downloads/DownloadsViewModel.kt` + `DownloadsViewModelTest.kt`
- [ ] `ui/settings/SettingsViewModel.kt` + `SettingsViewModelTest.kt`

## Phase 5 — Compose Content UI tests

- [ ] `LibraryContentTest.kt` (androidTest)
- [ ] `BrowseContentTest.kt` (androidTest)
- [ ] `SeriesContentTest.kt` (androidTest)
- [ ] `NovelReaderContentTest.kt` (androidTest)
- [ ] `MangaReaderContentTest.kt` (androidTest)
- [ ] `ComponentsTest.kt` (androidTest)

## Phase 6 — Worker tests

- [ ] `workers/ChapterDownloadWorker.kt`
- [ ] `workers/LibraryUpdateWorker.kt`
- [ ] `ChapterDownloadWorkerTest.kt` (androidTest)

## Phase 7 — Edge cases & regression safety

- [ ] `data/local/filesystem/DownloadStore.kt` + `DownloadStoreTest.kt`
- [ ] `data/local/prefs/SettingsStore.kt` + `SettingsStoreTest.kt`
- [ ] `core/util/HashingTest.kt`

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

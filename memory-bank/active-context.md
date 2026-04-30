# Active context

Last updated: 2026-04-29 (after Phase 0)

## Current phase

**Phase 1** — Domain models & Source contract TDD (not yet started)

## What was just completed

**Phase 0 — Test infrastructure setup**

- Added 6 new test library entries to `gradle/libs.versions.toml` (`kotlinx-coroutines-test`, `turbine`, `room-testing`, `work-testing`, `ktor-client-mock`, `truth`)
- Added 2 new version refs (`turbine = "1.2.0"`, `truth = "1.4.4"`)
- Wired all test deps in `app/build.gradle.kts` (testImplementation + androidTestImplementation)
- Set `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` for Room schema export
- Created test directory structure:
  - `app/src/test/java/.../testutil/` — shared test utilities
  - `app/src/test/java/.../fakes/` — fake implementations (reserved, empty)
  - `app/src/test/resources/fixtures/examplemanhwa/` — HTML fixtures (reserved, empty)
- Created `testutil/MainDispatcherRule.kt` — JUnit TestRule replacing Dispatchers.Main
- Created `testutil/KtorMockHelpers.kt` — `mockHttpClient()`, `readFixture()`, `respondHtml()`
- Created `memory-bank/` directory with 7 context files

## What's next

Phase 1 — Domain models & Source contract:
1. Write tests first for all domain types (`SeriesTest`, `ChapterTest`, `SeriesPageTest`, `FilterListTest`, `ChapterContentTest`, `SeriesStatusTest`, `ContentTypeTest`)
2. Write `SourceContractTest` and `HtmlSourceTest` using `MockEngine`
3. Implement domain models to make tests pass
4. Implement `Source` interface, `HtmlSource` base class, `SourceRegistry`, `computeSourceId()`
5. Create `TestFixtures.kt` with factory functions for test data
6. Create `FakeSource.kt` for future repository tests

## Known blockers

None.

## Active conventions in play

- TDD: tests first, then production code (red → green → refactor)
- No `runBlocking` — use `runTest` from `kotlinx-coroutines-test`
- Hand-rolled fakes for interfaces we control (`Source`, repositories) — not Mockito/MockK
- `com.opus.novelparser` is the package (not `com.example.reader` as in architecture examples)
- Entities/DAOs/migrations go under `data/local/database/` subpackages (dao, entities, mappers, migrations)

## Pending decisions

- Which manhwa site will be the first concrete source for Phase 2?
- Whether to create `src/test/kotlin/` directory or keep using `src/test/java/` (currently java/ is used)

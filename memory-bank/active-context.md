# Active context

Last updated: 2026-05-04 (Phase 1 complete)

## Current phase

**Phase 1** — Domain models & Source contract ✅ **COMPLETE**

- 8 domain models: ContentType, SeriesStatus, ChapterContent, Filter, FilterList, Series, Chapter, SeriesPage
- Source interface, HtmlSource base class (Ktor+Jsoup), SourceRegistry, computeSourceId utility
- Test infrastructure: TestFixtures (5 factory functions), FakeSource (hand-rolled, configurable + call recording)
- 59 tests across 12 test suites, all green
- Test sources migrated from `java/` to `kotlin/` directory
- `domain-author` subagent added, orchestrator task dispatch made runtime-driven

## What was just completed

**Phase 1 — Domain models & Source contract TDD (a9467c9)**

- 8 immutable domain data classes matching architecture.md §3.2 exactly
- `ChapterContent` sealed interface with exactly two variants (Text, Pages)
- `Source` interface with 5 properties + 6 suspend functions + `supports(filter)` default
- `HtmlSource` abstract base class (161 lines) with Ktor+Jsoup integration and 15 MockEngine tests
- `SourceRegistry` wrapping `Map<Long, Source>` with `get(id)` and `all()`
- `computeSourceId()` pure function using `hashCode().toLong() and 0xFFFFFFFFL`
- Test infrastructure: `TestFixtures.kt` + `FakeSource.kt` with call recording
- Zero Android dependencies in domain layer, zero wildcard imports

## What's next

Phase 2 — First source plugin (example manhwa):
1. Choose a manhwa site for the first concrete source
2. Scrape and save HTML fixtures (popular, search, series detail, chapter)
3. Implement `ExampleManhwa` extending `HtmlSource` with real CSS selectors
4. Write parser tests using MockEngine + HTML fixtures
5. Register in `core/di/SourceModule.kt`

## Known blockers

None. Phase 2 awaits user decision on which manhwa site to implement first.

## Active conventions in play

- TDD: tests first, then production code (red → green → refactor)
- No `runBlocking` — use `runTest` from `kotlinx-coroutines-test`
- Hand-rolled fakes for interfaces we control — not Mockito/MockK
- `com.opus.readerparser` package
- Test sources in `app/src/test/kotlin/`
- Commit prefixes: `feat:` `fix:` `refactor:` `ci:` `cd:` `docs:`
- Git for local ops, `gh` CLI for remote

## Pending decisions

- Which manhwa site will be the first concrete source for Phase 2?

# Active context

Last updated: 2026-05-01 (Android CLI integration)

## Current phase

**Phase 8** — Android CLI integration (in progress)

- `android-cli` skill integrated into pipeline and agents
- `scripts/emulator` + `scripts/run-journeys` created for AVD + journey management
- `journeys/` directory with 3 initial XML specs (`library.xml`, `browse.xml`, `series.xml`)
- `AGENTS.md` §15 documents when/how agents load the skill
- `memory-bank/` files updated to reflect Phase 8
- TODO: `chmod +x` the new scripts

## What was just completed

**Phase 0 — Test infrastructure setup (finalized)**

- Test utilities fixed for Ktor 3.0.1 mock engine API and AGP 9.x compatibility
- Build compiles (`assembleDebug`), unit tests pass, lint passes
- WSL environment bootstrapped (JDK 17 + Android SDK via `scripts/setup-wsl.sh`)

**CI/CD pipeline**

- Local pre-push hook: `scripts/pre-push` (unit tests, optional instrumented)
- Manual full check: `scripts/ci-check` (lint + unit + instrumented + assemble)
- GitHub Actions CI: `.github/workflows/ci.yml` (push/PR → lint → test → assemble → upload)
- GitHub Actions Release: `.github/workflows/release.yml` (tag v* → build → GitHub Release)
- Conditional signing: `app/build.gradle.kts` reads `KEYSTORE_*` env vars, no-op without them
- Commit conventions: see `memory-bank/commit-conventions.md`

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
- `com.opus.readerparser` is the package (not `com.example.reader` as in architecture examples)
- Entities/DAOs/migrations go under `data/local/database/` subpackages (dao, entities, mappers, migrations)
- Commit prefixes: `feat:` `fix:` `refactor:` `ci:` `cd:` `docs:` — see `memory-bank/commit-conventions.md`

## Pending decisions

- Which manhwa site will be the first concrete source for Phase 2?
- Whether to create `src/test/kotlin/` directory or keep using `src/test/java/` (currently java/ is used)

# AGENTS.md

ReaderParser is a personal Android app that reads webnovel and manhwa sites
through site-specific `Source` plugins. UI is Jetpack Compose; networking is
Ktor.

Use this file for repo-specific rules and routing only. The global persona owns
delegation/tool discipline. Read `architecture.md` when changing layering or
contracts. Read `codemap.md` only for unfamiliar or multi-layer work.

## Non-negotiables

1. Domain code has **zero** Android, Room, Compose, or Ktor dependencies.
2. Ktor calls live only inside `Source` implementations or repositories.
3. ViewModels never reference `SourceRegistry` or concrete `Source`s.
4. Novel and manhwa readers stay separate screens.
5. `ChapterContent` stays a sealed interface with exactly `Text(html)` and
   `Pages(imageUrls)`.
6. Domain models are immutable `data class`es.
7. No `runBlocking` in production code.
8. `*Screen` wires the ViewModel; `*Content` is the stateless preview target.
9. Series/chapter identity is `(sourceId, url)`.
10. Downloads stay in app-private storage (`context.filesDir`).

## Important paths

Main code lives under `app/src/main/java/com/opus/readerparser/`.

| Area | Path |
| --- | --- |
| Domain models / use cases | `domain/` |
| Repositories | `data/repository/` |
| Source contract / base classes | `data/source/` |
| Room / DAO / migrations | `data/local/database/` |
| Filesystem / prefs | `data/local/filesystem/`, `data/local/prefs/` |
| DI modules | `core/di/` |
| Site plugins | `sources/` |
| UI screens / navigation / theme | `ui/` |
| Tests | `app/src/test/`, `app/src/androidTest/` |

## Read the nearest local rules

- `app/src/main/java/com/opus/readerparser/ui/AGENTS.md` for Compose screens.
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md` for new source
  plugins.
- `app/src/main/java/com/opus/readerparser/data/source/AGENTS.md` for the
  `Source` contract.
- `app/src/main/java/com/opus/readerparser/data/local/database/AGENTS.md` for
  Room changes.

## Project specialists

These extend the global orchestrator persona with repo-specific lanes:

- `source-author` — new site plugins + fixtures/tests.
- `screen-author` — four-file Compose screen scaffolding.
- `room-migration` — Room schema changes + migration tests.
- `domain-author` — domain / contract work.
- `runner` — build, lint, test, formatter verification.
- `reviewer` — read-only diff review.
- `journey-runner` — emulator / APK / journey XML execution.

Prefer these specialists over stuffing repo-specific workflow into the root
prompt.

## Placement rules

- New source: `sources/<sitename>/<SiteName>.kt`
- Repository contract: `domain/` interface + `data/repository/` impl
- Room entities / DAO / migrations: `data/local/database/`
- Network / JSON / cookies: `data/network/`
- Hilt modules: `core/di/`
- Reusable composables: `ui/components/`
- New screen: `ui/<screen>/` with exactly `*Screen.kt`, `*Content.kt`,
  `*ViewModel.kt`, `*UiState.kt`

## Testing and verification

New code ships with tests.

- Sources: JVM tests with saved HTML fixtures + `MockEngine`
- Repositories / ViewModels: JVM tests with hand-rolled fakes
- Room changes: migration tests
- `*Content` composables: Compose UI tests

Verification commands:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:detekt       # if configured
./gradlew :app:ktlintCheck  # if configured
```

Do not silence lint/test failures to get green.

## GitHub and Android tools

- Use `git` for local history, diff, staging, commits, push, and pull.
- Use `gh` for GitHub objects: PRs, issues, releases, workflow runs, checks.
- Prefer CLI over a GitHub MCP unless structured remote access is clearly
  worth the extra tokens.
- Load `android-cli` only for Android docs, emulator, device, APK, or journey
  work. Keep CLI manuals out of always-loaded prompts.

## Ask before doing

Ask before:

- changing the `Source` interface
- changing entity identity / PK / FK behavior
- adding a new top-level layer or module
- adding a manifest permission
- adding a new third-party dependency
- replacing Hilt or Ktor's engine

Routine work such as a new source, screen, repository method, or migration can
proceed without a separate approval gate.

## Out of scope

- No sync/accounts/cloud.
- No content hosting/redistribution.
- No multi-user behavior.
- No general-purpose browser shell.
- No iOS/desktop/web unless explicitly requested.

## Memory Bank

Task start:

- Always read `memory-bank/activeContext.md`.
- Always read `memory-bank/progress.md`.
- Lazy-load `memory-bank/projectbrief.md`,
  `memory-bank/productContext.md`, `memory-bank/systemPatterns.md`, and
  `memory-bank/techContext.md` only when the task needs them.
- The parent/orchestrator owns memory-bank reads and writes. Specialists get a
  short summary and, only when needed, a single relevant memory file.

Task end:

- Update `memory-bank/activeContext.md` with current state, active decisions,
  next step, and last updated date.
- Update `memory-bank/progress.md` with completed, in progress, blocked, known
  issues, and verification commands.
- Update the stable memory files only when durable product, architecture, or
  tooling knowledge changed.

## Context retrieval rule

- Start with the directly referenced file, command, or test.
- Read the nearest relevant `AGENTS.md` before broad exploration.
- Read `memory-bank/activeContext.md` and `memory-bank/progress.md` at task
  start.
- Read `memory-bank/projectbrief.md`, `memory-bank/productContext.md`,
  `memory-bank/systemPatterns.md`, or `memory-bank/techContext.md` only when
  task-relevant.
- Read `architecture.md` when changing layers, contracts, or core data flow.
- Read `codemap.md` only for unfamiliar or multi-layer work.
- Do not load the whole `memory-bank/` by default.
- Do not load whole-repo docs by default.

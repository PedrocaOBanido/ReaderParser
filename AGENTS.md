# AGENTS.md

ReaderParser is a personal Android app that reads webnovel and manhwa sites
through site-specific `Source` plugins. UI is Jetpack Compose; networking is
Ktor.

Use this file for repo-specific rules and routing only. The global persona owns
delegation/tool discipline.

- Read `architecture.md` for layer rules, contracts, invariants, and
  architectural decisions.
- Read `codemap.md` for current repository structure, entry points, and
  directory maps.
- Read a folder's `codemap.md` for local implementation details inside that
  area.

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
- `integrator` — git staging, commit, branch, push, PR creation, CI watch, merge-on-green.

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

Test utilities:

- `app/src/test/kotlin/com/opus/readerparser/testutil/MainDispatcherRule.kt`
- `app/src/test/kotlin/com/opus/readerparser/testutil/KtorMockHelpers.kt`
- `app/src/test/kotlin/com/opus/readerparser/testutil/TestFixtures.kt`
- `app/src/androidTest/java/com/opus/readerparser/testutil/FakeCoilRule.kt`

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

## Commit conventions

Every commit uses one of the prefixes below. A commit never mixes prefixes.

| Prefix | When to use |
|---|---|
| `feat:` | New file, new screen, new source plugin, new capability |
| `fix:` | Bug fix to previously committed code (compile errors, crashes, wrong behavior) |
| `refactor:` | Restructuring committed code without changing behavior |
| `ci:` | CI pipeline, pre-push hooks, test scripts, Gradle verification tasks |
| `cd:` | Release pipeline, signing config, deployment scripts, environment bootstrap |
| `docs:` | `AGENTS.md`, `architecture.md`, `README.md`, `openspec/`, KDoc |

Rules:

1. **A fix only exists relative to a prior commit.** If a `feat:` commit
   introduces code that doesn't compile, the compilation fix is part of that
   same `feat:` commit — it was never committed broken.
2. **Refactors don't change behavior.** If you improve code structure and add a
   feature in the same commit, it's a `feat:`. If you fix a bug while
   restructuring, it's a `fix:`.
3. **CI vs CD.** CI covers quality gates (test, lint, assemble). CD covers
   delivery (signing, release, artifact upload). When in doubt, prefer `ci:` for
   automation that runs on every push and `cd:` for automation that publishes.
4. **One commit = one verb.** If the diff adds a new screen *and* its tests,
   that's one `feat:` commit. If the diff adds a screen *and* fixes a database
   migration bug, that's two commits: `feat:` then `fix:`.
5. **Subject line:** imperative mood, present tense, ≤ 72 characters. Body
   explains *why*, not *what*.

## Workflow

Non-trivial changes go through the OpenSpec workflow:

- **Propose** → `openspec propose` or the `/openspec-propose` skill creates a
  change directory with `proposal.md`.
- **Design** → `openspec design` adds `design.md` with decisions and trade-offs.
- **Specs** → delta specs go under `openspec/changes/<name>/specs/`.
- **Tasks** → `openspec tasks` produces `tasks.md` with tracked checkboxes.
- **Implement** → the `/openspec-apply-change` skill executes tasks, marking
  checkboxes as work completes.
- **Archive** → when all tasks pass, the change is archived and durable outcomes
  are synced into canonical docs and main specs.

Trivial/read-only work proceeds directly: answering questions, read-only
exploration, single-file cosmetic fixes, dependency version bumps, and
CI/tooling config edits with no behavioral change. The
`repository-governance` spec at `openspec/specs/repository-governance/spec.md`
defines the full policy.

## Out of scope

- No sync/accounts/cloud.
- No content hosting/redistribution.
- No multi-user behavior.
- No general-purpose browser shell.
- No iOS/desktop/web unless explicitly requested.

## Context retrieval rule

- Start with the directly referenced file, command, or test.
- Read the nearest relevant `AGENTS.md` before broad exploration.
- Read `architecture.md` when changing layers, contracts, invariants, or core
  data flow.
- Read `codemap.md` when you need the current structure, entry points, or a
  repository-wide navigation map.
- Read the nearest folder `codemap.md` before broad edits inside that area.
- Do not load whole-repo docs by default.

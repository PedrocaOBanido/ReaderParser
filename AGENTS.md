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

<!-- headroom:learn:start -->
## Headroom Learned Patterns
*Auto-generated by `headroom learn` on 2026-07-08 — do not edit manually*

### Tool Output Guardrails
*~1,400,896 tokens/session saved*
- When a file/tool read is needed, fetch the exact file or range once with enough output budget; do not repeat generic `read: ?`, `edit: ?`, or `write: ?` calls to recover truncated context.
- For very large files, use targeted `rg`, `sed -n`, or line ranges instead of re-reading whole files repeatedly.
- Batch related file edits into coherent `apply_patch` hunks; avoid long sequences of tiny patch/edit/write calls for the same feature area.

### Git Inspection
*~60,000 tokens/session saved*
- Capture git state once per phase with `git status -sb`, `git diff --stat`, `git diff --name-only`, and `git log --oneline -10`; avoid repeating unchanged `git status`, `git diff`, and `git log` commands.
- Never run raw `git log` without `--oneline -N` or another bound; it returns tens of thousands of bytes in this repo.
- After PR merge with `--delete-branch`, do not also push-delete the same remote branch; fetch/prune and verify PR state instead.

### Gradle Verification
*~26,718 tokens/session saved*
- Use `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew ... --console=plain` for Android verification.
- On Gradle failures, capture full output once or inspect the generated report/XML; avoid loops of `./gradlew :app:assembleDebug`, `:app:compileDebugAndroidTestKotlin`, or `... | tail -20`.
- For targeted JVM test loops such as `LibraryViewModelTest`, run once with full output/report lookup before patching and rerunning.

### Todo Updates
*~7,400 tokens/session saved*
- Do not rewrite the same todo list after every small step; update todos only when a status actually changes or the plan materially changes.
- Repeated todo loops occurred for OpenSpec apply/archive, headroom learn, GitHub repo creation, release workflow, worktree setup, adb setup, and project graph checks.
- If a checklist is stable, keep it in one task/status message and only emit the delta.

### Search Scope
*~7,200 tokens/session saved*
- Avoid repo-wide `Glob: **/*`; use `rg --files` or scoped globs such as `app/src/main/java/.../*.kt`, `openspec/changes/<name>/**`, or the directly referenced path.
- For local rules, check the known nearest `AGENTS.md` path directly instead of repeating `**/AGENTS.md`.
- For repeated keyword probes like `release`, `headroom`, or `headroom|learn`, use one scoped `rg -n "pattern" <known dirs>` instead of many broad grep variants.

### OpenSpec Commands
*~5,100 tokens/session saved*
- Use `openspec list --json` once, then `openspec status --change <name> --json`; do not poll the same change status repeatedly unless files changed.
- Before `openspec instructions ... --change <name>`, confirm the change exists and schema validates; `openspec changes` is invalid, use `openspec change` or `openspec list`.
- For house-style smoke tests, validate `schema.yaml` metadata and template paths before running `openspec instructions proposal|plan`; bad schema/template paths caused repeated failures.

### Samsung Search Tests
*~4,500 tokens/session saved*
- JVM unit tests that touch Android framework classes like `android.util.Log`, `Uri`, `ContentResolver`, or `ContentValues` can fail without Robolectric/default values; move those cases to `androidTest` or isolate Android calls behind fakes.
- For WorkManager androidTest code, inspect the installed `work-testing` AAR/JAR API once before guessing `TestListenableWorkerBuilder` signatures.
- When `SearchIndexSyncerTest` fails on `android.util.Log`, stop rerunning JVM tests and either fake logging/platform calls or move the test to Android instrumentation.

### RTK Commands
*~3,159 tokens/session saved*
- Do not rerun `rtk init --show`, `rtk git status --short --branch`, `rtk git log --oneline -10`, or `rtk adb devices` during the same phase unless config/history/device state changed.
- `rtk pytest` may fail when `pytest` is absent; for simple scratch Python tests use `python3 -m unittest discover -s tests -v`.
- `rtk ruby` is not available here; validate YAML with `python3 -c 'import yaml; yaml.safe_load(open(path))'`.

### Release Workflow
*~3,159 tokens/session saved*
- Release workflow YAML validation should use `python3`/PyYAML; `ruby` and `rtk ruby` are unavailable in this environment.
- For release-note research, inspect `.github/workflows/release.yml` and bounded `gh release/pr` output once instead of repeated broad `release` grep searches.

### Python Runtime
*~2,100 tokens/session saved*
- Use `python3`, not `python`; this environment repeatedly returned `zsh: command not found: python`.
- For `headroom-ai` internals, use `/home/pedro/.local/share/pipx/venvs/headroom-ai/bin/python` because system `python3` may not import `headroom`.
- In scratch smoke repos, run `python3 -m unittest discover -s tests -v` directly and only rerun after a code/test change.

### Headroom Learn
*~1,700 tokens/session saved*
- `headroom learn` should prefer the Codex backend; OpenCode is explicit-only via `HEADROOM_LEARN_CLI=opencode` and is not the default.
- Avoid repeating `headroom learn`, `headroom learn --help`, and short Python probes; inspect `headroom.learn.analyzer` once with the pipx venv Python.
- Do not fan out repeated `headroom`/`learn` repo searches; if project code has no hits in one scoped search, switch to the installed package path.

### Understand Dashboard
*~700 tokens/session saved*
- When launching the understand-anything dashboard, use `python3` in shell snippets and export `LOG_FILE`/`PID_FILE` in the same command that reads them.
- If Vite tries to spawn a Windows browser path, keep the server URL from stdout and do not retry the dashboard launch loop.
- Check an existing dashboard PID once before relaunching; avoid repeated PID-file polling when the URL is already known.

### OpenSpec House Style
*~1,526 tokens/session saved*
- For house-style schema smoke tests, fix `schema.yaml` metadata and template paths before running `openspec instructions proposal|plan`; bad schema/template paths caused repeated `smoke-house` failures.
- `openspec instructions proposal` requires `--change`; running it without a change only returns a missing-option error.

<!-- headroom:learn:end -->

# scripts/

## Responsibility

Developer tooling and CI automation surface for the ReaderParser project. Contains
five standalone shell scripts that cover environment bootstrapping, AVD lifecycle
management, pre-commit/pre-push quality gates, journey-based integration testing,
and full CI checks. None of these scripts are part of the app binary; they exist
entirely to streamline local development and CI workflows.

```
scripts/
├── ci-check          # Full local CI run (lint + unit tests + instrumented tests + assemble + journey setup)
├── emulator          # AVD lifecycle management (create / start / stop / list / delete)
├── pre-push          # Pre-push hook: unit tests (and instrumented if device present)
├── run-journeys      # Journey test runner: list, print steps, or setup emulator + install APK
└── setup-wsl.sh      # One-shot WSL environment bootstrap (JDK 17 + Android SDK + Android CLI)
```

## Design

### Guiding principles

- **Idempotency where possible.** `setup-wsl.sh` skips already-installed tools.
  `emulator create` checks for existing AVDs before creating.
- **Graceful degradation.** Scripts detect missing prerequisites (`adb`, `android`,
  AVDs) and skip instrumented tests or journey steps rather than hard-failing.
- **Single responsibility.** Each script does exactly one category of work.
  `ci-check` orchestrates multiple phases by calling `emulator` and `run-journeys`
  as sub-scripts.

### Script-by-script design

#### `ci-check`

Aggregates the full pre-PR validation pipeline into one command. Phases:

1. `:app:lintDebug` — Android lint (style, correctness, performance).
2. `:app:testDebugUnitTest` — all JVM unit tests.
3. `:app:connectedDebugAndroidTest` — instrumented tests (only if a device/emulator is
   attached; detected via `adb devices`).
4. `:app:assembleDebug` — verifies the debug APK compiles.
5. Journey test orchestration — if an AVD is available, boots the emulator,
   waits for boot completion, and runs `scripts/run-journeys` to display available
   journeys. The actual journey steps are executed by an agent using `android-cli`;
   the script handles provisioning and teardown.

The script is the canonical "did I break anything?" command for developers before
pushing.

#### `pre-push`

Designed to be installed as a Git pre-push hook (`scripts/pre-push`). Runs
unit tests unconditionally; runs instrumented tests if an emulator or physical
device is available at push time. Exits non-zero on any test failure, which
aborts the push. Requires `ANDROID_HOME` to be set (checked with a helpful
error referencing `scripts/setup-wsl.sh`).

#### `emulator`

Abstracts the `android emulator` CLI command with a fixed configuration read from
`avd-config.json` at the repo root (falls back to sensible defaults: AVD name
`readerparser-api36`, system image `system-images;android-36;google_apis;x86_64`).

| Subcommand | Action |
|---|---|
| `create` | Installs system image + creates AVD (idempotent) |
| `start` | Boots the AVD |
| `stop` | Stops the AVD |
| `list` | Lists existing AVDs |
| `delete` | Stops + removes the AVD |

The JSON config file allows CI environments to override the AVD name or image
without changing the script.

#### `run-journeys`

Journey test runner with three modes:

| Argument | Action |
|---|---|
| _(none)_ or `list` | Enumerate all `.xml` files in `journeys/`, extracting the `name` attribute from each |
| `*.xml` | Parse and print the journey's `name`, `description`, and ordered `<action>` steps for manual or agent-driven execution |
| `--setup` | Full provisioning: create AVD → start emulator → wait for boot → install debug APK |

The script does not execute touch/tap/swipe commands itself. It is explicitly
designed to output structured steps that an agent (with `skill("android-cli")`)
follows, reporting results as JSON per `journeys/README.md`. The `--setup` mode
is shared with `ci-check` for end-to-end provisioning.

#### `setup-wsl.sh`

One-shot environment bootstrap for Windows Subsystem for Linux (WSL) users.
Performs in order:

1. Install OpenJDK 17 via `apt-get` (if not already present).
2. Install the `android` CLI tool via Google's install script.
3. Create `$ANDROID_HOME` directory and install SDK packages: platform `android-36`,
   build-tools `36.0.0`, platform-tools, emulator, and the `x86_64` system image.
4. Append `ANDROID_HOME`, `JAVA_HOME`, and `PATH` entries to the user's shell RC
   file (`.zshrc`, `.bashrc`, or `.profile` — auto-detects which).

## Flow

### Developer workflow

```
Clone → run setup-wsl.sh (once)
  → ANDROID_HOME + JDK 17 + Android CLI + SDK installed
  → source shell RC

Daily:
  Edit code → ./gradlew :app:assembleDebug   // quick compile check
  Edit code → scripts/pre-push               // before pushing (runs tests)
  Before PR: scripts/ci-check                // full validation (lint + tests + assemble + optional journeys)

Agent-driven journey tests:
  scripts/run-journeys --setup                // provisions emulator + installs APK
  → agent follows printed steps with android-cli
  → scripts/emulator stop                     // clean up
```

### CI pipeline flow

```
CI trigger
  → scripts/ci-check
    → ./gradlew :app:lintDebug
    → ./gradlew :app:testDebugUnitTest
    → ./gradlew :app:connectedDebugAndroidTest   (if device present)
    → ./gradlew :app:assembleDebug
    → scripts/emulator create + start
    → scripts/run-journeys                       (if AVD configured)
    → scripts/emulator stop
```

## Integration

### Consumed by

| Script | Consumed by |
|---|---|
| `ci-check` | Developers (manual PR gate); CI pipeline entry point |
| `pre-push` | Git hooks (symlinked or copied to `.git/hooks/pre-push`) |
| `emulator` | `ci-check`, `run-journeys`, and any developer managing AVDs |
| `run-journeys` | `ci-check`; agents using `android-cli` skill |
| `setup-wsl.sh` | WSL developers setting up a fresh environment |

### Depends on

| Script | External dependencies |
|---|---|
| `ci-check` | `adb`, `android` CLI (for journey section), `./gradlew` |
| `pre-push` | `adb` (optional), `ANDROID_HOME`, `./gradlew` |
| `emulator` | `android` CLI, `python3` (for JSON config parsing) |
| `run-journeys` | `android` CLI, `adb`, `./gradlew` (for APK build in `--setup`), `python3` |
| `setup-wsl.sh` | `curl`, `sudo apt-get`, `python3`; runs on Linux/WSL only |

### Connection to build system

All scripts that run Gradle tasks (`ci-check`, `pre-push`) depend on the version
catalog at `gradle/libs.versions.toml`. Changes to the catalog (adding/removing
dependencies) may affect what these scripts compile and test, but the scripts
themselves require no modifications.

### Testing these scripts

The scripts are not unit-tested in the traditional sense. Validation happens by
running them in CI (for `ci-check` and `pre-push`) or during developer environment
setup (for `setup-wsl.sh` and `emulator`). If a script is changed, the author
should manually verify it with the relevant workflow (e.g., run `scripts/ci-check`
to verify a change to the lint/test sequence).

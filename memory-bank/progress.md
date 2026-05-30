# Progress

Last updated: 2026-05-29

## Completed

- Implemented the shared reader chapter-list fix:
  - added `ui/components/ReaderChapterListSheet.kt`
  - wired `MangaReaderScreen` and `NovelReaderScreen` to open/dismiss the sheet
    from existing `ShowChapterList` effects
  - extended both reader UiState/ViewModel pairs with current-series chapter
    list state and chapter-selection navigation through existing
    `NavigateToChapter` effects
- Added targeted coverage for the reader chapter-list flow:
  - updated `MangaReaderViewModelTest` and `NovelReaderViewModelTest`
  - updated `MangaReaderContentTest` and `NovelReaderContentTest` with
    screen-level chapter-list behavior checks
  - added `ReaderScreenTestChapterRepository` under androidTest test utilities
- Fixed androidTest API usage in `LibraryContentTest.kt` and
  `NovelReaderContentTest.kt` so the androidTest source set compiles again.
- Verification passed for:
  - `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:testDebugUnitTest --tests "*MangaReaderViewModelTest" --tests "*NovelReaderViewModelTest"`
  - `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:assembleDebugAndroidTest`
  - `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:assembleDebug`
- Saved the approved plan for the shared reader chapter-list fix at
  `plans/2026-05-29-reader-shared-chapter-list-fix.md`.
- v1.0.0 GitHub release created and published at merge commit `4b97e8a`
  (PR #15, app icon). Full release notes cover sources, screens, core features,
  and technical architecture.
- `feature/app-icon` branch deleted after merge into main.
- Added global OpenCode skill `~/.config/opencode/skills/git-sync-pr-watch/`.
- Added global OpenCode skill `~/.config/opencode/skills/android-command-routing/`.
- Fixed `.github/workflows/release.yml` for signed APK publication.
- Configured GitHub repository secrets for release APK signing.
- Historical build-out phases 0–8 are materially present in the repo: test
  infrastructure, source contract, first source plugin, repository layer,
  ViewModels, Compose screens/content, worker flows, edge-case coverage, and
  journey tooling.
- The memory-bank audit confirmed that Compose screen/content files,
  navigation, workers, and journey scripts already exist, so the old phase
  notes were stale.
- The memory-bank workflow was normalized:
  - `activeContext.md` and `progress.md` are now the core task-start files
  - `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
    `techContext.md` were added as lazy-load references
  - `AGENTS.md`, startup commands, specialist guidance, and OpenCode config now
    route through the lean memory flow
- Historical debug notes were moved out of `memory-bank/`, and duplicated rule
  files were retired from the core workflow.
- A concrete plan for splitting architecture documentation from codemap
  responsibilities was saved to
  `plans/2026-05-27-architecture-codemap-doc-split.md`.
- The documentation split is now implemented:
  - `AGENTS.md` explicitly routes doc usage by responsibility
  - `architecture.md` is a shorter normative guide focused on rules and
    contracts
  - `codemap.md` explicitly owns repository navigation and implementation maps
- Updated project-local OpenCode command workflow:
  - `/start` now drafts approval-ready plans and saves approved plans under
    `plans/`
  - `/run-plan` now executes an approved plan in an isolated subtask session
- FreeWebNovel source-onboarding planning context was reviewed and a plan was
  saved at `plans/2026-05-27-freewebnovel-source-onboarding.md`.
- FreeWebNovel source onboarding was implemented:
  - added `sources/freewebnovel/FreeWebNovel.kt`
  - registered the source in `core/di/SourceModule.kt`
  - added live-backed fixtures under `fixtures/freewebnovel/`
  - added `FreeWebNovelTest` covering parser flows and failure cases
- Generated and saved a repository knowledge graph at
  `.understand-anything/knowledge-graph.json` for commit
  `b7852189a3bff5e1165f195ac4fd5a565e1c9794`.
- Refreshed `.understand-anything/meta.json` and
  `.understand-anything/fingerprints.json`; the fingerprint baseline covers 277
  text-readable files.
- Updated `.understand-anything/.understandignore` to exclude
  `.understand-anything/` so future `/understand` runs do not self-analyze
  generated artifacts.
- Reviewer findings on FreeWebNovel were fixed:
  - detail-page status parsing now handles completed-series markup
  - latest pagination now stops on terminal pages with disabled `>>` links
  - regression tests/fixtures were added for both behaviors
- Verification passed for:
  - `./gradlew :app:testDebugUnitTest --tests "*FreeWebNovelTest"`
  - `./gradlew :app:assembleDebug`
  - reviewer re-check of the current FreeWebNovel diff

## In progress

- None.

## Blocked

- Running the targeted `connectedDebugAndroidTest` reader classes is blocked by
  the current environment having no attached device or running emulator
  (`adb devices` returned an empty list).

## Known issues

- OpenCode must be restarted before newly added skills or updated project
  command files are available in future sessions.
- The generated knowledge graph validated without critical issues, but it still
  reports orphan warnings for some binary assets, test fixtures, and standalone
  tooling files that do not participate in dependency edges.
- Build/test health should be re-verified on demand when code changes resume;
  stale blocker notes should not be carried forward without revalidation.
- Pre-existing environment warnings remain during Gradle runs:
  - Kotlin JDK 25 fallback to JVM 24 target
  - Gradle deprecation warnings for Gradle 10 compatibility
- `local.properties` points at a non-existent SDK path in this environment, so
  Gradle verification currently needs `ANDROID_HOME`/`ANDROID_SDK_ROOT` set to
  `/home/pedro/Android/Sdk`.
- The release workflow fix was validated end-to-end: the `v1.0.0` release
  produced a signed `app-release.apk` (13.8 MB) after secrets were configured.

## Verification commands

- Skills/config-time files:
  - inspect `~/.config/opencode/skills/git-sync-pr-watch/SKILL.md`
  - inspect `~/.config/opencode/skills/android-command-routing/SKILL.md`
- Knowledge graph:
  - inspect `.understand-anything/knowledge-graph.json`
  - inspect `.understand-anything/meta.json`
  - inspect `.understand-anything/fingerprints.json`
  - optionally launch `/understand-dashboard`
- Docs/workflow consistency:
  - inspect `AGENTS.md`
  - inspect `architecture.md`
  - inspect `codemap.md`
  - inspect `plans/2026-05-27-architecture-codemap-doc-split.md`
  - inspect `.opencode/command/start.md`
  - inspect `.opencode/command/run-plan.md`
  - inspect `.opencode/command/light-start.md`
  - inspect `.opencode/opencode.json`
  - inspect `memory-bank/`
- Release workflow:
  - inspect `.github/workflows/release.yml`
- App verification when source or build files change:
  - `./gradlew :app:testDebugUnitTest --tests "*FreeWebNovelTest"`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:lintDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:detekt`
  - `./gradlew :app:ktlintCheck`

- Generated and integrated a new wuxia-themed app icon using `@canvas-drawer`.
- Replaced default Android Studio vector icons with adaptive PNG layers and raster fallbacks.

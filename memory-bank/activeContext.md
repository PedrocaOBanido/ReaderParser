# Active context

Last updated: 2026-05-29

## Current objective

The shared reader chapter-list fix is implemented; only device-backed
instrumentation execution remains when an emulator or physical device is
available.

## Current state

- `v1.0.0` release tag is published on GitHub at merge commit `4b97e8a`
  (PR #15, app icon merged). The GitHub release includes full release notes
  covering sources, screens, core features, and technical architecture.
- An implementation plan for the shared reader chapter-list fix was saved at
  `plans/2026-05-29-reader-shared-chapter-list-fix.md`.
- A repository knowledge graph has been generated at
  `.understand-anything/knowledge-graph.json` for commit
  `b7852189a3bff5e1165f195ac4fd5a565e1c9794`.
- `.understand-anything/meta.json` and `.understand-anything/fingerprints.json`
  were refreshed alongside the graph; the fingerprint baseline covers 277
  text-readable files.
- `feature/app-icon` branch has been deleted (merged into main).
- Local `main` is synced to `origin/main`.
- All previous infrastructure is intact: two source plugins (AsuraScans,
  FreeWebNovel), full UI screen set, background workers, and release CI.
- Release workflow produces a signed `app-release.apk` when secrets are
  present.
- Project-local OpenCode command flow now uses a plan-first `/start` plus an
  isolated `/run-plan` handoff for approved plan execution.
- The manga and novel readers now share `ui/components/ReaderChapterListSheet.kt`
  for chapter-list UI while keeping separate screens, ViewModels, and UiState
  types.
- Both reader ViewModels now retain the current series chapter list in UiState,
  and chapter selection routes back through the existing
  `NavigateToChapter` effect flow.
- Targeted reader ViewModel tests passed, and both the debug app APK and debug
  androidTest APK now build successfully.

## Active decisions

- The reader chapter-list UI should be reusable only between the manga reader
  and novel reader.
- The chapter list must show the chapters of the series currently being read.
- The shared reader sheet should stay limited to the manga and novel readers;
  no broader reader composable consolidation is planned.
- Reader chapter selection should dismiss the sheet locally in `*Screen` and
  navigate through the existing ViewModel effect channel.
- `.understand-anything/` is now excluded inside
  `.understand-anything/.understandignore` so future `/understand` runs do not
  analyze their own generated artifacts.
- Git/GitHub tasks that cross into commit or PR creation should prefer the
  `git-sync-pr-watch` skill so requested commits are pushed to `origin` and PRs
  are checked/watched for initial CI outcomes.
- Android command-line tasks that mention `adb`, emulator/device control, APK
  install/run, or Gradle install-style workflows should prefer the
  `android-command-routing` skill to decide between `android`, `adb`, and
  `./gradlew`.
- `freewebnovel.com` is onboarded as a novel source with
  `ContentType.NOVEL` and `chapterTextParse` only.
- Search uses the site's GET-compatible route
  `/search?searchkey={encodedQuery}`.
- Popular uses `/sort/most-popular` without pagination; latest uses
  `/sort/latest-release` with page-number routes, and `hasNextPage` must ignore
  disabled `javascript:void(0)` pager links.
- Chapter HTML is parsed from `#article` and cleaned of inline ad and
  watermark nodes before returning `ChapterContent.Text` HTML.
- Release publication must never proceed with an empty APK path.
- The release workflow should publish a signed APK when available and fall back
  to the unsigned APK only when that is the artifact actually produced.
- `v1.0.0` points at merge commit `4b97e8a` (`Merge pull request #15 ...`).
- `activeContext.md` and `progress.md` are the only core task-start memory
  files.
- `projectbrief.md`, `productContext.md`, `systemPatterns.md`, and
  `techContext.md` are lazy-load references.
- The parent/orchestrator owns memory-bank reads and writes.
- Specialists should receive a short summary and only the single relevant
  memory file when needed.
- `architecture.md` is the normative architecture document.
- `codemap.md` is the descriptive repository atlas.
- `AGENTS.md` routes humans and agents to the right document by need.
- Use `/start` to draft and approve a plan, save it under `plans/`, then use
  `/run-plan <plan-path>` to execute that approved plan in an isolated session.

## Known constraints

- Do not load the whole `memory-bank/` by default.
- Keep detailed investigations, one-off plans, and archival notes outside the
  core memory flow.
- Update `activeContext.md` and `progress.md` before ending a task.
- Update the stable memory files only when durable product, architecture, or
  tooling knowledge changed.
- The source selectors were derived from live HTML fixtures; refresh fixtures if
  FreeWebNovel changes its markup.

## Relevant files

- `AGENTS.md`
- `plans/2026-05-29-reader-shared-chapter-list-fix.md`
- `architecture.md`
- `codemap.md`
- `.understand-anything/knowledge-graph.json`
- `.understand-anything/meta.json`
- `.understand-anything/fingerprints.json`
- `.understand-anything/.understandignore`
- `~/.config/opencode/skills/git-sync-pr-watch/SKILL.md`
- `~/.config/opencode/skills/android-command-routing/SKILL.md`
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `.opencode/command/start.md`
- `.opencode/command/run-plan.md`
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md`
- `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/freewebnovel/FreeWebNovelTest.kt`
- `app/src/test/resources/fixtures/freewebnovel/`
- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`
- `plans/2026-05-27-freewebnovel-source-onboarding.md`
- `app/src/main/java/com/opus/readerparser/ui/components/ReaderChapterListSheet.kt`
- `app/src/main/java/com/opus/readerparser/ui/reader/manhwa/MangaReaderScreen.kt`
- `app/src/main/java/com/opus/readerparser/ui/reader/manhwa/MangaReaderViewModel.kt`
- `app/src/main/java/com/opus/readerparser/ui/reader/novel/NovelReaderScreen.kt`
- `app/src/main/java/com/opus/readerparser/ui/reader/novel/NovelReaderViewModel.kt`
- `app/src/androidTest/java/com/opus/readerparser/ui/reader/manhwa/MangaReaderContentTest.kt`
- `app/src/androidTest/java/com/opus/readerparser/ui/reader/novel/NovelReaderContentTest.kt`

## Next safe action

Start an emulator or connect a device, then run the two targeted
`connectedDebugAndroidTest` reader test classes.

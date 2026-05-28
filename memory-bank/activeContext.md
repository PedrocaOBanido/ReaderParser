# Active context

Last updated: 2026-05-27

## Current objective

v1.0.0 has been released. No immediate action required.

## Current state

- `v1.0.0` release tag is published on GitHub at merge commit `4b97e8a`
  (PR #15, app icon merged). The GitHub release includes full release notes
  covering sources, screens, core features, and technical architecture.
- `feature/app-icon` branch has been deleted (merged into main).
- Local `main` is synced to `origin/main`.
- All previous infrastructure is intact: two source plugins (AsuraScans,
  FreeWebNovel), full UI screen set, background workers, and release CI.
- Release workflow produces a signed `app-release.apk` when secrets are
  present.

## Active decisions

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
- `architecture.md`
- `codemap.md`
- `~/.config/opencode/skills/git-sync-pr-watch/SKILL.md`
- `~/.config/opencode/skills/android-command-routing/SKILL.md`
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md`
- `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/freewebnovel/FreeWebNovelTest.kt`
- `app/src/test/resources/fixtures/freewebnovel/`
- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`
- `plans/2026-05-27-freewebnovel-source-onboarding.md`

## Next safe action

None — v1.0.0 is released. Open new features or improvements as needed.

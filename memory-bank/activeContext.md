# Active context

Last updated: 2026-05-27

## Current objective

Track the freshly created `v1.0.0` release tag and hand off for any optional
release-workflow monitoring or broader QA.

## Current state

- `FreeWebNovel` was implemented as a new `HtmlSource` novel plugin under
  `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`.
- `SourceModule.kt` now registers `FreeWebNovel(client)`.
- Live-backed fixtures were added for popular, latest, search, series, and
  chapter flows under `app/src/test/resources/fixtures/freewebnovel/`.
- Reviewer follow-up fixes are in place:
  - detail-page status parsing now matches both ongoing and completed markup
  - terminal latest-page pagination ignores disabled `javascript:void(0)` `>>`
  - live-backed regression fixtures/tests were added for both cases
- `FreeWebNovelTest` was added with parser coverage for identity, popular,
  latest, terminal latest pagination, search, series details, completed detail
  status, chapter list, chapter content cleaning, and failure paths.
- Verification completed successfully for:
  - `./gradlew :app:testDebugUnitTest --tests "*FreeWebNovelTest"`
  - `./gradlew :app:assembleDebug`
  - reviewer re-check on the current uncommitted diff
- Release tag `v1.0.0` was created from the latest merged PR on `main`
  (`8c948d9`, PR #13) and pushed to `origin`.
- The documentation split plan in
  `plans/2026-05-27-architecture-codemap-doc-split.md` has been implemented.
- `architecture.md` now focuses on durable rules, contracts, invariants, and
  decisions while `codemap.md` owns live structure and implementation mapping.
- `.github/workflows/release.yml` now records whether the build is signed or
  unsigned, prefers a signed APK when present, falls back to an unsigned APK,
  and fails the workflow if no release APK is produced.

## Active decisions

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
- `v1.0.0` points at merge commit `8c948d9` (`Merge pull request #13 ...`).
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
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md`
- `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/freewebnovel/FreeWebNovelTest.kt`
- `app/src/test/resources/fixtures/freewebnovel/`
- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`
- `plans/2026-05-27-freewebnovel-source-onboarding.md`

## Next safe action

Optionally monitor the release workflow triggered by `v1.0.0`, or run broader
static-analysis verification (`lintDebug`, `detekt`, `ktlintCheck`) and a
manual in-app smoke test of latest pagination, completed-series details, and
search.

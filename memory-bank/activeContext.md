# Active context

Last updated: 2026-05-27

## Current objective

Finish the `freewebnovel.com` source-onboarding task with the new source,
fixtures, and tests in place, then hand off for any optional broader QA.

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
- The parent/orchestrator continues to own memory-bank reads and writes.

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
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md`
- `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/freewebnovel/FreeWebNovelTest.kt`
- `app/src/test/resources/fixtures/freewebnovel/`
- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`
- `plans/2026-05-27-freewebnovel-source-onboarding.md`

## Next safe action

Optionally run broader static-analysis verification (`lintDebug`, `detekt`,
`ktlintCheck`) or perform a manual in-app smoke test of latest pagination,
completed-series details, and search.

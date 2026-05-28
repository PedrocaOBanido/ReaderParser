# FreeWebNovel source onboarding plan

Saved for implementation in the next session.

## Request restatement

Onboard `freewebnovel.com` as a new novel `Source` plugin.

## Recommended execution lane

- Use `source-author` after approval.
- Reason: this is a repo-specific new-source workflow with parser, DI registration,
  fixtures, and tests.

## Scope

### Files expected to be added

- `app/src/main/java/com/opus/readerparser/sources/freewebnovel/FreeWebNovel.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/freewebnovel/FreeWebNovelTest.kt`
- `app/src/test/resources/fixtures/freewebnovel/*`

### Files expected to be updated

- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`

## Context reviewed

- `AGENTS.md`
- `memory-bank/activeContext.md`
- `memory-bank/progress.md`
- `app/src/main/java/com/opus/readerparser/sources/AGENTS.md`
- `app/src/main/java/com/opus/readerparser/data/source/HtmlSource.kt`
- `app/src/main/java/com/opus/readerparser/core/di/SourceModule.kt`
- `app/src/main/java/com/opus/readerparser/sources/asurascans/AsuraScans.kt`
- `app/src/test/kotlin/com/opus/readerparser/sources/asurascans/AsuraScansTest.kt`
- `app/src/test/kotlin/com/opus/readerparser/testutil/KtorMockHelpers.kt`

## Applicable constraints

- New source lives under `sources/<sitename>/`.
- Source file extends `HtmlSource`.
- For a novel source, override `chapterTextParse` only; do not implement
  `chapterPagesParse`.
- Register the source in `core/di/SourceModule.kt`.
- Add HTML fixtures under `app/src/test/resources/fixtures/freewebnovel/`.
- Add a `MockEngine`-backed parser test.
- Use `selectFirst(...)` with null-safety.
- Use `absUrl(...)` for links.
- Trim extracted text.
- Do not change `Source`, `HtmlSource`, or `SourceRegistry` contracts.
- Use `computeSourceId(name, lang, type)`; do not hand-pick IDs.
- Source code should throw on parse errors rather than swallow them.

## Assumptions to validate during implementation

- `freewebnovel.com` is a text-novel source, so `ContentType.NOVEL` is expected.
- The site exposes enough HTML for: popular, search, series details, chapter list,
  chapter text, and some latest/updates flow.
- Exact selectors, URL patterns, and any required headers must be derived from live
  HTML and fixtures, not invented.

## Implementation phases

### 1. Live-site reconnaissance

Inspect the live site and collect representative HTML for:

- a listing/popular page
- a search results page
- a series detail page
- a chapter page
- a latest/updates page or fallback route used for `getLatest`

Capture pagination behavior, required headers, relative-vs-absolute URLs, and the
container that holds chapter body HTML.

### 2. Source implementation

Create `FreeWebNovel.kt` with:

- computed source identity
- `baseUrl`, `lang`, and `ContentType.NOVEL`
- implementations for popular/search/details/chapter-list/latest flows
- `chapterTextParse` returning cleaned chapter HTML/text payload expected by the app
- optional per-request headers only if live-site inspection proves they are needed

### 3. Registration

Add `FreeWebNovel(client)` to `SourceModule.kt` so it is part of the registry.

### 4. Fixtures and tests

Add saved HTML fixtures and a parser test suite covering:

- identity fields
- popular parsing + next-page detection
- latest parsing
- search parsing
- series detail enrichment
- chapter list parsing
- chapter content parsing as `ChapterContent.Text`
- at least one failure path for missing required markup

### 5. Verification

Run targeted verification first:

- `./gradlew :app:testDebugUnitTest --tests "*FreeWebNovelTest"`

If the implementation touches shared build behavior or reveals broader failures,
expand to:

- `./gradlew :app:testDebugUnitTest`

## Acceptance criteria

- `freewebnovel.com` is available as a registered source in the app registry.
- The source is implemented as a novel parser with `chapterTextParse` only.
- No source-contract or architecture rule is violated.
- Fixtures exist for every covered parsing flow.
- Parser tests pass for the new source.
- No selectors, defaults, or missing fields are guessed without live HTML support.

## Clarifying question

- If you have preferred sample series/chapter URLs for fixture capture, send them;
  otherwise implementation can choose stable examples from the live site.

## Next step

Wait for approval, then dispatch the implementation to `source-author` with this
plan and a short memory summary.

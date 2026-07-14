## 1. Unified Reader state and behavior

- [x] 1.1 Add ViewModel tests that load `ChapterContent.Text` and `ChapterContent.Pages` through one Reader state while preserving route content type and chapter identity.
- [x] 1.2 Add tests for content-type mismatch recovery, loading/error clearing, Retry (including dead-chapter re-resolution), previous/next navigation, and download effects. Implement cancellation propagation and chapter selection handling.
- [x] 1.3 Add tests locking text progress clamping and distinct persistence by `(sourceId, chapterUrl)`. Implement immediate text read behavior, page-zero initialization, non-persisted page position, final-page read behavior, and progress restoration after WebView layout.
- [x] 1.4 Implement one Reader ViewModel, state, actions, and effects without changing repositories, `Source`, or `ChapterContent`.

## 2. Shared Reader content and controls

- [x] 2.1 Add Compose tests for the shared loading, retryable error, Back, previous/next, chapter-list, and download controls. Implement tap-to-toggle overlay behavior.
- [x] 2.2 Add Compose tests proving `Text` uses the text renderer with percentage progress label and `Pages` uses the image renderer from page zero with page-count progress.
- [x] 2.3 Build one Reader content composable using shared MANHWA-style overlay chrome and private content-specific renderers.
- [x] 2.4 Wire functional Back and Retry actions and ensure tap detection does not prevent text scrolling or page scrolling.

## 3. Navigation and screen consolidation

- [x] 3.1 Implement one Reader route carrying source ID, series URL, chapter URL, and content type from both series types. Preserve identity through adjacent and chapter-sheet navigation.
- [x] 3.2 Replace separate Series-screen reader callbacks and destinations with one Reader navigation path that preserves content type across chapter changes.
- [x] 3.3 Wire one Reader screen to state/effects and the existing shared chapter-list sheet.
- [x] 3.4 Remove the superseded NovelReader and MangaReader screen, content, ViewModel, state, routes, and tests after unified coverage passes.

## 4. Canonical documentation

- [x] 4.1 Update `architecture.md` to replace the separate-reader invariant and flow with one Reader screen containing separate text/page renderers.
- [x] 4.2 Update root `AGENTS.md` non-negotiables and placement guidance to match the unified Reader architecture.
- [x] 4.3 Update the appropriate codemap when the reader package and navigation entry points change.
- [x] 4.4 Confirm `download-offline-reader`, `download-enqueue`, `architecture.md`, root `AGENTS.md`, and codemap terminology all describe the unified Reader before archive.

## 5. Review and verification

- [x] 5.1 Review unified state transitions, content mismatch handling, cancellation, progress/read timing, and download/offline behavior; resolve every finding.
- [x] 5.2 Review Compose gesture interaction, accessibility descriptions, Back/Retry behavior, route replacement, and removal of duplicated implementations; resolve every finding.
- [x] 5.3 Run targeted Reader ViewModel, repository, and Compose tests for both content types.
- [x] 5.4 Run `./gradlew :app:testDebugUnitTest`.
- [x] 5.5 Run `./gradlew :app:assembleDebug`.
- [x] 5.6 Run `./gradlew :app:lintDebug`.

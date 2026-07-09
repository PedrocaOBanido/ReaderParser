## Implementation slices

### Slice A: Browse auto-load-on-scroll

**Tasks:** A.1, A.2
**Tests first:** Add or adjust a Browse UI/content test that proves reaching the end of the list triggers LoadMore automatically while preserving the manual footer fallback.
**TDD exception:** existing ViewModel guard coverage may remain as-is.

### Slice B: FreeWebNovel paginated chapter list

**Tasks:** B.1, B.2, B.3
**Tests first:** Add a focused chapter-list test with page 1 + page 2 fixtures/stubs that proves all chapters are collected before any production change.
**TDD exception:** none

### Slice C: AsuraScans latest/popular split regression

**Tasks:** C.1, C.2, C.3
**Tests first:** Add regression proof that AsuraScans latest(1) and popular(1) request distinct URLs and parse distinct results; only change production parser code if the test shows drift.
**TDD exception:** none

## Review checkpoints

 - After Slice A: review the auto-load-on-scroll test and UI wiring for minimality.
 - After Slice B: review chapter aggregation behavior for completeness and contract stability.
 - After Slice C: review whether production parser changes are actually required.

## Final verification intent

- Targeted tests for Browse UI/ViewModel, FreeWebNovelTest, and AsuraScansTest
- `./gradlew :app:testDebugUnitTest` after targeted runs are clean
- `./gradlew :app:assembleDebug` if a device APK becomes necessary

## Intended commit grouping

- Commit 1: UI auto-load-on-scroll
- Commit 2: FreeWebNovel chapter pagination
- Commit 3: AsuraScans latest/popular regression proof and any required parser fix
- Commit 4: OpenSpec verification cleanup, if needed

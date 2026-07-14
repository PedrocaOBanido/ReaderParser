## Implementation slices

### Slice 1: Unified Reader state machine

**Tasks:** 1.1–1.4
**Tests first:** Create unified Reader ViewModel tests for Text, Pages, explicit route type, one forced-network recovery from downloaded-content mismatch, retry, cancellation, chapter actions, downloads, text progress clamping/restoration/immediate-read behavior, page-zero initialization, and final-page read timing before moving duplicated ViewModel behavior into one state machine.
**TDD exception:** none

### Slice 2: Shared immersive Reader content

**Tasks:** 2.1–2.4
**Tests first:** Add Compose tests for shared chrome actions and visibility plus renderer-specific text/page output and progress labels before adapting the MANHWA overlay layout around both renderers. Verify Back and Retry callbacks before deleting old content composables.
**TDD exception:** none

### Slice 3: Route and screen migration

**Tasks:** 3.1–3.4
**Tests first:** Add destination construction and SavedStateHandle cases for NOVEL and MANHWA routes, including adjacent and chapter-sheet identity preservation, before replacing Series callbacks, NavGraph destinations, and screen wiring. Remove old reader files only after unified tests pass.
**TDD exception:** none

### Slice 4: Canonical architecture synchronization

**Tasks:** 4.1–4.4
**Tests first:** Documentation-only synchronization after implementation and route tests establish the final package and navigation shape. Completion requires unified terminology in `download-offline-reader`, `download-enqueue`, `architecture.md`, root `AGENTS.md`, and codemap before archive.
**TDD exception:** Canonical documentation has no executable behavior; review verifies it matches the implemented architecture without duplicating codemap ownership.

## Review checkpoints

- After Slice 1: Review state shape, route typing, mismatch handling, cancellation, chapter actions, and read/progress semantics; fix or explicitly disposition every finding before Slice 2.
- After Slice 2: Review gesture coexistence, accessibility, shared controls, renderer isolation, Back, Retry, and progress display; fix or explicitly disposition every finding before Slice 3.
- After Slice 3: Review navigation replacement, process-restored arguments, chapter-to-chapter routing, old-file deletion, and offline/download behavior; fix or explicitly disposition every finding before documentation synchronization.
- After Slice 4: Review `architecture.md`, `AGENTS.md`, and codemap ownership against the final diff before verification.

## Final verification intent

- Run targeted unified Reader ViewModel, `ChapterRepositoryImplTest`, navigation, SavedStateHandle, and Compose content tests for both Text and Pages.
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:testDebugUnitTest --console=plain`
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:assembleDebug --console=plain`
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:lintDebug --console=plain`
- If an emulator is available, run affected Reader Compose instrumentation tests with `:app:connectedDebugAndroidTest`.

## Intended commit grouping

- Commit 1: `refactor:` unify Reader state and content rendering.
- Commit 2: `refactor:` replace separate reader navigation routes.
- Commit 3: `docs:` document the unified Reader architecture.

## Implementation slices

### Slice 1: Source-detail title parsing

**Tasks:** 1.1–1.3
**Tests first:** In `FreeWebNovelTest` and `AsuraScansTest`, add `getSeriesDetails prefers extracted detail title`, `getSeriesDetails falls back to nonblank incoming title`, and `getSeriesDetails does not use blank incoming title` before updating each `seriesDetailsParse` implementation.
**TDD exception:** none

### Slice 2: Library blank-title repair

**Tasks:** 2.1–2.4
**Tests first:** In `SeriesRepositoryImplTest`, add successful blank-title refresh persistence and retrieval/response/parsing-failure preservation cases. In `LibraryViewModelTest`, add successful repair preserving `(sourceId, url)` and bookmark data, failure leaving the bookmark unchanged, `blank bookmark is attempted once per lifecycle`, and `blank bookmark may be retried in a new lifecycle` before wiring repair through the existing refresh and persistence flow.
**TDD exception:** none

### Slice 3: Shared adaptive series catalog

**Tasks:** 3.1–3.4
**Tests first:** Add Compose tests in `LibraryContentTest` and `BrowseContentTest` proving the shared card renders a cover and readable title, invokes the caller-provided series action, and remains interactive at compact and expanded widths before extracting the Material-theme-aware card and adaptive grid/container.
**TDD exception:** none

### Slice 4: Library catalog behavior cleanup

**Tasks:** 4.1–4.2
**Tests first:** In `LibraryContentTest`, add `Unread filter is absent` and catalog assertions retaining supported sort, text search, removal, and Samsung Search interactions before deleting the inert control and now-unused state or presentation logic.
**TDD exception:** none

### Slice 5: Browse explicit search and request handling

**Tasks:** 5.1–5.5
**Tests first:** In `BrowseViewModelTest`, add `mode or source change without submitted query makes no request`, `submitted search uses selected source and mode`, loading and retry of the submitted request with source/pagination preserved, and stale query/source/mode/page response rejection. In `BrowseContentTest`, add explicit submit, loading feedback, retry feedback, series-detail navigation, and pagination interaction cases before changing Browse state, ViewModel, and content.
**TDD exception:** none

## Review checkpoints

- After Slice 2: Review source-title parsing and Library repair for fallback correctness, identity/data preservation, lifecycle attempt limits, and use of the existing refresh/persistence path; fix or explicitly disposition every finding before Slice 3.
- After Slice 4: Review the shared adaptive UI and Library cleanup for one shared Material-aware card/grid, compact/expanded usability, removal of Unread, and preservation of Library behaviors; fix or explicitly disposition every finding before Slice 5.
- After Slice 5: Review Browse explicit submission, loading/retry, request-identity stale-response guards, navigation, pagination, and shared catalog use; fix or explicitly disposition every finding before final verification.

## Final verification intent

- Run the targeted `FreeWebNovelTest`, `AsuraScansTest`, `SeriesRepositoryImplTest`, `LibraryViewModelTest`, `BrowseViewModelTest`, `LibraryContentTest`, and `BrowseContentTest` cases added or changed by this work.
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:testDebugUnitTest --console=plain`
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:assembleDebug --console=plain`
- `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:lintDebug --console=plain`
- If an emulator is available, run the affected Compose instrumentation tests: `ANDROID_HOME=/home/pedro/Android/Sdk ANDROID_SDK_ROOT=/home/pedro/Android/Sdk ./gradlew :app:connectedDebugAndroidTest --console=plain`.

## Intended commit grouping

- Commit 1: `fix:` repair source-detail titles and existing blank Library bookmark titles.
- Commit 2: `refactor:` share the adaptive cover-first catalog UI and remove Library's inert Unread control.
- Commit 3: `refactor:` make Browse search explicit and guard request results.

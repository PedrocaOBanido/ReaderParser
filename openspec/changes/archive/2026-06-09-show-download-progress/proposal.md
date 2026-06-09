## Why

Downloads currently show queued state but not meaningful in-progress status. The `ChapterDownloadWorker` sets progress to 0f when starting and 1f when completing, with no intermediate updates. Users see a "Downloading" badge but the progress bar never moves, providing no feedback on actual download progress. This is particularly noticeable for manhwa downloads where multiple pages are downloaded sequentially.

## What Changes

- Add intermediate progress updates in `ChapterDownloadWorker` during download execution
- For manhwa: report per-page progress as each image is downloaded
- For novels: report progress after content fetch completes (single-step progress)
- Update UI status text to show percentage when in progress
- Maintain existing architecture: domain stays Android-free, repositories own orchestration

## Capabilities

### New Capabilities

- `download-progress-reporting`: Intermediate progress updates during chapter download execution, including per-page progress for manhwa and status text display

### Modified Capabilities

- `download-enqueue`: Modify the "Download progress visibility" requirement to specify intermediate progress updates rather than just initial/completed states

## Impact

- `ChapterDownloadWorker`: Add progress update calls during download loop
- `DownloadRepository`: May need additional method for granular progress updates
- `DownloadQueueDao`: Already supports progress updates via `updateStateWithError`
- `DownloadsContent.kt`: Enhance status badge to show percentage or page count
- `DownloadItem`: Already has `progress` field, no domain model changes needed
- Requires `androidx.hilt:hilt-compiler` (KSP) for `@HiltWorker` codegen — without this, WorkManager cannot instantiate the app's `@HiltWorker`s (`ChapterDownloadWorker`, `LibraryUpdateWorker`) at runtime

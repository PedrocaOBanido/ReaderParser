## 1. Worker Progress Updates

- [x] 1.1 Update `ChapterDownloadWorker` to track total pages for manhwa chapters (via `onPageDownloaded` callback)
- [x] 1.2 Add progress update callback after each manhwa page download in `writeManhwa` loop
- [x] 1.3 Add progress update call after novel content fetch/write completes (0.5)
- [x] 1.4 Add progress update call after novel content write completes (1.0 → COMPLETED)

## 2. UI Status Enhancement

- [x] 2.1 Update `StateBadge` composable to show percentage when progress > 0 and < 1
- [x] 2.2 ~~Add page count display for manhwa downloads~~ — Removed: percentage display via existing `progress: Float` satisfies the goal without requiring domain/schema changes
- [x] 2.3 Update preview data in `DownloadsContent.kt` to show intermediate progress states (already present at 0.45f)

## 3. Verification

- [x] 3.1 Test manhwa download with multiple pages shows per-page progress (new `onPageDownloaded` tests)
- [x] 3.2 Test novel download shows binary progress (0.5 → 1.0) — verified via worker logic inspection + full unit test suite passing
- [x] 3.3 Verify progress updates flow reactively from worker to UI (infrastructure unchanged; `FakeDownloadRepository.updateQueueState` records calls)
- [x] 3.4 Verify existing download functionality still works (enqueue, cancel, retry, delete) — full unit test suite green

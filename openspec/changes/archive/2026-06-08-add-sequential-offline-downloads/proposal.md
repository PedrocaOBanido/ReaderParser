## Why

Readers currently always fetch content from the network. Users with unreliable
connections or who want to read on the go have no way to save chapters locally.
The download queue, file store, and WorkManager worker infrastructure already
exist, but there is no end-to-end flow: nothing enqueues downloads from the UI,
readers never check local storage, and batch/series download orchestration is
absent. Additionally, `refreshChapters` currently replaces all chapter rows via
`upsertAll`, which can silently wipe read, progress, and downloaded state.

## What Changes

- Add "Download chapter" and "Download unread / range" actions to series detail
  and reader screens.
- Implement a `DownloadEnqueuer` use case that inserts chapters into the queue
  and kicks off `ChapterDownloadWorker` jobs one at a time in a sequential
  chain.
- Extend `ChapterRepositoryImpl.getContent` to check `DownloadStore` before
  calling the source, falling back to network only when no local copy exists.
- Add `cancelBatch(seriesKey)` to `DownloadRepository` that removes all queued
  items for a series and cancels the active worker if it belongs to that batch.
- Guard `refreshChapters` against state loss: preserve `read`, `progress`, and
  `downloaded` columns when upserting refreshed chapter rows.
- Add "Downloads" entry point (FAB or menu item) on the series detail screen
  with unread-only vs range picker.

## Capabilities

### New Capabilities

- `download-enqueue`: Single-chapter and batch/series download enqueueing,
  sequential worker chaining, and batch cancellation.
- `download-offline-reader`: Readers prefer locally cached content from
  `DownloadStore` before falling back to the network.
- `chapter-refresh-state-preservation`: `refreshChapters` preserves existing
  read, progress, and downloaded state across refreshes.

### Modified Capabilities

<!-- No existing specs to modify. -->

## Impact

- **Domain layer**: new `DownloadEnqueuer` use-case interface; extended
  `DownloadRepository` with `cancelBatch`.
- **Data layer**: `DownloadRepositoryImpl` gains `cancelBatch`; `ChapterRepositoryImpl`
  gains offline-first `getContent`; `ChapterDao.upsertAll` logic changes to
  preserve state.
- **UI layer**: series detail screen and both reader screens gain download
  actions; Downloads screen unchanged.
- **Infrastructure**: `ChapterDownloadWorker` unchanged; WorkManager tag scheme
  extended for batch grouping.
- **Persistence**: no schema migration needed; `download_queue` table already
  supports per-chapter state.

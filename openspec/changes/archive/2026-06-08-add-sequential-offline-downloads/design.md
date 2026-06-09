## Context

ReaderParser already has the lower layers of a download pipeline:

- **Persistence**: `download_queue` table (`DownloadQueueEntity` +
  `DownloadQueueDao`) tracks per-chapter queue state (QUEUED → RUNNING →
  COMPLETED/FAILED). `DownloadStore` / `DownloadStoreImpl` persists chapter
  payloads to `filesDir/downloads/` with a `{sourceId}/{seriesHash}/{chapterHash}`
  layout.
- **Worker**: `ChapterDownloadWorker` downloads a single chapter, writes it via
  `DownloadStore`, and marks it downloaded in the chapter table. It retries up
  to 3 times with exponential backoff.
- **UI**: `DownloadsScreen` / `DownloadsViewModel` observe the queue and expose
  cancel/retry per item.

What is missing is the **end-to-end flow**: no UI action enqueues downloads,
readers always hit the network, there is no batch orchestration, and
`refreshChapters` can overwrite read/progress/downloaded state.

The readers (`NovelReaderViewModel`, `MangaReaderViewModel`) both call
`chapterRepository.getContent(chapter)` which goes straight to
`sourceRegistry[sourceId].getChapterContent(chapter)`. They never consult
`DownloadStore`.

`ChapterRepositoryImpl.refreshChapters` calls
`chapterDao.upsertAll(remote.map { it.toEntity() })` which uses
`OnConflictStrategy.REPLACE`. This replaces the full row, so existing `read`,
`progress`, and `downloaded` booleans/floats are lost when the source returns
stale or default values.

## Goals / Non-Goals

**Goals:**

- Users can enqueue a single chapter download from any reader or series detail
  screen.
- Users can enqueue a batch download for all unread chapters or a user-specified
  range on the series detail screen.
- Downloads execute sequentially: one `ChapterDownloadWorker` at a time, chained
  so the next starts after the previous completes.
- Canceling a batch removes all queued items for that batch and cancels the
  active worker if it belongs to that batch.
- Readers prefer locally cached content before falling back to the network.
- `refreshChapters` preserves existing chapter state (read, progress, downloaded).

**Non-Goals:**

- Downloading entire series in parallel (bandwidth/storage pressure).
- Pause/resume per-item (the existing retry mechanism covers failures).
- Wi-Fi-only or battery-saver download constraints beyond the existing
  `NetworkType.CONNECTED` constraint.
- Cloud sync or cross-device download state.
- A dedicated "Downloads" settings screen (queue screen already exists).

## Decisions

### D1: Sequential chaining via WorkManager tags

**Decision**: Enqueue batch downloads as individual `OneTimeWorkRequest`s, each
tagged with `download-{sourceId}-{batchId}`. The enqueueing code enqueues them
all at once; WorkManager's default `ExistingWorkPolicy.KEEP` ensures only one
worker runs at a time per unique work name.

**Rationale**: WorkManager already serializes `OneTimeWorkRequest`s by default.
Chaining via `then()` was considered but creates complexity around cancellation
and progress tracking. Using per-item requests with a shared tag lets
`cancelBatch` find and cancel remaining items cleanly.

**Alternative considered**: A single long-running worker that loops through
chapters. Rejected because it would not survive process death gracefully and
would need custom retry logic that WorkManager already provides per-request.

### D2: DownloadEnqueuer as a domain use case

**Decision**: Introduce `DownloadEnqueuer` interface in `domain/` with methods
`enqueueChapter(sourceId, chapterUrl)` and
`enqueueBatch(sourceId, seriesUrl, chapterUrls)`. Implementation lives in
`data/repository/`.

**Rationale**: Keeps the enqueue logic testable and separate from ViewModel
concerns. ViewModels call the use case; the use case talks to
`DownloadRepository` and WorkManager.

### D3: Offline-first getContent

**Decision**: Modify `ChapterRepositoryImpl.getContent` to check
`DownloadStore.read(chapter)` first. If non-null, return the cached content
immediately. Only fall back to the source when no local copy exists.

**Rationale**: This is the minimal change that makes readers consume local
content. No UI changes needed in readers — they already call `getContent`.

**Alternative considered**: A separate `getOfflineContent` method on the
repository. Rejected because it would require every callsite to change and
would not automatically benefit existing reader flows.

### D4: State-preserving chapter refresh

**Decision**: Change `ChapterDao` to use an insert-or-update strategy that
preserves `read`, `progress`, and `downloaded` columns. Two options:

1. Use `@Insert(onConflict = REPLACE)` but add a separate query that merges
   state before replace.
2. Use `@Upsert` (Room 2.5+) with a custom mapper that copies state from the
   existing row.

Chosen approach: **option 2** — use `@Upsert` (or a transaction that reads
existing state first, then inserts with merged values). The existing entity
already has `read`, `progress`, `downloaded` columns; the refresh query just
needs to carry them forward.

**Rationale**: `REPLACE` deletes + inserts, which triggers foreign key cascades
and loses state. A proper upsert that preserves state columns is the safest
approach. Room's `@Upsert` maps to `INSERT ... ON CONFLICT DO UPDATE` which
avoids the delete-then-insert cycle.

### D5: Batch identity

**Decision**: A batch is identified by `(sourceId, seriesUrl)` pair. All
queued chapters for a series share this identity. `cancelBatch` deletes all
`download_queue` rows matching the sourceId where the chapter URLs are in the
batch set.

**Rationale**: Series-level identity is already `(sourceId, url)` throughout the
app. No need to invent a separate batch ID.

## Risks / Trade-offs

- **[Risk] WorkManager process death during batch** → Mitigated by
  `ChapterDownloadWorker`'s retry logic and the fact that QUEUED items persist
  in Room. On restart, the user can manually retry failed items.

- **[Risk] Large series batch (100+ chapters)** → Mitigated by sequential
  execution. Storage pressure is the user's responsibility; the enqueue screen
  should show chapter count before confirming.

- **[Risk] Offline-first getContent may serve stale content** → This is by
  design for offline reading. The user can manually delete downloads or refresh
  from the series detail screen to force a re-fetch.

- **[Trade-off] No pause/resume** → Acceptable for v1. The cancel + retry
  mechanism covers most failure scenarios without the complexity of pause state
  management.

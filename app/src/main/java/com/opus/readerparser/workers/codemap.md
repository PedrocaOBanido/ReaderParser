# app/src/main/java/com/opus/readerparser/workers/

## Responsibility

Background task orchestration via Android `WorkManager`. Houses three `CoroutineWorker`
implementations and one `BroadcastReceiver` that survive process death and run on a
scheduled or manual basis independently of any UI screen:

- **`ChapterDownloadWorker`** — downloads a single chapter's content to app-private
  storage when the user enqueues it from the reader or downloads screen.
- **`LibraryUpdateWorker`** — periodically (every 6 hours) refreshes the chapter
  list for every series in the user's library, so new chapters are discovered
  without manual pull-to-refresh.
- **`SamsungSearchRebuildWorker`** — one-time worker that performs a full
  Samsung Search index rebuild, triggered by `SamsungSearchUpdateReceiver`.

- **`SamsungSearchUpdateReceiver`** — receives `ACTION_UPDATE_INDEX` broadcasts
  from Samsung Search and enqueues `SamsungSearchRebuildWorker` via WorkManager.

## Design

### Worker contract

Both workers extend `@HiltWorker CoroutineWorker` and are instantiated by
WorkManager's `HiltWorkerFactory`. They are injected with domain repository
interfaces — never with `SourceRegistry` or concrete `Source` implementations.
This preserves the layering rule that background work goes through the same
repository boundary as UI-initiated operations.

### `ChapterDownloadWorker`

- **Input**: `KEY_SOURCE_ID` (Long) + `KEY_CHAPTER_URL` (String), packed into
  `Data` by the static `buildRequest()` factory.
- **Flow**: lookup chapter in local DB → fetch content via `ChapterRepository.getContent()` →
  write to app-private storage via `DownloadStore` → mark downloaded in DB.
  HTTP image bytes for manhwa pages are fetched via an injected `HttpClient` in the
  `writeManhwa` lambda; the worker never calls `Source` methods directly.
- **Backoff**: exponential, 30s base, up to 3 total attempts (`runAttemptCount < 2` for retry).
- **Tagging**: deterministic tag `"download-$sourceId-${hashUrl(chapterUrl)}"` enables
  deduplication and cancellation via `WorkManager.cancelAllWorkByTag()`.
- **Constraints**: requires `NetworkType.CONNECTED`.

### `LibraryUpdateWorker`

- **Input**: none (no input data required — operates on the full library).
- **Flow**: read library series from `SeriesRepository.observeLibrary()` → call
  `ChapterRepository.refreshChapters()` for each.
- **Periodicity**: `PeriodicWorkRequest` every 6 hours (minimum interval allowed
  by WorkManager for periodic work). The `buildPeriodicRequest()` companion factory
  creates the request; actual scheduling is done at app startup via the DI layer.
- **Fault tolerance**: a single failed series does not block subsequent ones
  (each `refreshChapters` call is independent). Exceptions propagate per-series
  and failures trigger retry via WorkManager's built-in backoff.
- **Constraints**: requires `NetworkType.CONNECTED`.

### `SamsungSearchRebuildWorker`

- **Input**: none.
- **Flow**: calls `SearchIndexSyncer.rebuildIndex()` which queries the DAO,
  clears the Samsung Search index, and bulk-inserts current indexable series.
- **Backoff**: default WorkManager backoff. Retries up to 2 times
  (`runAttemptCount < 2`) before returning `Result.failure()`.
- **Trigger**: enqueued by `SamsungSearchUpdateReceiver` on
  `ACTION_UPDATE_INDEX` broadcast.

### `SamsungSearchUpdateReceiver`

- **Action**: `com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX`
- **Flow**: validates the action string, then enqueues a
  `OneTimeWorkRequest` for `SamsungSearchRebuildWorker` via WorkManager.
- **Permission**: requires `SEND_ACTION_UPDATE_INDEX` (declared in manifest).

### Shared patterns

| Aspect | Approach |
|---|---|
| DI | `@HiltWorker` + `@AssistedInject` — no manual `WorkerFactory` needed |
| Error handling | Catch at the top of `doWork()`, map to `Result.retry()` or `Result.failure()` |
| Logging | `Log.e` on failure (Android's logcat), with sourceId and chapterUrl for traceability |
| State reporting | Write download queue state (`DownloadState.RUNNING`/`COMPLETED`/`FAILED`) through `DownloadRepository.updateQueueState()` so the Downloads screen can observe progress reactively |
| DB mutations | Workers call repository methods, not DAOs directly; this keeps entity mapping in one place |

## Flow

### Chapter download

```
User tap "download" → ViewModel
  → DownloadRepository.retry() / DownloadRepository.cancel()
    → WorkManager.enqueue(ChapterDownloadWorker.buildRequest(sourceId, chapterUrl))

Worker execution (background thread, WorkManager-managed):
  inputData(sourceId, chapterUrl)
    → ChapterRepository.findByUrl(sourceId, chapterUrl)   // lookup local cache
    → DownloadRepository.updateQueueState(RUNNING)
    → ChapterRepository.getContent(chapter)                // fetches from source or local
    → DownloadStore.writeNovel() / writeManhwa()           // persist to filesDir
    → ChapterRepository.markDownloaded(chapter, true)      // flip DB flag
    → DownloadRepository.updateQueueState(COMPLETED)
    → Result.success()
```

### Library update

```
WorkManager trigger (every 6h, or on-demand from app)
  → LibraryUpdateWorker.doWork()
    → SeriesRepository.observeLibrary().first()          // get saved series list
    → forEach series: ChapterRepository.refreshChapters() // fetch fresh chapter list from source
    → Result.success()
```

## Integration

### Dependencies consumed

| Worker | Repository | Storage | Network |
|---|---|---|---|
| `ChapterDownloadWorker` | `ChapterRepository`, `DownloadRepository` | `DownloadStore` | `HttpClient` (image bytes only) |
| `LibraryUpdateWorker` | `SeriesRepository`, `ChapterRepository` | — | — |

Both repositories are domain-layer interfaces; their implementations live in
`data/repository/` and internally delegate to `SourceRegistry` + DAOs.
No worker imports an Androidx lifecycle, Compose, or UI type.

### Scheduling

Work is enqueued from the application layer at startup and from ViewModels on
user action:

- **`ChapterDownloadWorker`** — enqueued by `DownloadsViewModel` (via
  `DownloadRepository.retry()`) and potentially by the Series/Reader screen
  when the user initiates a download. The `buildRequest()` companion factory
  is the single point of `OneTimeWorkRequest` creation.
- **`LibraryUpdateWorker`** — enqueued once at app startup in `App.kt` or the
  `Application.onCreate()` hook via `WorkManager.enqueueUniquePeriodicWork()`.
  The `buildPeriodicRequest()` companion is the single factory.

### Testing

Test source sets mirror the worker directory:

- **`src/test/kotlin/.../workers/ChapterDownloadWorkerTest.kt`** — JVM unit test
  using `TestListenableWorkerBuilder` + fake repositories + fake `DownloadStore`.
  Validates success, source-not-found, retry-on-network-error, and final-failure
  scenarios.
- **`src/androidTest/kotlin/.../workers/ChapterDownloadWorkerTest.kt`** —
  instrumented test using `WorkManagerTestInitHelper` for full WorkManager
  lifecycle verification on-device.

There is no dedicated `LibraryUpdateWorkerTest` yet; testing relies on the
integration test coverage of `SeriesRepository` and `ChapterRepository`.

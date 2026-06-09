## 1. Domain layer — DownloadEnqueuer contract

- [x] 1.1 Create `DownloadEnqueuer` interface in `domain/` with `enqueueChapter(sourceId, chapterUrl)` and `enqueueBatch(sourceId, seriesUrl, chapterUrls: List<String>)` suspend functions
- [x] 1.2 Add `cancelBatch(sourceId: Long, chapterUrls: Set<String>)` to `DownloadRepository` interface

## 2. Data layer — DownloadEnqueuer implementation

- [x] 2.1 Implement `DownloadEnqueuerImpl` in `data/repository/`: insert chapters into `download_queue` via DAO and enqueue `ChapterDownloadWorker` per chapter
- [x] 2.2 Implement `cancelBatch` in `DownloadRepositoryImpl`: delete all matching queue rows and cancel WorkManager work by tag
- [x] 2.3 Register `DownloadEnqueuerImpl` in Hilt DI module

## 3. Offline-first content loading

- [x] 3.1 Inject `DownloadStore` into `ChapterRepositoryImpl`
- [x] 3.2 Modify `ChapterRepositoryImpl.getContent` to call `DownloadStore.read(chapter)` first; return cached content if non-null, otherwise fall back to source
- [x] 3.3 Add unit test for `ChapterRepositoryImpl` offline-first path (mock `DownloadStore` returns content → no source call)

## 4. State-preserving chapter refresh

- [x] 4.1 Modify `ChapterDao.upsertAll` to preserve `read`, `progress`, `downloaded` columns: use a transaction that reads existing state before inserting, or switch to `@Upsert` with a state-preserving mapper
- [x] 4.2 Add unit test for `ChapterDao` refresh preserves existing state
- [x] 4.3 Add unit test for `ChapterDao` refresh inserts new chapters with default state
- [x] 4.4 Add unit test for `ChapterDao` refresh removes stale chapters no longer in remote list

## 5. UI — Enqueue actions on series detail screen

- [x] 5.1 Add "Download" FAB or menu action to series detail screen that calls `DownloadEnqueuer.enqueueBatch` with unread chapters
- [x] 5.2 Add range picker dialog (start chapter, end chapter) to series detail screen for range-based batch download
- [x] 5.3 Show snackbar confirmation after enqueue (e.g., "Queued 12 chapters for download")

## 6. UI — Enqueue action on reader screens

- [x] 6.1 Add "Download" action to `NovelReaderAction` and `NovelReaderUiState`; wire to `DownloadEnqueuer.enqueueChapter`
- [x] 6.2 Add "Download" action to `MangaReaderAction` and `MangaReaderUiState`; wire to `DownloadEnqueuer.enqueueChapter`
- [x] 6.3 Show visual indicator on reader chapter list for already-downloaded chapters

## 7. UI — Downloads screen enhancements

- [x] 7.1 Add "Delete" action to `DownloadsAction` that calls `DownloadStore.delete` and removes the queue entry
- [x] 7.2 Wire delete action in `DownloadsViewModel`

## 8. Testing and verification

- [x] 8.1 Write unit test for `DownloadEnqueuerImpl` (enqueues correctly, deduplicates, batches in order)
- [x] 8.2 Write unit test for `DownloadRepositoryImpl.cancelBatch`
- [x] 8.3 Write unit test for offline-first `ChapterRepositoryImpl.getContent`
- [x] 8.4 Write unit test for state-preserving `ChapterDao` refresh
- [x] 8.5 Run `./gradlew :app:assembleDebug` — builds cleanly
- [x] 8.6 Run `./gradlew :app:testDebugUnitTest` — all tests pass

# app/src/main/java/com/opus/readerparser/data/local/database/dao/

## Responsibility

Room DAO interfaces — the query surface for every database table. Each DAO
defines reactive `Flow`-returning queries for observable state and `suspend`
functions for one-shot reads, writes, and deletes.

## Files

| File | Table | Role |
|---|---|---|
| `SeriesDao.kt` | `series` | Library CRUD, detail updates, `observeLibrary()` flow |
| `ChapterDao.kt` | `chapters` | Chapter listing, read/progress/downloaded state mutations |
| `DownloadQueueDao.kt` | `download_queue` | Queue observation, state transitions, join query `observeAllWithDetails()` |

## Design

**Flow-based observation.** Every list-returning `observe*()` method returns
`Flow<List<Entity>>`. Room invalidates the flow on any write to the table,
so repository flows automatically re-emit without manual refresh logic.

**Upsert with REPLACE.** Both `SeriesDao.upsertAll()` and
`ChapterDao.upsertAll()` use `OnConflictStrategy.REPLACE`. This handles
the case where a series or chapter row already exists (e.g., from a previous
browse listing) and needs to be overwritten with newer data.

**Selective updates over full replace.** `SeriesDao.updateDetails()` is a
targeted UPDATE that touches only content columns, leaving `inLibrary` and
`addedAt` untouched. This prevents library flags from being silently dropped
when browsing triggers a series persist. If no row exists (`updated == 0`),
the caller must `insert()` separately.

**`DownloadQueueWithDetails` — a join DTO.** `DownloadQueueDao` defines a
`data class` (not an entity) that joins `download_queue` → `chapters` →
`series` to enrich queue items with `chapterName` and `seriesTitle`. Room
maps the raw SQL columns to the data class constructor automatically.

## Key queries

| DAO | Query | Signature |
|---|---|---|
| SeriesDao | Observe library | `Flow<List<SeriesEntity>>` ordered by `addedAt DESC` |
| SeriesDao | Update details only | `suspend updateDetails(...): Int` — returns rows affected |
| SeriesDao | Get by sourceId | `suspend getBySourceId(sourceId: Long): List<SeriesEntity>` — fallback for remote search returning empty; caller applies `TitleMatcher.matches()` |
| ChapterDao | Observe chapters for a series | `Flow<List<ChapterEntity>>` ordered by `number ASC` |
| ChapterDao | Upsert from remote fetch | `suspend upsertAll(List<ChapterEntity>)` — REPLACE strategy |
| DownloadQueueDao | Observe queue with names | `Flow<List<DownloadQueueWithDetails>>` via LEFT JOIN |
| DownloadQueueDao | Update state + error | `suspend updateStateWithError(sourceId, chapterUrl, state, progress, errorMessage)` |

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `entities/` | Returns/accepts | Method parameters and return types are `Entity` types |
| `AppDatabase` | Provided by | `AppDatabase.seriesDao()`, `.chapterDao()`, `.downloadQueueDao()` |
| `data/repository/` | Called by | Repository impls inject DAOs via constructor |
| `core/di/DatabaseModule.kt` | Wired by | `@Provides fun provideSeriesDao(db): SeriesDao = db.seriesDao()` |

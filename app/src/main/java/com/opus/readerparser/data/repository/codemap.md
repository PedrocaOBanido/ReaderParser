# app/src/main/java/com/opus/readerparser/data/repository/

## Responsibility

Concrete implementations of the domain-defined repository interfaces. Every
`*Impl` class here bridges `SourceRegistry` (network/parse) with local
persistence (Room DAOs, DataStore, filesystem). ViewModels depend only on the
domain interfaces; Hilt wires the impls.

## Files

| File | Implements | Role |
|---|---|---|
| `SeriesRepositoryImpl.kt` | `SeriesRepository` | Browsing (popular/latest/search), library CRUD, detail refresh, library-search invalidation |
| `ChapterRepositoryImpl.kt` | `ChapterRepository` | Chapter list sync, content retrieval, read/progress state |
| `DownloadRepositoryImpl.kt` | `DownloadRepository` | Download queue observation, cancel/retry |
| `SourceRepositoryImpl.kt` | `SourceRepository` | Returns metadata for all registered sources |

## Design

**Repository per aggregate**. Each domain interface gets exactly one
implementation. Dependencies are injected: `SourceRegistry` for network,
Room DAO or DataStore for persistence. The impls are `@Singleton` — they live
for the lifetime of the app.

**Side-effect pattern for browse flows.** `SeriesRepositoryImpl.fetchPopular()`,
`fetchLatest()`, `search()`, and `refreshDetails()` all persist any returned
series to the database via `saveSeries()`. This means browsing implicitly
caches listing results, so library addition is never a no-op due to a missing
row.

**Fuzzy search fallback in `search()`.** When a remote search returns empty
results on page 1 with a non-blank query, `SeriesRepositoryImpl.search()`
falls back to matching against locally cached series. It fetches all cached
series for the source via `seriesDao.getBySourceId(sourceId)` and filters
them using `TitleMatcher.matches(query, title)` from `core/util`. Matched
results are sorted by title and returned with `hasNextPage = false`. This
ensures that series already in the database remain discoverable through
search even when the remote site does not return them (e.g., site search is
broken or the series was cached from a browse/popular call under a different
canonical title).

**`saveSeries()` uses upsert-via-update-then-insert.** It first calls
`seriesDao.updateDetails()` (a targeted UPDATE that preserves `inLibrary` and
`addedAt`). If the row did not exist (`updated == 0`), it calls
`seriesDao.insert()`. This prevents library flags from being silently dropped
when a browse result overwrites a series that is already in the user's library.

**Chapter refresh is full-replace.** `ChapterRepositoryImpl.refreshChapters()`
fetches the remote chapter list, maps each to `ChapterEntity` (with default
`read=false`, `progress=0f`, `downloaded=false`), and calls
`chapterDao.upsertAll()`. Old chapters remain (with their progress) because
Room REPLACE only overwrites rows with the same PK — new chapters are inserted,
unchanged ones are replaced.

**Download queue operations are thin.** `DownloadRepositoryImpl` delegates
to `DownloadQueueDao` with minimal transformation. The `observeQueue()` method
uses the join query `observeAllWithDetails()` to enrich entities with chapter
and series display names for the UI.

**`SourceRepositoryImpl` is a pure mapper.** It does not talk to the database
at all. It wraps `SourceRegistry.all()` by projecting each `Source` into a
`SourceInfo` domain object. No caching — sources are static at build time.

## Flow

```
ViewModel (via domain interface)
  │
  ▼
RepositoryImpl
  ├── SourceRegistry[id].get*()  ← network fetch through Source plugin
  ├── DAO.observe*()             ← reactive read from Room
  ├── DAO.suspend*()             ← one-shot write to Room
  └── SettingsStore              ← preferences read/write
```

### Example: browsing popular series
```
BrowseViewModel.fetchPopular(sourceId, page)
  → SeriesRepositoryImpl.fetchPopular(sourceId, page)
     → SourceRegistry[id].getPopular(page)    ← network
     → result.series.forEach { saveSeries(it) }  ← upsert to Room
     → return SeriesPage                       ← to ViewModel
```

### Example: observing library
```
LibraryViewModel
  → SeriesRepositoryImpl.observeLibrary()
      → seriesDao.observeLibrary()              ← Room Flow
      → .map { it.toDomain() }                  ← entity→domain
      → Flow<List<Series>>                      ← to ViewModel
```

### Example: library search invalidation
```
LibraryViewModel
  → SeriesRepositoryImpl.observeLibrarySearchInvalidations()
      → combine(seriesDao.observeLibrary(), seriesDao.observeIndexableSeries())
      → Flow<Unit>                               ← active search refresh trigger
```

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `domain/` interfaces | Implements | `SeriesRepository`, `ChapterRepository`, `DownloadRepository`, `SourceRepository` |
| `data/source/SourceRegistry.kt` | Calls into | `SourceRegistry[id]` for all network-backed operations |
| `data/local/database/dao/` | Calls into | `seriesDao`, `chapterDao`, `downloadQueueDao` |
| `data/local/database/mappers/` | Uses | `toDomain()`, `toEntity()`, `toChapterWithState()` |
| `data/local/prefs/SettingsStore.kt` | Calls into | `SettingsRepositoryImpl` uses `SettingsStore` |
| `core/di/RepositoryModule.kt` | Wired by | `@Binds *Impl → *Repository` |

# app/src/main/java/com/opus/readerparser/data/local/

## Responsibility

All local persistence on the Android device. Four sub-packages, each
backed by a different storage technology, each with an interface that is
fakeable in JVM unit tests:

| Sub-package | Technology | Purpose |
|---|---|---|
| `database/` | Room (SQLite) | Structured data: series metadata, chapters, download queue |
| `filesystem/` | Flat files | Blob cache: downloaded chapter HTML and page images |
| `prefs/` | Jetpack DataStore (Preferences) | App-wide user settings |
| `search/` | Samsung Search v2 ContentProvider | External search index for downloaded series |

## Design

**Interface + Impl pattern.** Every public type in this package has a
corresponding interface (in `domain/` or locally) and an implementation that
depends on Android infrastructure. Tests provide hand-rolled fakes or in-memory
Room databases.

**Identity = `(sourceId, url)`.** Every persisted entity uses the same composite
key defined in the domain layer. No auto-increment surrogate keys cross the
boundary.

**Thread safety.** Room DAOs are suspend functions (run on Room's internal
dispatcher). `DownloadStoreImpl` explicitly switches to `Dispatchers.IO` for
file I/O. DataStore operations are atomic via `edit {}`.

## Flow

```
data/repository/ → data/local/
                     ├── database/   (DAOs → entities → mappers → domain models)
                     ├── filesystem/ (DownloadStore → file read/write)
                     ├── prefs/      (SettingsStore → DataStore → AppSettings)
                     └── search/     (SearchIndexSyncer → SamsungSearchClient → Samsung Search provider)
```

- Room flows (`Flow<List<Entity>>`) are the reactive backbone — repository
  impls map them to domain types and expose them upstream.
- The filesystem layer is pull-based (one-shot `read()`/`write()` calls from
  `ChapterRepository` and download workers), not reactive.
- DataStore emits `Flow<Preferences>` which `SettingsRepositoryImpl` maps to
  an `AppSettings` domain model.

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `data/repository/` | Called by | Repository impls inject DAOs, `DownloadStore`, `SettingsStore` |
| `core/di/` | Wired by Hilt | `DatabaseModule` provides Room DB + DAOs; `FilesystemModule` provides `DownloadStore`; `PrefsModule` provides DataStore; `SearchModule` provides `SearchProviderDelegate` |
| `domain/` (models) | Maps to | `database/mappers/` convert entities to `Series`, `Chapter`, `ChapterWithState` |
| `workers/` | Driven by | `SamsungSearchRebuildWorker` calls `SearchIndexSyncer.rebuildIndex()`; `SamsungSearchUpdateReceiver` schedules the worker |

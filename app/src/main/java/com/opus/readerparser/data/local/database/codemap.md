# app/src/main/java/com/opus/readerparser/data/local/database/

## Responsibility

Room database layer — the single source of truth for structured, persisted app
state. Contains the `AppDatabase` class, entities (tables), DAOs (queries),
mappers (entity↔domain conversion), and migrations (schema versioning).

## Design

**One `AppDatabase` with three tables.** Version 1 (no migrations yet).
`exportSchema = true` — auto-generated JSON schemas are committed in
`app/schemas/`.

```
AppDatabase
├── seriesDao()          → SeriesDao
├── chapterDao()         → ChapterDao
└── downloadQueueDao()   → DownloadQueueDao
```

**Sub-package layout:**

| Package | Contents |
|---|---|
| `entities/` | `SeriesEntity`, `ChapterEntity`, `DownloadQueueEntity` — Room `@Entity` data classes with composite PKs |
| `dao/` | `SeriesDao`, `ChapterDao`, `DownloadQueueDao` — Room `@Dao` interfaces with suspend + Flow queries |
| `mappers/` | Extension functions: `SeriesEntity.toDomain()`, `Chapter.toEntity()`, `GenreJson` (JSON encode/decode for `List<String>`) |
| `migrations/` | `Migration` classes (currently empty — database at version 1) |

**Identity = `(sourceId, url)`.** All three entities use this composite primary
key. `ChapterEntity` has a `ForeignKey` to `SeriesEntity(sourceId, url)` with
`CASCADE` on delete. No auto-increment IDs.

**Reactive queries.** DAOs return `Flow<List<Entity>>` for observable state
(library, chapters sorted by number, download queue). One-shot operations
(single-row lookup, inserts, updates) are `suspend`.

## Flow

```
repository/Impl
  → DAO method
     ├── Flow query: Room invalidates on any write to the table → re-emits
     └── suspend query: direct read/write, no subscription

DAO result (Entity / List<Entity> / Flow<List<Entity>>)
  → repository maps to domain types via mappers/
     (SeriesEntity.toDomain(), ChapterEntity.toChapterWithState(), etc.)
  → returned to ViewModel as domain objects
```

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `dao/` | Provides | DAO interfaces exposed via `AppDatabase` abstract methods |
| `entities/` | Defines | `@Entity` classes referenced in `@Database(entities = [...])` and DAO method signatures |
| `mappers/` | Used by | Repository impls call `toDomain()` / `toEntity()` / `toChapterWithState()` |
| `core/di/DatabaseModule.kt` | Created by | Hilt `@Provides` builds the DB and exposes each DAO |
| `data/repository/` | Called by | Repository impls inject DAOs directly |

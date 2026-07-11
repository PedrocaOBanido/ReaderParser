# app/src/main/java/com/opus/readerparser/data/local/database/entities/

## Responsibility

Room `@Entity` data classes — the table definitions for the app's persistent
storage. These are the only types annotated with Room annotations; they are
converted to/from domain models by `mappers/`.

## Files

| Entity | Table | Composite PK | Foreign Keys |
|---|---|---|---|
| `SeriesEntity` | `series` | `(sourceId, url)` | — |
| `ChapterEntity` | `chapters` | `(sourceId, url)` | → `SeriesEntity(sourceId, seriesUrl)` with `CASCADE` delete |
| `DownloadQueueEntity` | `download_queue` | `(sourceId, chapterUrl)` | — (no FK; queue entries may reference chapters that are not yet cached) |

## Design

**Identity = `(sourceId, url)`.** Every entity uses this pair as its primary
key (or an equivalent variant: `(sourceId, chapterUrl)` for the download queue).
This matches the domain rule that series/chapter identity is stable across
sessions and never depends on auto-incrementing row IDs.

**`ChapterEntity` has a foreign key** to `SeriesEntity` on `(sourceId, seriesUrl)`.
When a series is removed from the database (via `DELETE FROM series WHERE ...`),
all its child chapters are removed automatically (`onDelete = ForeignKey.CASCADE`).
An index on `(sourceId, seriesUrl)` accelerates the chapter-per-series lookup.

**Nullable fields** mirror the domain model: `author`, `artist`, `description`,
`coverUrl` are `null` when the source does not provide them. `uploadDate` is
`null` when unknown. This aligns with the source contract's policy of using
nullable fields for genuinely optional data.

**Serialized genres.** `SeriesEntity.genresJson` stores `List<String>` as a
JSON string via `GenreJson`. The Kotlin `List<String>` type is not directly
mappable in Room, so the serialization is explicit in the entity layer rather
than relying on a Room `@TypeConverter`.

**Enum as String.** `status` and `type` are stored as their `.name` strings
(`"ONGOING"`, `"NOVEL"`, etc.) and deserialized back via `valueOf()` in the
mapper. This is human-readable in the database and avoids JPA-style ordinal
pitfalls.

**`inLibrary` guard.** `SeriesEntity.inLibrary` is a boolean flag (not a
separate join table). When `false`, the series row may still exist (e.g., from
a browse listing or detail fetch) but will not appear in `observeLibrary()`.
This avoids data loss when toggling library status.

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `AppDatabase` | Included in | `@Database(entities = [SeriesEntity::class, ChapterEntity::class, DownloadQueueEntity::class])` |
| `dao/` | Queried by | DAO methods return and accept these entity types |
| `mappers/` | Converted by | `SeriesEntity.toDomain()`, `Series.toEntity()`, `ChapterEntity.toDomain()`, `ChapterEntity.toChapterWithState()`, `Chapter.toEntity()` |

# app/src/main/java/com/opus/readerparser/core/util/

## Responsibility

**Pure‑JVM utility functions** that are referenced across multiple layers of the
app. These are stateless, side‑effect‑free operations with zero Android
dependencies — they compile against plain Kotlin/JVM.

## Design

Three standalone files, each exposing a single top‑level utility (one is an `object` with two public methods):

| File | Function / Object | Signature | Purpose |
|------|-------------------|-----------|---------|
| `ComputeSourceId.kt` | `computeSourceId` | `(name: String, lang: String, type: ContentType) -> Long` | Deterministic source identity for database foreign keys |
| `Hashing.kt` | `hashUrl` | `(url: String) -> String` | Stable, filesystem‑safe path component for downloads |
| `TitleMatcher.kt` | `TitleMatcher` (object) | `matches(query, title): Boolean` + `editDistance(a, b): Int` | Fuzzy series‑title search for in‑memory filtering |

### `computeSourceId`

```
"$name/$lang/${type.name}".hashCode().toLong() and 0xFFFFFFFFL
```

- Stable across reinstalls (same inputs → same output).
- Used as a foreign key in Room entities (`SeriesEntity.sourceId`,
  `ChapterEntity.sourceId`).
- Never hand‑picked; every `Source` calls this in its `id` val.

### `hashUrl`

- SHA‑1 of UTF‑8 bytes → lower‑case hex → truncated to 16 characters.
- Produces short, safe directory names under `filesDir/downloads/`.
- Truncation (64 bits) is sufficient to avoid accidental collisions within a
  single series/chapter namespace.
- Used exclusively by `DownloadStoreImpl` for path derivation.

### `TitleMatcher`

- **`matches(query, title)`**: Returns `true` when `query` matches `title` via
  exact substring match or fuzzy substring match (edit distance ≤ 1).
  - Case‑insensitive (`lowercase()`) with leading/trailing whitespace stripped
    and internal whitespace collapsed to single spaces.
  - Empty query matches everything.
  - Fuzzy pass uses a sliding‑window approach: for every window of length
    `qLen‑1`, `qLen`, or `qLen+1` in the title, checks `editDistance ≤ 1`.
- **`editDistance(a, b)`**: Levenshtein edit distance capped at 2, designed to
  short‑circuit as soon as distance ≥ 2 is detected. Optimised for the
  common case of strings that differ by at most one character.
- JVM‑testable without any Android dependencies.
- Used as a lightweight fallback when remote search returns empty results
  (`SeriesRepositoryImpl.search()`) and for client‑side filtering within the
  library (`LibraryViewModel.filterAndSort()`).

## Flow

```
Source constructor
  └─ calls computeSourceId(name, lang, type) → stored in Source.id
  └─ id propagates to Domain: Series.sourceId, Chapter.sourceId
  └─ id stored as DB foreign key in SeriesEntity, ChapterEntity

DownloadStore.writeManhwa / writeNovel
  └─ calls hashUrl(seriesUrl) → directory name for series
  └─ calls hashUrl(chapterUrl) → directory name for chapter
  └─ combined path: downloads/{sourceId}/{seriesHash}/{chapterHash}/

SeriesRepositoryImpl.search(query, sourceId)
  └─ delegates to Source.search(query) on the network
  └─ if results are empty, fallback to in‑memory:
       TitleMatcher.matches(query, each local series from DB)

LibraryViewModel.filterAndSort(query, sortOrder)
  └─ applies TitleMatcher.matches(query, series.title) for client‑side filter
  └─ matches is a pure function — runs on any dispatcher
```

## Integration

- **Consumed by**: Every `Source` implementation (calls `computeSourceId` in its
  `id` property); `DownloadStoreImpl` (calls `hashUrl`); `SeriesRepositoryImpl`
  (calls `TitleMatcher.matches` as search fallback); `LibraryViewModel` (calls
  `TitleMatcher.matches` for in‑library filtering); any test or fixture that
  needs to compute stable identifiers or fuzzy‑match titles.
- **Dependencies on**: `ContentType` enum from `domain/model/` (only
  `ComputeSourceId.kt`); standard library only.
- **Does not depend on**: Android SDK, Ktor, Room, or any other framework.
  All three files are fully JVM‑testable without Robolectric or Android test
  rules.

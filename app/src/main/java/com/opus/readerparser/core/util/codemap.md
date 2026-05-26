# app/src/main/java/com/opus/readerparser/core/util/

## Responsibility

**Pure‑JVM utility functions** that are referenced across multiple layers of the
app. These are stateless, side‑effect‑free operations with zero Android
dependencies — they compile against plain Kotlin/JVM.

## Design

Two standalone files, each containing a single top‑level function:

| File | Function | Signature | Purpose |
|------|----------|-----------|---------|
| `ComputeSourceId.kt` | `computeSourceId` | `(name: String, lang: String, type: ContentType) -> Long` | Deterministic source identity for database foreign keys |
| `Hashing.kt` | `hashUrl` | `(url: String) -> String` | Stable, filesystem‑safe path component for downloads |

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
```

## Integration

- **Consumed by**: Every `Source` implementation (calls `computeSourceId` in its
  `id` property); `DownloadStoreImpl` (calls `hashUrl`); any test or fixture
  that needs to compute stable identifiers.
- **Dependencies on**: `ContentType` enum from `domain/model/` (only
  `ComputeSourceId.kt`); standard library only.
- **Does not depend on**: Android SDK, Ktor, Room, or any other framework.
  Both files are fully JVM‑testable without Robolectric or Android test rules.

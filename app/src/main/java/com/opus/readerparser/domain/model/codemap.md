# app/src/main/java/com/opus/readerparser/domain/model/

## Responsibility

This is the **pure-Kotlin domain model** layer. It defines every shape of data
that crosses the boundary between layers — the lexicon of the entire app.

- Zero dependencies on Android, Ktor, Room, Compose, or any other framework.
- Every type here compiles against plain JVM (`kotlin-stdlib` only).
- No behaviour, no validation, no business logic — just data.
- These types flow through repository interfaces (in `domain/`) up to
  ViewModels and down to data-layer implementations.

## Design

### 1. Identity convention

Two types carry the identity pair `(sourceId: Long, url: String)`:

- **`Series`** — identity is `(sourceId, url)`. This is the primary key in
  both Room (as a composite PK) and in-memory maps.
- **`Chapter`** — identity is `(sourceId, url)`. `seriesUrl` is a foreign-key
  reference to `Series.url` (always within the same `sourceId`). The third
  component `seriesUrl` exists only for query convenience.

No auto-increment IDs anywhere.

### 2. Immutability

Every type is an immutable `data class` or a `sealed interface`. Update
through `.copy(...)` only. No `var` fields, no mutable collections
(`genres: List<String>` is `List`, not `MutableList`).

### 3. Sealed interfaces for closed-variant types

| Sealed type | Variants | Purpose |
|---|---|---|
| `ChapterContent` | `Text(html)` / `Pages(imageUrls)` | Reader screen branches on this exactly once; all other layers are content-type-agnostic. |
| `Filter` | `Text(key,value)` / `Select(key,value)` / `Toggle(key,boolean)` | Search/browse filters passed to sources. Three shapes cover the majority of web novel / manhwa site filter UIs. |

Adding a third variant to `ChapterContent` is an architectural decision that
requires explicit sign-off.

### 4. Enums for bounded domains

| Enum | Values | Used by |
|---|---|---|
| `ContentType` | `NOVEL`, `MANHWA` | `Series.type`, `SourceInfo.type`, reader routing |
| `SeriesStatus` | `UNKNOWN`, `ONGOING`, `COMPLETED`, `HIATUS`, `CANCELLED` | `Series.status`, mapped from site-specific strings |
| `DownloadState` | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` | `DownloadItem.state` |
| `AppTheme` | `SYSTEM`, `LIGHT`, `DARK` | `AppSettings.theme`, persisted to DataStore |
| `ManhwaLayout` | `PAGED_LTR`, `PAGED_RTL`, `WEBTOON` | Reader preference |
| `ManhwaZoom` | `FIT_WIDTH`, `FIT_HEIGHT`, `ORIGINAL` | Reader preference |

### 5. Nullable fields represent genuinely optional data

- `Series.author`, `Series.artist`, `Series.description`, `Series.coverUrl` —
  nullable because some site listings do not provide them. The
  `getSeriesDetails()` call may fill them in later.
- `Chapter.uploadDate` — nullable because not all sites expose dates.
- `DownloadItem.errorMessage` — null when the download has not failed.

Non-null defaults (`emptyList()`, `SeriesStatus.UNKNOWN`) are used when a
reasonable zero-value exists. **Never** use nullable booleans.

### 6. Wrapper / composite types

| Type | Contents | Purpose |
|---|---|---|
| `SeriesPage` | `series: List<Series>`, `hasNextPage: Boolean` | Paginated listing response from sources. Sources compute `hasNextPage` from DOM; callers drive page iteration. |
| `ChapterWithState` | `chapter: Chapter`, `read`, `downloaded`, `progress: Float` | Joins static chapter metadata with per-user mutable state. Returned by `ChapterRepository.observeChapters()` for the series detail screen. |
| `FilterList` | `filters: List<Filter>` | Passed to `Source.search()` and `SeriesRepository.search()`. Wrapped in a dedicated type so the signature is self-documenting. |
| `SourceInfo` | `id`, `name`, `lang`, `type` | Lightweight source descriptor used in UI pickers and browse screens. Discards `baseUrl` and full `getPopular`/`getLatest`/`search` capability — that stays on the `Source` interface. |
| `DownloadItem` | `sourceId`, `chapterUrl`, `seriesTitle`, `chapterName`, `state`, `progress`, `errorMessage` | Composite view of a download queue entry with user-presentable labels. |
| `AppSettings` | `theme`, `novelFontSize`, `novelFontFamily`, `manhwaLayout`, `manhwaZoom` | Aggregate of all user-configurable preferences. Read as a single `Flow` by SettingsViewModel. |

## Flow

```
Source plugins (data/source/)  ──→  Series, Chapter, ChapterContent
                                             │
                                             ▼
                    ┌────────────────────────────────────┐
                    │     Repository interfaces           │
                    │   (domain/ — contracts only)        │
                    └──────┬─────────────────────────────┘
                           │  return types / parameter types
                           ▼
              ┌─────────────────────────┐
              │     ViewModels           │
              │  (ui/*/ — presentation)  │
              │  import domain.model.*   │
              └─────────────────────────┘
                           │  map to UiState
                           ▼
              ┌─────────────────────────┐
              │     Composable screens   │
              │  read UiState fields     │
              └─────────────────────────┘
```

- **Sources** produce raw domain models (`Series`, `Chapter`, `ChapterContent`)
  from HTML/JSON parsing. These are not mapped to Room entities yet.
- **Repositories** (in `data/repository/`) receive domain models from sources,
  map them to Room entities for persistence, and return domain models back.
- **ViewModels** consume domain models from repository flows/suspends and
  map them to UI-specific `UiState` types.
- **Composables** never import domain models directly — they read `UiState`.
  The sole exception is `ChapterContent`, which the reader screen dispatches
  on (Text vs Pages) to render the correct reader layout.

## Integration

### Upper-layer dependency (presentation)

ViewModels in `ui/*/` import from `domain.model.*` for the return types of
repository method calls, but they **immediately map** these into screen-local
`UiState` data classes. Domain models never appear in composable signatures
except `ChapterContent`.

All domain model imports in ViewModels come from `com.opus.readerparser.domain.model`.

### Lower-layer boundary (data implementations)

Repository implementations in `data/repository/`:

- Accept domain models from sources or callers.
- Map to/from Room entities (`SeriesEntity`, `ChapterEntity`,
  `DownloadQueueEntity`) via mapper functions inside `data/local/database/`.
- Return domain models to callers.

Source plugins in `data/source/` and `sources/*/` produce raw domain models
directly from parsed HTML/JSON — no mapping layer between source output and
domain model.

### Hard boundary

```kotlin
// domain/model/ — compiles against plain JVM only
// No imports from:
//   android.*, androidx.*, io.ktor.*, org.jsoup.*, kotlinx.serialization.*
```

This is enforced by the build configuration. If a type in this package
imports anything outside `kotlin.*`, `kotlinx.coroutines.flow.Flow` (for
repository return types), it is a violation.

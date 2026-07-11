# app/src/main/java/com/opus/readerparser/data/local/database/mappers/

## Responsibility

Pure-Kotlin extension functions that convert between Room entities and domain
models. No Android dependencies, no dependency injection — just functions.

## Files

| File | Conversions |
|---|---|
| `SeriesMappers.kt` | `SeriesEntity.toDomain(): Series` · `Series.toEntity(): SeriesEntity` |
| `ChapterMappers.kt` | `ChapterEntity.toDomain(): Chapter` · `Chapter.toEntity(seriesUrl): ChapterEntity` · `ChapterEntity.toChapterWithState(): ChapterWithState` |
| `GenreJson.kt` | `genresToJson(List<String>): String` · `jsonToGenres(String): List<String>` |

## Design

**Top-level extension functions.** No class wrappers — callers import the
function directly: `import com.opus.readerparser.data.local.database.mappers.toDomain`.

**`toEntity()` creates a clean entity.** The `Chapter.toEntity()` function
always produces `read=false`, `progress=0f`, `downloaded=false`. The caller
(the repository) is responsible for reading existing state if it needs to
preserve user progress. This is intentional: when refreshing chapters from
the network, old progress should not be stomped — but the repository handles
that by mapping entities to domain models and back in the correct order.

**`toDomain()` is lossy for UI state.** `SeriesEntity.toDomain()` drops
`inLibrary` and `addedAt` (not part of the domain `Series` model).
`ChapterEntity.toDomain()` drops `read`, `downloaded`, `progress`. These are
recovered by `toChapterWithState()`, which bundles the domain `Chapter` with
the state fields into a `ChapterWithState` data class.

**Status/type parsing is lenient.** `SeriesEntity.toDomain()` uses
`runCatching { SeriesStatus.valueOf(status) }.getOrDefault(...)` so that
unknown enum values (e.g., from future versions or corrupt data) degrade to
`UNKNOWN` / `NOVEL` rather than crashing.

**Genre serialization.** `GenreJson` is a utility object wrapping
`kotlinx.serialization` to encode `List<String>` as a JSON array string
(`["Action","Fantasy"]`) and back. `jsonToGenres()` returns `emptyList()` for
blank input, preventing parse failures on null or empty database fields.

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `entities/` | Reads from | Takes entity instances as `this` receiver or parameter |
| `domain/model/` | Creates | Returns domain types (`Series`, `Chapter`, `ChapterWithState`) |
| `data/repository/` | Called by | Repository impls call mapper functions to convert between layers |

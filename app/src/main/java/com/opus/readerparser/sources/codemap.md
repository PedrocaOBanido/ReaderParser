# app/src/main/java/com/opus/readerparser/sources/

## Responsibility

This directory holds all **source plugin implementations** — one subdirectory per
supported site (e.g., `asurascans/`). Each plugin implements the `Source`
interface (typically via `HtmlSource`) and is the only layer that knows about
site-specific HTML structure, URL patterns, and parsing logic.

The rest of the app is site-agnostic, depending only on the `Source` contract
and the domain models (`Series`, `Chapter`, `ChapterContent`, etc.).

## Design

### Per-site isolation

Each site lives in its own directory named after the site in lowercase. The
directory contains exactly one Kotlin file (`<SiteName>.kt`) that extends
`HtmlSource`. This keeps site-specific:
- CSS selectors
- URL construction logic
- Date/string parsing helpers
- Custom HTTP headers (e.g., User-Agent for Cloudflare bypass)

completely isolated from one another and from the data layer.

### The `HtmlSource` template method pattern

Concrete sources override a set of abstract methods that `HtmlSource` calls
from its `Source` interface implementations:

```
HtmlSource.getPopular(page)         →  popularUrl(page) + popularSelector() + seriesFromElement()
                                     + popularNextPageSelector()
HtmlSource.getLatest(page)          →  (no default — must be overridden entirely)
HtmlSource.search(query, page, …)   →  searchUrl(...) + searchSelector() + searchSeriesFromElement()
                                     + searchNextPageSelector()
HtmlSource.getSeriesDetails(series) →  seriesDetailsParse(doc, series)
HtmlSource.getChapterList(series)   →  chapterListSelector() + chapterFromElement()
HtmlSource.getChapterContent(chapter) → chapterTextParse()  or  chapterPagesParse()
                                       (dispatched on ContentType)
```

This pattern ensures:
- Each concrete source only writes **extraction logic** (selectors + field mapping).
- HTTP fetching and Jsoup parsing are handled once in `HtmlSource`.
- `getChapterContent` dispatches automatically between novel text and manhwa
  pages based on the `type` property.

### Content-type contract

Every source declares `type: ContentType` (`NOVEL` or `MANHWA`). A source
overrides exactly one of `chapterTextParse` or `chapterPagesParse` and leaves
the other as `error("...")`. The reader screen branches on this once;
everything else stays agnostic.

### Error philosophy

Sources throw on error — they do not log, do not catch broadly, and do not
return null sentinels for missing data. Repositories and ViewModels handle
exception propagation. Missing optional data is expressed via nullable domain
model fields (e.g., `Series.author: String?`).

### Naming & identity

Source IDs are generated via `computeSourceId(name, lang, type)`:
```kotlin
fun computeSourceId(name: String, lang: String, type: ContentType): Long =
    "$name/$lang/${type.name}".hashCode().toLong() and 0xFFFFFFFFL
```
This is deterministic across reinstalls and serves as a foreign key in Room.

## Flow

```
ViewModel.request
    → Repository
        → SourceRegistry.get(sourceId)
            → Source.getPopular(page) / getSeriesDetails(series) / ...

HtmlSource template:
    1. Call fetchDoc(url) → Ktor GET → Jsoup.parse(response.bodyAsText())
    2. Apply site-specific CSS selectors to the Document
    3. Map Jsoup Elements to domain model instances (Series, Chapter, ChapterContent)

Errors propagate as exceptions back through Repository → ViewModel (caught,
mapped to UiState.error).
```

## Integration

### DI / SourceRegistry

All source instances are created at app start by `SourceModule` (in
`core/di/SourceModule.kt`). The module provides a `SourceRegistry` singleton:

```kotlin
@Provides @Singleton
fun registry(client: HttpClient): SourceRegistry = SourceRegistry(
    listOf(
        AsuraScans(client),
        // Add new sources here.
    ).associateBy { it.id },
)
```

Adding a new source requires exactly two steps:
1. Create `sources/<sitename>/<SiteName>.kt`.
2. Add `SiteName(client)` to the list in `SourceModule.kt`.

### Repository layer

Repositories (`SeriesRepositoryImpl`, `ChapterRepositoryImpl`) inject
`SourceRegistry` and call through to the appropriate source by `sourceId`.
They never reference concrete source classes — only the `Source` interface.

### Testing

Source tests live in `app/src/test/kotlin/.../sources/<sitename>/` using:
- Hand-saved HTML fixtures in `app/src/test/resources/fixtures/<sitename>/`
- Ktor `MockEngine` to serve the fixtures without network calls
- Verification of selectors, field extraction, and edge cases (nulls, missing elements, relative URLs)

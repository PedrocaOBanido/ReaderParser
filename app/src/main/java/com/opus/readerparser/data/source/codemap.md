# app/src/main/java/com/opus/readerparser/data/source/

## Responsibility

The **plugin system** for content sources. Defines the `Source` interface that
every site plugin implements, an `HtmlSource` base class for HTML-based sites
(which forms the majority), and a compile-time `SourceRegistry`.

This is the only package in the data layer that imports Ktor and Jsoup types.

## Files

| File | Kind | Role |
|---|---|---|
| `Source.kt` | Interface | Central contract for all site plugins. 7 `suspend` methods + metadata properties. |
| `HtmlSource.kt` | Abstract class | Base implementation for HTML sites. Wires Ktor HTTP → Jsoup parsing → domain types. Concrete sites override selector/parser methods. |
| `SourceRegistry.kt` | Class | `Map<Long, Source>` lookup. Populated once at app start by Hilt. |

## Design

**`Source` is the only contract.** Every site plugin implements `Source`.
The rest of the app (repositories, ViewModels) never references a concrete
source class — only `Source` through `SourceRegistry`.

**`HtmlSource` reduces boilerplate.** It provides:
- `fetchDoc(url)` — Ktor GET + Jsoup parse (with `fetchDocBody()` extension point
  for per-source HTTP customization).
- Default implementations of `getPopular()`, `search()`, `getSeriesDetails()`,
  `getChapterList()`, and `getChapterContent()` that call into abstract
  selector/parser methods.
- `getLatest()` is **not** implemented — there is no sensible default for
  latest-update pages, which differ per site.

**What concrete sources override:**

| Must override | For novels | For manhwa |
|---|---|---|
| `popularUrl()`, `popularSelector()`, `seriesFromElement()`, `popularNextPageSelector()` | ✓ | ✓ |
| `searchUrl()` | ✓ | ✓ |
| `seriesDetailsParse()` | ✓ | ✓ |
| `chapterListSelector()`, `chapterFromElement()` | ✓ | ✓ |
| `chapterTextParse()` | ✓ | — |
| `chapterPagesParse()` | — | ✓ |

**Separation by `ContentType`.** `getChapterContent()` switches on the source's
`type` field:
```kotlin
when (type) {
    ContentType.NOVEL  → ChapterContent.Text(chapterTextParse(doc))
    ContentType.MANHWA → ChapterContent.Pages(chapterPagesParse(doc))
}
```
Setting `type` incorrectly (e.g., `NOVEL` for a manhwa site) is a bug that
surfaces as a runtime `IllegalStateException` from the `error("Override for ...")`
default.

**`SourceRegistry` is a static map.** Keyed by `computeSourceId(name, lang, type)`
— a stable hash. No dynamic loading, no `PackageManager` scanning. Adding a
new source means: implement `Source`/`HtmlSource`, add it to the
`SourceRegistry` constructor call in `core/di/SourceModule.kt`.

**Errors are exceptions.** Sources never log, never catch broadly, never return
`null`. Missing optional elements use `selectFirst` + null-safety. Data that is
genuinely optional from the site is represented as nullable fields on domain
models.

## Flow

```
RepositoryImpl
  → SourceRegistry[id]
     → Source (concrete plugin, e.g., AsuraScans)
        → HtmlSource.getPopular(page)
           → fetchDoc(popularUrl(page))
              → Ktor GET → Jsoup.parse()
           → doc.select(popularSelector()).map(::seriesFromElement)
           → return SeriesPage(series, hasNext)
        
        → HtmlSource.getChapterContent(chapter)
           → fetchDoc(chapter.url)
           → when(type) {
                NOVEL  → Text(chapterTextParse(doc))
                MANHWA → Pages(chapterPagesParse(doc))
             }
```

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `sources/` (plugins) | Extended by | Each `<SiteName>.kt` extends `HtmlSource` (or implements `Source` directly) |
| `domain/model/` | Returns | `Series`, `SeriesPage`, `Chapter`, `ChapterContent`, `FilterList` |
| `data/repository/` | Called by | Repository impls obtain sources via `SourceRegistry[id]` |
| `core/di/SourceModule.kt` | Wired by | `@Provides fun registry(client): SourceRegistry` — lists all concrete sources |
| `data/network/` | Uses | Receives shared `HttpClient` from Hilt's `NetworkModule` |

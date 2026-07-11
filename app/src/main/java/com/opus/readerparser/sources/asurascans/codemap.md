# app/src/main/java/com/opus/readerparser/sources/asurascans/

## Responsibility

Provides the source plugin for **Asura Scans** — a manhwa scanlation aggregation
site (`https://asurascans.com/`). This plugin implements the full `Source`
interface to allow the app to browse, search, fetch series details, list
chapters, and retrieve chapter page images from the site.

Asura Scans is an **Astro-built site** that serves server-rendered HTML. The
plugin relies entirely on static HTML selectors — no JSON API, no SPA hydration.

## Design

### `AsuraScans` class

Extends `HtmlSource` and overrides every abstract method required for the
`HtmlSource` template-method pipeline.

#### Overridden parsing methods

| HtmlSource method                | AsuraScans implementation                                                |
|----------------------------------|--------------------------------------------------------------------------|
| `popularUrl(page)`               | `$baseUrl/browse?page=N` (page 1 omits `?page=`)                         |
| `popularSelector()`              | `#series-grid div.series-card`                                           |
| `seriesFromElement(el)`          | Extracts title (`h3`), URL (`a[href^="/comics/"]`), cover (`img`), status (`span.capitalize`) |
| `popularNextPageSelector()`      | `a[aria-label="Next page"]:not(.opacity-25)` (disabled link detection)   |
| `getLatest(page)`                | Homepage scrape: `div.grid.grid-cols-12.gap-2.py-4.px-2.border-b` rows  |
| `searchUrl(query, page, …)`      | `$baseUrl/browse?search={query}&page=N` (URL-encoded query)              |
| `seriesDetailsParse(doc, series)` | Detail page: author/artist links, meta description, genre links, cover, status |
| `chapterListSelector()`          | `a[data-astro-prefetch="hover"][href*="/chapter/"]`                      |
| `chapterFromElement(el, series)` | Extracts URL, name, number via regex, relative date via `parseRelativeDate` |
| `chapterPagesParse(doc)`         | `div[data-page] img` → list of image URLs in reading order               |

#### Cloudflare bypass

Overrides `fetchDocBody(url)` to send a realistic browser User-Agent and
Accept headers on every request:

```kotlin
override suspend fun fetchDocBody(url: String): String =
    client.get(url) {
        header("User-Agent", USER_AGENT)
        header("Accept", "text/html,…")
        header("Accept-Language", "en-US,en;q=0.9")
    }.bodyAsText()
```

The `USER_AGENT` constant mimics Chrome 125 on Windows. This is the only
source-level customization of the shared Ktor `HttpClient` — it is done by
overriding a single method rather than creating a separate client.

### Key parsing details

#### Browse page cards

Series cards in `#series-grid` contain a cover link, title, chapter count, and
status badge. The `seriesFromElement` method uses `selectFirst` with null-safe
navigation (`?: error("…")`) to avoid `NoSuchElementException` from bare
`.first()` calls, per project convention.

#### Series detail page enrichment

The `seriesDetailsParse` method enriches a `Series` with:
- **Author** — `a[href^="/browse?author="]`
- **Artist** — `a[href^="/browse?artist="]`
- **Description** — `<meta name="description">` content (HTML-entity decoded)
- **Genres** — all `a[href^="/browse?genres="]` links
- **Status** — first `span.capitalize` matching known status values (ongoing, completed, hiatus, cancelled)
- **Cover** — `#desktop-cover-container img` (falls back to listing cover)

#### Chapter number extraction

Uses the regex `Chapter\s+(\d+(?:\.\d+)?)` — case-insensitive — to parse
chapter numbers from names like "Chapter 108" or "Chapter 12.5". Returns
`-1f` as the sentinel for unparseable names.

#### Relative date parsing

Chapter upload dates are given as relative strings (e.g., "22 hours ago",
"3 days ago", "last week") or absolute dates ("Apr 4, 2026"). The
`parseRelativeDate` helper handles:
- Absolute dates via `DateTimeFormatter.ofPattern("MMM d, yyyy")`
- "last week" → 7 days ago
- "X hours/days/weeks/months/years ago" → computed epoch millis

#### Homepage "Latest Updates" section

`getLatest` only supports page 1 and scrapes the homepage for series rows in
the "Latest Updates" grid. This is a distinct scraper (`latestSelector` +
`latestSeriesFromElement`) separate from the browse-page parser because the
HTML structure differs completely.

### Content-type constraint

`type = ContentType.MANHWA`, so only `chapterPagesParse` is wired.
`chapterTextParse` delegates to `error("…")` as required by the convention.

## Flow

```
Repository calls:
    getPopular(1)         →  fetchDoc("/browse") → select "#series-grid div.series-card"
                              → map seriesFromElement → SeriesPage

    getLatest(1)          →  fetchDoc("/") → select "div.grid.grid-cols-12..." rows
                              → map latestSeriesFromElement → SeriesPage

    search(query, page)   →  fetchDoc("/browse?search=...&page=N") → select same selector
                              → map seriesFromElement → SeriesPage

    getSeriesDetails(s)   →  fetchDoc(s.url) → extract author/artist/desc/genres/status/cover
                              → s.copy(...) with enriched fields

    getChapterList(s)     →  fetchDoc(s.url) → select chapter rows
                              → map chapterFromElement → List<Chapter>

    getChapterContent(c)  →  fetchDoc(c.url) → select div[data-page] img
                              → extract src → ChapterContent.Pages(imageUrls)
```

All paths go through `HtmlSource.fetchDoc(url)` which calls
`AsuraScans.fetchDocBody(url)` → Ktor GET with custom headers → `Jsoup.parse`.

## Integration

### Registration

Registered in `core/di/SourceModule.kt`:
```kotlin
SourceRegistry(listOf(AsuraScans(client)).associateBy { it.id })
```

The `SourceModule` provides the shared `HttpClient` via `NetworkModule`.
`AsuraScans` receives it as a constructor parameter and passes it to the
`HtmlSource` superclass.

### ID computation

```kotlin
val id: Long = computeSourceId("AsuraScans", "en", ContentType.MANHWA)
```

Stable across reinstalls. Used as a foreign key in Room `SeriesEntity` and
`ChapterEntity` tables.

### Repository access

Repositories access this source through `SourceRegistry[id]`, which returns
the `Source` interface. They never reference `AsuraScans` directly. This means
all callers are decoupled from the concrete site implementation.

### Testing

Tests live in `app/src/test/kotlin/.../sources/asurascans/` using:
- HTML fixtures in `app/src/test/resources/fixtures/asurascans/`
- Ktor `MockEngine` to intercept requests and return fixture content
- Test cases per endpoint: popular, latest, search, series details,
  chapter list, chapter content
- Edge cases: missing optional elements, disabled next-page link,
  unparseable chapter names, unknown status values, all relative date formats

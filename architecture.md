# ReaderParser Architecture

ReaderParser is a personal Android app for reading webnovel and manhwa content
through site-specific `Source` plugins behind a common contract.

This document is the **normative architecture guide**. It defines durable layer
rules, contracts, invariants, and architectural decisions. It does **not** own
file-by-file repository mapping; use `codemap.md` and folder `codemap.md`
documents for current implementation locations.

## 1. Purpose and scope

ReaderParser has three core architectural goals:

1. keep site-specific scraping isolated behind a stable plugin boundary
2. keep domain contracts Android-free and easy to test on the JVM
3. keep novel and manhwa reading experiences distinct where their content
   shapes genuinely differ, while sharing one Reader screen and control layout

The repository is currently a single Android module, but the architectural
boundaries below still apply even when code lives in one Gradle module.

Core stack, at a stable-summary level: Jetpack Compose for UI, Kotlin
coroutines and Flow for async/state, Ktor + Jsoup for remote content, Room for
structured persistence, DataStore for preferences, WorkManager for background
jobs, and Hilt for DI.

## 2. Architectural principles

- **Layered design:** presentation depends on domain contracts, not concrete
  storage or source implementations.
- **Stable plugin boundary:** supported sites integrate through the `Source`
  contract so site churn does not leak across the app.
- **Pure domain:** domain models and repository contracts stay free of Android,
  Room, Compose, and Ktor types.
- **Explicit ownership:** repositories coordinate remote and local data; source
  plugins fetch and parse remote content; UI renders state and forwards actions.
- **Unified reader with content-specific renderers:** one Reader screen
  renders both text and image content through dedicated renderers while
  sharing immersive controls.

## 3. Layer model and dependency rules

Calls go down the stack; data and state flow back up.

| Layer | Responsibility | Must not depend on |
| --- | --- | --- |
| UI | Compose screens, stateless content composables, navigation wiring | `SourceRegistry`, concrete sources, Room, Ktor |
| Presentation | ViewModels, `UiState`, user actions/effects | concrete sources, Room entities, raw HTTP |
| Domain | Immutable models, repository contracts, use cases | Android, Compose, Room, Ktor |
| Data | Repository implementations and orchestration | UI concerns |
| Source plugins | Remote fetch + parse behind `Source` | ViewModels, navigation, Room DAOs |
| Infrastructure | Room, DataStore, filesystem, Ktor, WorkManager, DI | higher-level feature policy |

Rules:

- Lower layers never import higher layers.
- ViewModels never reference `SourceRegistry` or concrete `Source`s.
- Ktor calls live only inside `Source` implementations or repositories.
- Repositories are the only layer allowed to coordinate both source plugins and
  local storage.
- `*Screen` wires the ViewModel; `*Content` is the stateless preview target.

## 4. Core contracts and invariants

The following rules are stable across refactors and file moves.

- Domain models are immutable `data class`es.
- `ChapterContent` remains a sealed interface with exactly two shapes:
  `Text(html)` and `Pages(imageUrls)`.
- Series and chapters are identified by `(sourceId, url)` across layers.
- `sourceId` must be stable and deterministic because it participates in
  persistence identity.
- One Reader screen with content-specific renderers for text and image pages.
- Downloads stay in app-private storage under `context.filesDir`.
- No `runBlocking` in production code.

Contract-level shapes:

```kotlin
sealed interface ChapterContent {
    data class Text(val html: String) : ChapterContent
    data class Pages(val imageUrls: List<String>) : ChapterContent
}
```

```kotlin
interface Source {
    val id: Long
    val name: String
    val lang: String
    val baseUrl: String
    val type: ContentType

    fun supports(filter: Filter): Boolean
    suspend fun getPopular(page: Int): SeriesPage
    suspend fun getLatest(page: Int): SeriesPage
    suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage
    suspend fun getSeriesDetails(series: Series): Series
    suspend fun getChapterList(series: Series): List<Chapter>
    suspend fun getChapterContent(chapter: Chapter): ChapterContent
}
```

The app depends on this interface, not on concrete site classes.

## 5. Source plugin model

Each supported site is implemented as a `Source` plugin.

- A plugin owns request construction, response parsing, and mapping into domain
  models.
- Plugins may extend shared helpers such as `HtmlSource`, but that base class is
  a convenience, not the architectural contract.
- Plugin implementations are selected through `SourceRegistry` by `sourceId`.
- The rest of the app remains site-agnostic by depending on `Source` and domain
  models only.

Architectural consequence: adding or changing a site should primarily affect the
plugin implementation, its registration, and its tests, not the UI contract.

## 6. Persistence and ownership boundaries

ReaderParser intentionally splits persistence by concern:

- **Room** stores structured metadata and state such as library membership,
  chapter progress, and download queue state.
- **Filesystem** stores downloaded chapter payloads.
- **DataStore** stores user preferences and reader settings.

Ownership rules:

- Repositories translate between domain models and persistence models.
- Source plugins do not write directly to Room, DataStore, or downloaded-file
  storage.
- ViewModels do not talk directly to DAOs, files, or network clients.
- Room schema changes require explicit migrations; release behavior must not
  rely on destructive migration.

## 7. High-level runtime and data flow

1. Android starts the app and DI builds the object graph.
2. A screen composable forwards user actions to its ViewModel.
3. The ViewModel calls domain repository interfaces.
4. Repository implementations decide whether to serve local state, refresh from
   a `Source`, or combine both.
5. When remote data is required, the repository resolves the site via
   `SourceRegistry[sourceId]` and calls the `Source` contract.
6. Source plugins fetch and parse remote content into domain models.
7. Repositories persist or merge results, then expose state back to presentation
   via flows or suspend results.
8. WorkManager workers reuse the same repository/source/storage graph outside
   the UI lifecycle.

Reader navigation opens one Reader destination with an explicit content type.
The screen branches once on `ChapterContent` to select the renderer:

- `ChapterContent.Text` uses the JavaScript-disabled WebView renderer.
- `ChapterContent.Pages` uses the vertical image-page renderer.

## 8. Key architectural decisions and trade-offs

### Unified reader with content-specific renderers

One Reader screen shares immersive controls, navigation, and state management
while keeping text and image rendering as private composables selected by
`ChapterContent`. This eliminates duplicated navigation, effects, and control
logic without introducing a polymorphic renderer abstraction.

### Repositories own orchestration

Repositories are the only layer allowed to see both plugin output and local
storage. This keeps ViewModels simple and keeps site/plugin code focused on
remote parsing instead of app state policy.

### Domain stays Android-free

Keeping domain contracts pure Kotlin preserves testability and prevents Android,
Room, or networking concerns from spreading upward.

### Single module, logical boundaries

The app is still a single Gradle module. That does not relax the dependency
rules above. If build or ownership pressure increases later, module splits may
follow the existing architectural boundaries rather than redefining them.

## 9. Relationship to `codemap.md`

Use the architecture and codemap documents by responsibility:

- **Update `architecture.md`** when layer rules, contracts, invariants, or
  architectural decisions change.
- **Update `codemap.md`** when structure, entry points, or implementation
  locations change.
- **Update both** when both the rules and the concrete structure changed, but do
  not duplicate the same detail in both places.

Start with `codemap.md` for codebase navigation. Read a folder's `codemap.md`
for local implementation maps.

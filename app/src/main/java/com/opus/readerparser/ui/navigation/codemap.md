# app/src/main/java/com/opus/readerparser/ui/navigation/

## Responsibility

Defines all app routes and the single compose-navigation graph. Every screen is
registered here; no screen composable calls `navController` directly — they
receive typed lambda callbacks that the NavGraph supplies.

## Design

### `Destinations.kt` — Route constants and builders

A single `object Destinations` contains:
- **Route constants** — string templates with `{param}` placeholders for
  `NavType` arguments. Six routes total:
  - `LIBRARY`, `BROWSE`, `DOWNLOADS`, `SETTINGS` — top-level, no arguments.
  - `SERIES` — requires `sourceId: Long` and `seriesUrl: String`.
  - `NOVEL_READER` / `MANGA_READER` — require `sourceId`, `seriesUrl`, `chapterUrl`.
- **Builder functions** — `series()`, `novelReader()`, `mangaReader()` that
  produce concrete route strings with `Uri.encode()` applied to URL parameters
  (URLs may contain slashes, query strings, and other characters unsafe in
  route templates).

Route templates vs builder output:
```
Destinations.SERIES                  → "series/{sourceId}/{seriesUrl}"
Destinations.series(1, "http://…")   → "series/1/http%3A%2F%2F…"
```

Identity is `(sourceId, url)` — all three URL parameters are encoded when
navigating and decoded automatically by `NavType.StringType` on arrival.

### `NavGraph.kt` — `AppNavGraph` composable

A single top-level composable called from the host activity (or top-level
wrapper). Contains:

1. **`rememberNavController()`** — single controller for the whole graph.
2. **`BottomNavItem`** — private data class for the three bottom tabs (Library,
   Browse, Downloads). Each has a route, label, and Material icon.
3. **`Scaffold` with conditional `NavigationBar`** — the bottom bar is rendered
   only when `currentRoute in bottomNavRoutes` (i.e., hidden on Series, reader,
   and Settings screens).
4. **`NavHost`** — start destination is `LIBRARY`. Seven `composable()`
   entries, one per route.

### Route dispatch for content type

The app has two reader routes (`NOVEL_READER` and `MANGA_READER`) rather than a
single parameterised route. This keeps route definitions simple and avoids
parsing `ContentType` inside NavGraph argument blocks. The decision is made at
navigation time inside `SeriesScreen`: when the effect fires, the
`LaunchedEffect` block checks `effect.type` and calls either
`onNavigateToNovelReader` or `onNavigateToMangaReader`. Navigation callbacks are
typed lambda parameters of the `SeriesScreen` composable.

### Screen callback contract

Every `composable` block passes typed lambdas to the screen composable. These
form the only bridge between the stateless screen and the navigation system:

```kotlin
// Top-level screens get simple callbacks
composable(Destinations.LIBRARY) {
    LibraryScreen(
        onNavigateToSeries = { series ->
            navController.navigate(Destinations.series(series.sourceId, series.url))
        },
        onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
    )
}

// Reader screens get chapter-to-chapter navigation with popUpTo
NovelReaderScreen(
    onNavigateToChapter = { chapter ->
        navController.navigate(Destinations.novelReader(...)) {
            popUpTo(Destinations.NOVEL_READER) { inclusive = true }
        }
    },
)
```

### Bottom navigation behaviour

Bottom tabs use `launchSingleTop = true` and `restoreState = true` with
`popUpTo(findStartDestination().id) { saveState = true }` — the standard
pattern for avoiding back-stack duplication while preserving per-tab state.

## Flow

```
Activity.onCreate
  → AppNavGraph()
    → Scaffold(bottomBar = NavigationBar if tab route)
      → NavHost(startDestination = LIBRARY)
        → LibraryScreen(onNavigateToSeries = { navController.navigate(...) })
          → User taps series → effect triggers callback → navController.navigate
            → SeriesScreen(onNavigateToNovelReader = ..., onNavigateToMangaReader = ...)
              → User taps chapter → effect branches on ContentType
                → NOVEL  → navController.navigate(NOVEL_READER)
                → MANHWA → navController.navigate(MANGA_READER)
```

Back navigation: reader and series screens accept `onBack: () -> Unit` which
calls `navController.popBackStack()`.

## Integration

- **Screens**: Each `composable()` calls the corresponding `*Screen` composable
  from `ui/<screen>/`. The screen receives its ViewModel via `hiltViewModel()`
  automatically from the composition local.
- **Theme**: `AppNavGraph()` is expected to be called inside `ReaderParserTheme`
  (which wraps the entire app in `MainActivity`). The theme file lives in
  `ui/theme/`.
- **Domain models**: `NavType.LongType` and `NavType.StringType` decode
  `sourceId`, `seriesUrl`, and `chapterUrl` from route parameters. The identity
  triplet `(sourceId, seriesUrl, chapterUrl)` matches the domain model identity
  convention.
- **Bottom nav state**: `currentBackStackEntryAsState()` reactively tracks the
  current route to show/hide the `NavigationBar`. The bottom nav only appears
  on the three tab routes; it disappears for detail and reader screens.

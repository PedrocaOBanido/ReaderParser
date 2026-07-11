# app/src/main/java/com/opus/readerparser/ui/

## Responsibility

Houses all Jetpack Compose UI code: seven screens, shared components, navigation
graph, and Material 3 theme. This is the top presentation layer — it consumes
`UiState` from ViewModels and renders composables; it never calls repositories,
sources, or storage directly.

## Design

### Screen structure — 4 files per screen

Each screen follows an identical layout enforced by `AGENTS.md`:

| File | Type | Role |
|---|---|---|
| `*Screen.kt` | `@Composable` | Wires the ViewModel (`hiltViewModel()`), collects `state` with `collectAsStateWithLifecycle()`, collects `effects` in a `LaunchedEffect(Unit)` block, then delegates to `*Content`. Never previewed. |
| `*Content.kt` | `@Composable` | Stateless function receiving `(state, onAction)`. Renders all UI. Always has at least one `@Preview`. |
| `*ViewModel.kt` | `@HiltViewModel` | Exposes `state: StateFlow<*>`, `effects: Flow<*>`, `onAction(XAction)`. |
| `*UiState.kt` | File | Contains the `data class UiState`, `sealed interface Action`, and `sealed interface Effect` for the screen. |

### State management contract

Every ViewModel follows:
- **`state`** — single `StateFlow<XUiState>` data class holding everything the
  screen renders (`isLoading`, `error`, plus domain data). No parallel flows.
- **`effects`** — `Channel<XEffect>(BUFFERED)` exposed as `receiveAsFlow()`
  for one-shot side effects (navigation, snackbars, toasts). Never put
  navigation in `UiState`.
- **`onAction`** — single entry point for all UI events. Every tap, swipe, or
  input maps to exactly one `Action` sealed interface variant.

### Composables

- All screens use **Material 3** exclusively. No Material 2 imports.
- Theme tokens come from `MaterialTheme.colorScheme` / `.typography` — no
  hardcoded colors or font sizes outside `ui/theme/`.
- Images use **Coil 3** `AsyncImage` with `placeholder` and `error` painters.
- Lists use `LazyColumn` / `LazyVerticalGrid` with stable `key` props.
- Shared composables (cover cards, chapter rows, status pills) live in
  `ui/components/` and are hoisted when used by 2+ screens.
- No business logic in composables — decisions about what to fetch or how to
  branch are made in ViewModels.

### Screens

| Screen | Path | Purpose |
|---|---|---|
| Library | `ui/library/` | Grid of saved series. Filter (unread), sort (default/title), settings gear. |
| Browse | `ui/browse/` | Source-picker → popular/latest/search within a source. |
| Series | `ui/series/` | Series detail + chapter list. Dispatches to novel or manga reader based on `ContentType`. |
| NovelReader | `ui/reader/novel/` | Vertical text reader for novels (ChapterContent.Text). |
| MangaReader | `ui/reader/manhwa/` | Pager/webtoon image reader for manhwa (ChapterContent.Pages). |
| Downloads | `ui/downloads/` | Download queue with progress and retry. |
| Settings | `ui/settings/` | App-wide preferences: theme, reader defaults. |

## Flow

```
User tap → Screen composable → onAction(viewModel::onAction)
  → ViewModel.onAction(XAction)
    → Repository / UseCase
      → MutableStateFlow<UiState>.update { ... }
  → Screen composable collects new state
  → *Content re-renders
```

One-shot side effects (navigation):
```
ViewModel.onAction → _effects.send(Effect.NavigateToReader(chapter))
  → LaunchedEffect(Unit) in *Screen.collects
    → lambda callback provided by NavGraph
      → navController.navigate(...)
```

The `SeriesScreen` is the dispatch point: when a chapter is opened, its
`SeriesEffect` carries both the `Chapter` and `ContentType`. The `LaunchedEffect`
block branches on `ContentType.NOVEL` vs `MANHWA` to call the correct
navigation callback.

## Integration

- **Upstream**: ViewModels depend on `domain/` interfaces (repositories) and
  `data/` implementations injected via Hilt. They never reference `SourceRegistry`
  or concrete `Source` types.
- **Downstream**: All Compose code is wrapped in `ReaderParserTheme` (from
  `ui/theme/`) at the top of `AppNavGraph`. The `AppTheme` enum from
  `domain/model/` drives light/dark/system switching.
- **Navigation**: `ui/navigation/NavGraph.kt` defines `AppNavGraph()` and the
  bottom nav scaffold. Screen composables receive lambda callbacks that the
  NavGraph wires to `navController.navigate(...)` or `popBackStack()`.
- **Navigation callbacks**: `*Screen` composables accept typed lambda parameters
  (e.g., `onNavigateToSeries: (Series) -> Unit`, `onBack: () -> Unit`). The
  `NavGraph` supplies implementations; the screen only calls them in response to
  effects.
- **Bottom navigation**: Three tabs (Library, Browse, Downloads) shown on a
  `NavigationBar` inside a `Scaffold`. Detail/reader screens hide the bottom bar.

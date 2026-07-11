# app/src/main/java/com/opus/readerparser/ui/browse/

## Responsibility

Provides a source-picker and browse/search interface for discovering series
across all registered sources. Supports three modes: Popular, Latest, Search.

## Design

### Files
| File | Kind | Responsibility |
|---|---|---|
| `BrowseScreen.kt` | Screen (orchestrator) | Wires `BrowseViewModel`, collects effects with `LaunchedEffect`, delegates to `BrowseContent`. |
| `BrowseContent.kt` | Content (stateless) | Renders source dropdown, mode tabs, search bar, and a paginated 2-column grid of series cards. Has `@Preview` variants. |
| `BrowseViewModel.kt` | ViewModel | Manages source selection, mode switching, paginated fetching, and search. Injects `SourceRepository` + `SeriesRepository`. |
| `BrowseUiState.kt` | Types | `BrowseUiState` data class, `BrowseAction` sealed interface, `BrowseEffect` sealed interface, `BrowseMode` enum. |

### State / Action / Effect
- **UiState** — `sources: List<SourceInfo>` (all registered sources), `selectedSourceId`, `mode: BrowseMode`, `series: List<Series>`, `isLoading`, `error`, `hasNextPage`, `currentPage`, `searchQuery`.
- **Action** — `SelectSource` / `SetMode` → reset fetch; `LoadMore` → append page; `SetSearchQuery` / `Search` → trigger search; `OpenSeries` → navigate.
- **Effect** — `NavigateToSeries(series)` → nav; `ShowError(message)` → snackbar (TODO).

### BrowseMode
- `POPULAR` — calls `seriesRepository.fetchPopular(sourceId, page)`.
- `LATEST` — calls `seriesRepository.fetchLatest(sourceId, page)`.
- `SEARCH` — calls `seriesRepository.search(sourceId, query, page, FilterList())`.

### Pagination
- `currentPage` tracks the last fetched page.
- `hasNextPage` comes from `SeriesPage.hasNextPage` in the repository response.
- `LoadMore` appends new results to the existing list (`reset = false`).
- Switching source or mode resets to page 1 (`reset = true`).

## Flow

```
User interaction
    │
    ▼
BrowseContent (stateless)
    │  onAction(BrowseAction)
    ▼
BrowseViewModel.onAction(action)
    │
    ├── SelectSource(id)          → _state.update + fetchPage(reset=true)
    ├── SetMode(mode)             → _state.update + fetchPage(reset=true)
    ├── LoadMore                  → fetchPage(page=currentPage+1, reset=false)
    ├── SetSearchQuery(q)         → _state.update { searchQuery = q }
    ├── Search                    → _state.update { mode=SEARCH } + fetchPage(reset=true)
    └── OpenSeries                → _effects.send(NavigateToSeries)
         │
         ▼
    fetchPage(sourceId, mode, page, reset):
        _state.update { isLoading=true, error=null }
        result = seriesRepository.fetchPopular|fetchLatest|search(...)
        _state.update { series = (if reset) result.series else old + result.series, ... }
        on failure: _state.update { error=..., isLoading=false } + _effects.send(ShowError)
```

Initial load (ViewModel `init`):
```
sourceRepository.getSources() → populate sources + select first
    └─ fetchPage(firstSource.id, POPULAR, 1, reset=true)
```

## Integration

- **Inputs**:
  - `SourceRepository.getSources(): List<SourceInfo>` — compile-time list of all registered sources.
  - `SeriesRepository.fetchPopular(sourceId, page): SeriesPage` — delegate to `Source.getPopular()`.
  - `SeriesRepository.fetchLatest(sourceId, page): SeriesPage` — delegate to `Source.getLatest()`.
  - `SeriesRepository.search(sourceId, query, page, filters): SeriesPage` — delegate to `Source.search()`.
- **Output effects**: `NavigateToSeries(series)` → nav graph to `SeriesScreen`; `ShowError` → snackbar (TODO).
- **No direct access** to `SourceRegistry` or `HttpClient`. All source calls go through `SeriesRepository`.

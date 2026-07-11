# app/src/main/java/com/opus/readerparser/ui/library/

## Responsibility

Displays the user's saved series library and provides sort/filter/search controls.
This is the app's home screen — the first thing the user sees.

## Design

### Files
| File | Kind | Responsibility |
|---|---|---|
| `LibraryScreen.kt` | Screen (orchestrator) | Wires `LibraryViewModel` via `hiltViewModel()`, collects effects with `LaunchedEffect`, delegates rendering to `LibraryContent`. Never previewed. |
| `LibraryContent.kt` | Content (stateless) | Renders the full library UI. Accepts `LibraryUiState` + `onAction` lambda + optional `onNavigateToSettings`. Has `@Preview` variants for loading / error / populated. |
| `LibraryViewModel.kt` | ViewModel | Single source of truth via `StateFlow<LibraryUiState>`. Injects `SeriesRepository`. Observes reactive library stream plus search invalidations on init. Routes non-blank queries through repository-backed Samsung Search and keeps blank queries local. Exposes `onAction(LibraryAction)` entry point. |
| `LibraryUiState.kt` | Types | `LibraryUiState` data class, `LibraryAction` sealed interface, `LibraryEffect` sealed interface, plus `LibrarySortBy` enum. |

### State / Action / Effect
- **UiState** — `series: List<Series>` (already sorted locally), `isLoading`, `error`, `sortBy`, `filterUnreadOnly`, `searchQuery: String = ""`.
- **Action** — `OpenSeries` → navigates away; `RemoveFromLibrary` → repo call; `SetSortBy` / `SetFilterUnreadOnly` → local state update; `SetSearchQuery` → blank queries refresh the local list, non-blank queries search Samsung Search through the repository.
- **Effect** — `NavigateToSeries(series)` → navigation callbacks fired in `LibraryScreen`; `ShowError(message)` → snackbar (TODO placeholder).

### State handling
- Loading: centered `CircularProgressIndicator`.
- Error: centered error text.
- Empty (no search): informative placeholder text ("Your library is empty…").
- Empty (with search): placeholder text changes to "No series match \"{query}\"." when searchQuery is non-empty.
- Populated: 2-column `LazyVerticalGrid` of `LibrarySeriesCard` composables.
- Cards show cover (Coil `AsyncImage`, 2:3 aspect ratio) + title. Long-press removes from library.
- Search bar (`OutlinedTextField` with leading search icon, trailing clear button) sits above the grid. Configures IME search action. Always visible, even when the library is empty.

### Sort / filter / search
- `LibrarySortBy.DEFAULT` (insertion order) or `TITLE` (alphabetical). Both operate on the in-memory list — no DB re-query.
- Unread filter chip toggles `filterUnreadOnly` in UI state (actual filtering is a TODO; `series` list is currently unfiltered).
- **Search**: blank queries render the observed library list locally. Non-blank queries call `SeriesRepository.searchLibrary()`; the repository queries Samsung Search, resolves hits back to local indexable rows, and the ViewModel renders provider-ordered results or an error state.

## Flow

```
User tap/input
     │
     ▼
LibraryContent (stateless composable)
     │  onAction(LibraryAction)
     ▼
LibraryViewModel.onAction(action)
     │
      ├── SetSearchQuery → blank query: local list / non-blank query: repository.searchLibrary() → _state.update
       ├── SetSortBy / SetFilterUnreadOnly → blank query: re-sort local list / non-blank query: keep search results
      ├── RemoveFromLibrary → seriesRepository.removeFromLibrary(...) → _effects.send(ShowError?) on failure
      ├── OpenSeries → _effects.send(NavigateToSeries)
      │
     ▼
StateFlow<LibraryUiState>  ──collectAsStateWithLifecycle──►  LibraryContent re-render
Channel<LibraryEffect>     ──LaunchedEffect collect──────►  LibraryScreen (nav / snackbar)
```

Background data flow (init):
```
seriesRepository.observeLibrary()  ──collect──►  allLibrarySeries = it; blank-query state refresh
seriesRepository.observeLibrarySearchInvalidations() ──collect──► active non-blank Samsung search refresh
```

## Integration

- **Input**: `SeriesRepository.observeLibrary(): Flow<List<Series>>` — reactive stream from Room.
- **Input**: `SeriesRepository.observeLibrarySearchInvalidations(): Flow<Unit>` — active-search refresh trigger for membership/download changes.
- **Output effects**: `NavigateToSeries(series)` → nav graph navigates to `SeriesScreen` with `sourceId` + `seriesUrl`; `ShowError` → snackbar (TODO).
- **Output callback**: `onNavigateToSettings` wired from `LibraryScreen` to `LibraryContent` for the settings gear icon.
- **No dependency on** `SourceRegistry`, `ChapterRepository`, or any concrete source. Follows AGENTS.md rule #3.

# app/src/main/java/com/opus/readerparser/ui/library/

## Responsibility

Displays the user's saved series library and provides local sort/filter controls.
This is the app's home screen — the first thing the user sees.

## Design

### Files
| File | Kind | Responsibility |
|---|---|---|
| `LibraryScreen.kt` | Screen (orchestrator) | Wires `LibraryViewModel` via `hiltViewModel()`, collects effects with `LaunchedEffect`, delegates rendering to `LibraryContent`. Never previewed. |
| `LibraryContent.kt` | Content (stateless) | Renders the full library UI. Accepts `LibraryUiState` + `onAction` lambda + optional `onNavigateToSettings`. Has `@Preview` variants for loading / error / populated. |
| `LibraryViewModel.kt` | ViewModel | Single source of truth via `StateFlow<LibraryUiState>`. Injects `SeriesRepository`. Observes reactive library stream on init. Exposes `onAction(LibraryAction)` entry point. |
| `LibraryUiState.kt` | Types | `LibraryUiState` data class, `LibraryAction` sealed interface, `LibraryEffect` sealed interface, plus `LibrarySortBy` enum. |

### State / Action / Effect
- **UiState** — `series: List<Series>` (already sorted locally), `isLoading`, `error`, `sortBy`, `filterUnreadOnly`.
- **Action** — `OpenSeries` → navigates away; `RemoveFromLibrary` → repo call; `SetSortBy` / `SetFilterUnreadOnly` → local state update.
- **Effect** — `NavigateToSeries(series)` → navigation callbacks fired in `LibraryScreen`; `ShowError(message)` → snackbar (TODO placeholder).

### State handling
- Loading: centered `CircularProgressIndicator`.
- Error: centered error text.
- Empty: informative placeholder text ("Your library is empty…").
- Populated: 2-column `LazyVerticalGrid` of `LibrarySeriesCard` composables.
- Cards show cover (Coil `AsyncImage`, 2:3 aspect ratio) + title. Long-press removes from library.

### Sort / filter
- `LibrarySortBy.DEFAULT` (insertion order) or `TITLE` (alphabetical). Both operate on the in-memory list — no DB re-query.
- Unread filter chip toggles `filterUnreadOnly` in UI state (actual filtering is a TODO; `series` list is currently unfiltered).

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
     ├── SetSortBy / SetFilterUnreadOnly → _state.update { ... }
     ├── RemoveFromLibrary → seriesRepository.removeFromLibrary(...) → _effects.send(ShowError?) on failure
     ├── OpenSeries → _effects.send(NavigateToSeries)
     │
     ▼
StateFlow<LibraryUiState>  ──collectAsStateWithLifecycle──►  LibraryContent re-render
Channel<LibraryEffect>     ──LaunchedEffect collect──────►  LibraryScreen (nav / snackbar)
```

Background data flow (init):
```
seriesRepository.observeLibrary()  ──collect──►  _state.update { series = it.sorted(sortBy) }
```

## Integration

- **Input**: `SeriesRepository.observeLibrary(): Flow<List<Series>>` — reactive stream from Room.
- **Output effects**: `NavigateToSeries(series)` → nav graph navigates to `SeriesScreen` with `sourceId` + `seriesUrl`; `ShowError` → snackbar (TODO).
- **Output callback**: `onNavigateToSettings` wired from `LibraryScreen` to `LibraryContent` for the settings gear icon.
- **No dependency on** `SourceRegistry`, `ChapterRepository`, or any concrete source. Follows AGENTS.md rule #3.

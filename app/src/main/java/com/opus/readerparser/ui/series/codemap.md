# app/src/main/java/com/opus/readerparser/ui/series/

## Responsibility

Displays full series details (cover, title, author, status, description, genre)
and its chapter list with read/downloaded state. Provides library toggle (add/remove)
and dispatches chapter taps to the correct reader (novel or manhwa).

## Design

### Files
| File | Kind | Responsibility |
|---|---|---|
| `SeriesScreen.kt` | Screen (orchestrator) | Wires `SeriesViewModel`, collects effects, dispatches reader navigation based on `ContentType`. Accepts separate `onNavigateToNovelReader` and `onNavigateToMangaReader` callbacks. |
| `SeriesContent.kt` | Content (stateless) | Renders a `LazyColumn` with a series header section (cover, metadata, expandable description) followed by chapter rows with read/downloaded indicators. Has `@Preview` variants. |
| `SeriesViewModel.kt` | ViewModel | Reads `sourceId` / `seriesUrl` from `SavedStateHandle`. Injects `SeriesRepository` + `ChapterRepository`. Observes chapter list reactively and fetches series details on init. |
| `SeriesUiState.kt` | Types | `SeriesUiState` data class, `SeriesAction` sealed interface, `SeriesEffect` sealed interface. |

### State / Action / Effect
- **UiState** — `series: Series?` (full details after fetch), `chapters: List<ChapterWithState>` (sorted newest-first, i.e. descending by chapter number), `isLoading`, `error`, `inLibrary`.
- **Action** — `Refresh` → re-fetch details + chapters; `ToggleLibrary(inLibrary)` → add/remove, updates `inLibrary` optimistically; `OpenChapter(chapter)` → navigate to reader.
- **Effect** — `NavigateToReader(chapter, type: ContentType)` → dispatches to novel or manhwa reader in `SeriesScreen`; `ShowError(message)` → snackbar (TODO).

### StubSeries pattern
The ViewModel constructs a minimal `Series` stub from `sourceId` + `seriesUrl` before the real data arrives so that `observeChapters(stubSeries)` can start collecting entries that already exist in the local DB. This avoids a race where the reactive chapter flow is set up after the details fetch completes.

### Reader navigation dispatch
`SeriesScreen` receives two navigation callbacks (one per `ContentType`) and branches on `effect.type` inside the `LaunchedEffect` collector. This keeps reader-specific routing out of the ViewModel.

### Chapter order
Chapters are sorted newest-first (descending by `chapter.number`) in the ViewModel via `sortedByDescending { chapter.number }` applied inside the `collect` block of `observeChapters`. This means the latest chapter appears at the top of the list regardless of the order Room returns them. The sorted order is updated reactively whenever the underlying Room flow emits a new list (e.g. after `refreshChapters`).

## Flow

```
User tap/input
    │
    ▼
SeriesContent (stateless)
    │  onAction(SeriesAction)
    ▼
SeriesViewModel.onAction(action)
    │
    ├── Refresh               → refresh() — re-fetch details + chapters
    ├── ToggleLibrary(bool)   → seriesRepository.addToLibrary|removeFromLibrary → _state.update { inLibrary }
    └── OpenChapter(chapter)  → _effects.send(NavigateToReader(chapter, type))
         │
         ▼
SeriesScreen.effects collector:
    NavigateToReader(chapter, NOVEL)  → onNavigateToNovelReader(sourceId, seriesUrl, chapterUrl)
    NavigateToReader(chapter, MANHWA) → onNavigateToMangaReader(sourceId, seriesUrl, chapterUrl)
    ShowError(message)                 → TODO snackbar
```

Background data flow (init):
```
chapterRepository.observeChapters(stubSeries)  ──collect──►  sortedByDescending { chapter.number } ──►  _state.update { chapters = it }
seriesRepository.refreshDetails(stubSeries)    ──then───►  _state.update { series = updated }
chapterRepository.refreshChapters(updated)      ──then───►  triggers observeChapters re-collect
seriesRepository.isInLibrary(...)                ──then───►  _state.update { inLibrary = true/false }
```

## Integration

- **Inputs**:
  - `SavedStateHandle["sourceId"]` and `["seriesUrl"]` — navigation arguments from the nav graph.
  - `SeriesRepository.refreshDetails(series)` — fills in author, description, status, etc. from source.
  - `SeriesRepository.isInLibrary(sourceId, url)` — checks local DB for library membership.
  - `SeriesRepository.addToLibrary` / `removeFromLibrary` — toggles library membership.
  - `ChapterRepository.observeChapters(series)` — reactive `Flow<List<ChapterWithState>>` from Room, includes read/downloaded/progress.
  - `ChapterRepository.refreshChapters(series)` — fetches latest chapter list from source and upserts into Room.
- **Output effects**:
  - `NavigateToReader(chapter, NOVEL)` → `NovelReaderScreen(sourceId, seriesUrl, chapterUrl)`.
  - `NavigateToReader(chapter, MANHWA)` → `MangaReaderScreen(sourceId, seriesUrl, chapterUrl)`.
  - `ShowError` → snackbar (TODO).
- **No direct access** to `SourceRegistry`, `ChapterContent`, or reader internals.

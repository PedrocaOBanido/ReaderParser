# app/src/main/java/com/opus/readerparser/ui/reader/

## Responsibility

Unified Reader screen that renders both novel (text) and manhwa (image pages)
content through dedicated renderers. One screen, ViewModel, state, and control
layout serves both content types; the renderer is selected by `ChapterContent`.

## Design

- **One screen, four files** — `ReaderScreen.kt`, `ReaderContent.kt`,
  `ReaderViewModel.kt`, `ReaderUiState.kt`. UiState also contains the sealed
  `ReaderAction` and `ReaderEffect` interfaces.
- **Content-type routing** — `SeriesScreen` passes `ContentType` to the Reader
  navigation destination. The ViewModel reads it from `SavedStateHandle` and
  uses it to validate loaded content and select the renderer.
- **Shared immersive chrome** — tap-to-toggle overlay with top bar (chapter
  name) and bottom bar (previous/next, progress, chapter list, download).
  Based on the former MANHWA reader interaction pattern, now applied to both.
- **Content-specific renderers** — `ReaderContent` branches once on
  `ChapterContent`:
  - `Text` → JavaScript-disabled WebView with scroll progress (percentage)
  - `Pages` → LazyColumn vertical image pages with page-count progress
- **Mismatch recovery** — if loaded content does not match the route content
  type, one forced network fetch bypasses the download cache. A persistent
  mismatch shows a retryable error.
- **Progress semantics preserved** — text progress is normalized `[0f, 1f]`
  persisted by `(sourceId, chapterUrl)`; page progress starts at zero and
  marks read on the final page.

## Flow

```
         SeriesScreen
              │
      onNavigateToReader(sourceId, seriesUrl, chapterUrl, contentType)
              │
              ▼
        ReaderScreen ─── hiltViewModel()
              │
              ▼
      ReaderViewModel(SavedStateHandle)
              │
     ┌────────┼────────┐
     │        │        │
  Text?    Pages?    Mismatch?
     │        │        │
     ▼        ▼        ▼
  WebView  LazyColumn  Forced fetch
  + scroll + page      → error
  progress   tracking
```

Within the Reader:

1. `ReaderScreen` wires `hiltViewModel()`, collects `state` with
   `collectAsStateWithLifecycle()`, and collects `effects` in a
   `LaunchedEffect(Unit)` block.
2. `ReaderScreen` delegates to `ReaderContent(state, isDarkTheme, onAction)`.
3. ViewModel `init` loads the current chapter from `SavedStateHandle` args
   including `contentType`.
4. Actions flow through `onAction()`: `SetProgress` (text), `SetPage` (pages),
   `PreviousChapter`, `NextChapter`, `OpenChapterList`, `DownloadChapter`,
   `SelectChapter`, `Retry`.
5. Effects are sent via `Channel<Effect>(BUFFERED)`: `NavigateToChapter`,
   `ShowChapterList`, `ShowError`, `ShowSnackbar`.
6. Chapter-to-chapter navigation pops the current destination and navigates
   to a new Reader destination preserving content type.

## Integration

- **`ChapterRepository`** — loads content, observes chapters, marks read,
  persists progress.
- **`DownloadEnqueuer`** — enqueues single-chapter downloads.
- **Navigation** (`Destinations.kt`) — single route
  `reader/{sourceId}/{seriesUrl}/{chapterUrl}/{contentType}`.
- **`ReaderChapterListSheet`** — shared bottom sheet for chapter selection.
- **Theme** — WebView uses theme colors (`BackgroundLight/Dark`,
  `PrimaryLight/Dark`) from `ui/theme/` for CSS injection.

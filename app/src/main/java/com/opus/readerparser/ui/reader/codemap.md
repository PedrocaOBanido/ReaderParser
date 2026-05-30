# app/src/main/java/com/opus/readerparser/ui/reader/

## Responsibility

Orchestrates the two distinct reader screens — novel (text) and manhwa (image
pages). This folder is a coordination point that groups the shared naming
convention and navigation pattern; each reader lives in its own subpackage.
The navigation graph dispatching to `novel/` or `manhwa/` is driven by
`ContentType` on the `Series` (see `NavGraph.kt` composable routing).

## Design

- **Mostly separate readers** — `novel/` and `manhwa/` keep separate screen,
  ViewModel, and UiState types. The only shared reader-specific composable is
  `ui/components/ReaderChapterListSheet.kt`; all other reader behavior stays
  split by content type.
- **4-file screen convention** enforced in both subpackages:
  `*Screen.kt`, `*Content.kt`, `*ViewModel.kt`, `*UiState.kt`.
  UiState files also contain the sealed `Action` and `Effect` interfaces.
- **Navigation shape is identical** — both accept `(onBack, onNavigateToChapter)`
  callbacks and read the same `SavedStateHandle` keys (`sourceId`, `seriesUrl`,
  `chapterUrl`). The `Destinations` object defines two distinct routes
  (`NOVEL_READER`, `MANGA_READER`) with the same argument template.
- **Chapter progression** uses a shared pattern: the ViewModel queries
  `ChapterRepository.observeChapters()` to determine next/previous chapter
  and emits `NavigateToChapter` effects. Neither reader knows about `SeriesScreen`;
  they only signal "go to this chapter" and let the navigation layer rebind.
- **Content-type dispatch** happens at the `SeriesScreen` level (or upstream),
  not inside this folder. `SeriesScreen.onNavigateToNovelReader` and
  `.onNavigateToMangaReader` are separate callbacks.

## Flow

```
             SeriesScreen
         ┌───────┴───────┐
         │               │
   (NOVEL type)    (MANHWA type)
         │               │
         ▼               ▼
  NovelReaderScreen  MangaReaderScreen
         │               │
  ┌──────┤               ├──────┐
  │      │               │      │
  ▼      ▼               ▼      ▼
  UiState/ViewModel   UiState/ViewModel
  Action/Effect       Action/Effect
```

Within each reader:

1. `*Screen` wires `hiltViewModel()`, collects `state` with
   `collectAsStateWithLifecycle()`, and collects `effects` in a
   `LaunchedEffect(Unit)` block.
2. `*Screen` delegates immediately to `*Content(state, onAction)`.
3. ViewModel `init` loads the current chapter from `SavedStateHandle` args.
4. Actions (`Load`, `PreviousChapter`, `NextChapter`, `SetProgress`/`SetPage`,
   `OpenChapterList`) flow through `onAction()`.
5. Effects (`NavigateToChapter`, `ShowChapterList`, `ShowError`) are sent via
   `Channel<Effect>(BUFFERED)` and collected by `*Screen` for one-shot side
   effects. `ShowChapterList` opens the shared `ReaderChapterListSheet`.
6. The navigation layer handles chapter-to-chapter transitions by
   `popUpTo(route, inclusive=true)` + navigate — each chapter load is a new
   composable instance.

## Integration

- **`ChapterRepository`** — both ViewModels depend on this single interface
  for loading content (`getContent`), reading progress (`observeChapters`),
  marking read (`markRead`), and persisting scroll/page progress
  (`setProgress`). Repository calls propagate to `SourceRegistry` for network
  fetches and to Room for local cache.
- **Navigation** (`Destinations.kt`) — routes `novel_reader/{sourceId}/{seriesUrl}/{chapterUrl}`
  and `manga_reader/{sourceId}/{seriesUrl}/{chapterUrl}`. SeriesScreen calls
  them via separate lambda callbacks. Within-reader chapter navigation emits
  `NavigateToChapter` effects that trigger the same route with a new `chapterUrl`,
  popping the current destination.
- **No dependency on other screens** — readers never import `SeriesScreen`,
  `BrowseScreen`, or any other feature package. They are pure rendering +
  ViewModel surfaces plugged into the nav graph.
- **Theme** — `NovelReaderContent` uses theme colors (`BackgroundLight/Dark`,
  `PrimaryLight/Dark`) from `ui/theme/` for WebView CSS injection.
  `MangaReaderContent` uses `MaterialTheme.colorScheme` directly.

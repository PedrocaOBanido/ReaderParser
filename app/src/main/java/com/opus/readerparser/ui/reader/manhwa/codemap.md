# app/src/main/java/com/opus/readerparser/ui/reader/manhwa/

## Responsibility

The manhwa (manga) reader screen — renders chapter image pages in a
`HorizontalPager` with previous/next chapter navigation, tracks reading
progress as the current page index, and auto-marks the chapter read when
the last page is reached.

Four files implementing the standard 4-file screen pattern:
`MangaReaderScreen.kt`, `MangaReaderContent.kt`, `MangaReaderViewModel.kt`,
`MangaReaderUiState.kt` (UiState also contains `Action` and `Effect`).

## Design

- **HorizontalPager-based paged layout** — Images are rendered page-by-page
  using Compose Foundation's `HorizontalPager` (paged LTR mode, the current
  layout). Pages are loaded via Coil 3 `AsyncImage` with `ColorPainter`
  placeholders and error fallbacks. The page count drives `rememberPagerState`.
- **Page tracking** — A `LaunchedEffect` observes `snapshotFlow { pagerState.currentPage }`
  and emits `MangaReaderAction.SetPage` on each swipe. The ViewModel updates
  `currentPage` in UiState and marks read when `currentPage == pages.lastIndex`.
  This is the reader's equivalent of progress.
- **No progress persistence** — Unlike the novel reader which persists scroll
  fraction via `setProgress()`, the manhwa reader currently does not persist
  page position beyond the viewmodel's in-memory `currentPage`. The
  `ChapterRepository.setProgress()` is unused here; progress is implicit via
  `markRead()` at the final page.
- **Left-to-right paging** — Pager is unidirectional LTR. A future layout
  switch (paged RTL, webtoon scroll) would branch in `MangaReaderContent` and
  be selected via `SettingsRepository`.
- **Chapter navigation** — Previous/next buttons in `TopAppBar` plus a page
  indicator ("N / M"). The ViewModel reuses the same `navigateChapter(forward)`
  pattern: query `ChapterRepository.observeChapters()` for adjacency and emit
  `NavigateToChapter`.
- **UiState shape** — Single data class with `chapter`, `pages: List<String>`
  (image URLs), `currentPage: Int`, `isLoading`, `error`, and two booleans for
  adjacent chapter presence. No HTML field.
- **Previews** — Three `@Preview` composables (loading, error, pages) target
  `MangaReaderContent`; `MangaReaderScreen` is never previewed per convention.

## Flow

```
MangaReaderScreen (wires VM, collects effects)
 │
 ├── collectAsStateWithLifecycle → MangaReaderUiState
 │   └── delegates to MangaReaderContent(state, onAction)
 │
 └── LaunchedEffect(Unit) collects effects channel
     ├── NavigateToChapter → onNavigateToChapter(chapter)
     ├── ShowChapterList   → TODO: bottom sheet
     └── ShowError         → TODO: snackbar

MangaReaderViewModel
 ├── init: loadCurrentChapter()
 │   └── observeChapters().first → find matching chapter → loadChapter()
 │
 ├── onAction(SetPage)     → update currentPage; markRead if last page
 ├── onAction(Previous/Next) → navigateChapter(forward)
 │   └── observeChapters().first → find adjacent → emit NavigateToChapter
 ├── onAction(Load)        → loadChapter(chapter)
 │   └── getContent → cast to ChapterContent.Pages → update state
 └── onAction(OpenChapterList) → emit ShowChapterList

MangaReaderContent
 ├── Scaffold with TopAppBar (prev, page counter, next, chapter list)
 ├── Loading state → CircularProgressIndicator
 ├── Error state   → error text + retry button
 ├── No pages      → "No pages available" text
 └── Pages loaded  → HorizontalPager
     ├── rememberPagerState(initialPage = state.currentPage)
     ├── LaunchedEffect: snapshotFlow{currentPage} → SetPage action
     └── Per page: AsyncImage with ColorPainter placeholder/error
         (ContentScale.FillWidth, full-width modifier)
```

## Integration

- **`ChapterRepository`** — sole domain dependency of `MangaReaderViewModel`.
  Used for: `getContent()` to fetch page URLs, `observeChapters()` to resolve
  chapter adjacency, and `markRead()` triggered when user reaches the last page.
- **`SavedStateHandle`** — receives `sourceId`, `seriesUrl`, `chapterUrl` from
  navigation arguments (set in `NavGraph.kt`'s `manga_reader` route).
- **`Destinations.mangaReader()`** — constructs the route URI. Chapter-to-chapter
  navigation re-navigates to the same route with a new `chapterUrl`, popping the
  old instance.
- **`SeriesScreen`** — creates the entry point; passes `onNavigateToMangaReader`
  lambda typed to `(Long, String, String) -> Unit`. `MangaReaderScreen` receives
  the inverse `onBack` and `onNavigateToChapter` callbacks.
- **Coil 3** — images loaded via `coil3.compose.AsyncImage` with
  `ContentScale.FillWidth`. Coil is configured globally via
  `NetworkModule` / `CoilModule`.
- **Distinct from the novel reader** — This screen is reached only when
  `Series.type == MANHWA`. It shares no composable or ViewModel logic with
  `NovelReaderScreen` and is registered on a separate navigation route
  (`manga_reader` vs `novel_reader`).

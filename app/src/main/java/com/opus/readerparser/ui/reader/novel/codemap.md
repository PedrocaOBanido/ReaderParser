# app/src/main/java/com/opus/readerparser/ui/reader/novel/

## Responsibility

The novel reader screen — renders sanitized chapter HTML inside a WebView with
custom CSS theming, tracks reading progress via scroll position, and enables
chapter-to-chapter navigation (previous/next) within the same series.

Four files implementing the standard 4-file screen pattern:
`NovelReaderScreen.kt`, `NovelReaderContent.kt`, `NovelReaderViewModel.kt`,
`NovelReaderUiState.kt` (UiState also contains `Action` and `Effect`).

## Design

- **WebView rendering** — Chapter HTML is wrapped in a styled `<html>` document
  with CSS variables for background, text, and link colors derived from the app
  theme. `javaScriptEnabled = false` and `builtInZoomControls = false`. This
  approach is chosen over Compose-native `AnnotatedString` conversion because
  novel HTML can contain arbitrary formatting, images, and tables that would
  require a full HTML → Compose renderer.
- **Progress tracking via scroll** — A `View.setOnScrollChangeListener` on the
  WebView computes `scrollY / (contentHeight - viewHeight)` and reports it as
  `NovelReaderAction.SetProgress(0f..1f)`. The ViewModel persists this via
  `ChapterRepository.setProgress()`.
- **Dark/light theme injection** — `NovelReaderContent` receives `isDarkTheme`
  from `NovelReaderScreen` (which calls `isSystemInDarkTheme()`). The WebView
  CSS is rebuilt when the theme changes via `remember(html, isDarkTheme)`.
- **Chapter navigation** — Previous/next buttons in the `TopAppBar` emit
  `NovelReaderEffect.NavigateToChapter`, which the Screen layer catches and
  routes to `onNavigateToChapter`. The navigation graph handles the composable
  swap with `popUpTo(inclusive=true)`. The ViewModel uses
  `ChapterRepository.observeChapters()` to determine adjacency.
- **UiState shape** — Single data class with `chapter`, `html`, `isLoading`,
  `error`, `progress` (0f..1f), and two booleans for adjacent chapter presence.
  Loading and error states are rendered inline within `NovelReaderContent`.
- **Previews** — Three `@Preview` composables (loading, error, dark content)
  target `NovelReaderContent` with sample data; `NovelReaderScreen` is never
  previewed per convention.

## Flow

```
NovelReaderScreen (wires VM, collects effects)
 │
 ├── collectAsStateWithLifecycle → NovelReaderUiState
 │   └── delegates to NovelReaderContent(state, onAction)
 │
 └── LaunchedEffect(Unit) collects effects channel
     ├── NavigateToChapter → onNavigateToChapter(chapter)
     ├── ShowChapterList   → TODO: bottom sheet
     └── ShowError         → TODO: snackbar

NovelReaderViewModel
 ├── init: loadCurrentChapter()
 │   └── observeChapters().first → find matching chapter → loadChapter()
 │
 ├── onAction(SetProgress)   → chapterRepository.setProgress()
 ├── onAction(Previous/Next) → navigateChapter(forward)
 │   └── observeChapters().first → find adjacent → emit NavigateToChapter
 ├── onAction(Load)          → loadChapter(chapter)
 │   └── getContent → cast to ChapterContent.Text → update state
 └── onAction(OpenChapterList) → emit ShowChapterList

NovelReaderContent
 ├── Scaffold with TopAppBar (prev, next, chapter list buttons)
 ├── Loading state → CircularProgressIndicator
 ├── Error state   → error text + retry button
 └── Content state → NovelWebView (AndroidView wrapping WebView)
     ├── Injects themed CSS at render time
     └── Reports scroll progress via onProgressChanged callback
```

## Integration

- **`ChapterRepository`** — sole domain dependency of `NovelReaderViewModel`.
  Used for: `getContent()` to fetch chapter HTML, `observeChapters()` to
  resolve chapter adjacency and restore saved progress, `setProgress()` to
  persist scroll position, and `markRead()` to auto-mark on load.
- **`SavedStateHandle`** — receives `sourceId`, `seriesUrl`, `chapterUrl` from
  navigation arguments (set in `NavGraph.kt`'s `novel_reader` route).
- **`Destinations.novelReader()`** — constructs the route URI. Chapter-to-chapter
  navigation re-navigates to the same route path with a new `chapterUrl`,
  popping the old instance.
- **`ui/theme/`** — `BackgroundLight`, `BackgroundDark`, `OnBackgroundLight`,
  `OnBackgroundDark`, `PrimaryLight`, `PrimaryDark` color constants imported
  for WebView CSS injection.
- **`SeriesScreen`** — creates the entry point; passes `onNavigateToNovelReader`
  lambda typed to `(Long, String, String) -> Unit`. The `NovelReaderScreen`
  receives the inverse `onBack` and `onNavigateToChapter` as navigation callbacks.

# Retrospective: unify-reader-screen

## What shipped

Merged separate NovelReader and MangaReader into one unified Reader screen with content-type branching. The new Reader handles both NOVEL (WebView) and MANHWA (image pages) via a single route with a `contentType` parameter.

### Key changes
- `ReaderViewModel` — unified state machine with mismatch recovery, debounced progress persistence, chapter navigation
- `ReaderContent` — shared immersive overlay chrome, WebView for text with scroll-based progress, LazyColumn for pages with page tracking
- `ReaderScreen` — wires ViewModel, effects, chapter list sheet
- `Destinations` / `NavGraph` — single `reader/{sourceId}/{seriesUrl}/{chapterUrl}/{contentType}` route
- `SeriesScreen` — unified `onNavigateToReader` callback
- Deleted 16 old files (novel/ + manhwa/ directories)
- Updated architecture.md, AGENTS.md, codemaps

### Net impact
- **-2,038 lines** (129 added, 2,167 deleted)
- **6 new files**, **16 deleted files**
- Zero new dependencies

## What went well

- Test-first approach caught issues early — mismatch recovery and progress clamping tests written before implementation
- Parallel fixer dispatches (VM batch + Content batch) saved time
- Oracle review found real bugs (WebView reload on recomposition, dead Retry, progress race)
- Splitting `restoreProgress` from `pendingProgress` resolved the scroll-vs-restore race cleanly

## What to watch

- WebView scroll restoration uses `webView.post` after `onPageFinished` — works for typical pages but may need testing with very long chapters
- `processLoadedContent()` is called from both normal and forced-network paths — good deduplication but adds a layer of indirection
- ManhwaPageList extraction improves cohesion but the main ReaderContent still has ~300 lines of overlay chrome

## Follow-ups

- Add Compose UI tests for tap-to-toggle visibility and Back button behavior
- Add navigation route construction tests (Destinations.READER builder)
- Consider caching WebView reference for explicit destroy lifecycle
- The parallel `html`/`pages` fields in ReaderUiState could be replaced with sealed `ChapterContent` to prevent impossible states

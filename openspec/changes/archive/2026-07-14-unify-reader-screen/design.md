## Context

Navigation branches on `ContentType` in `SeriesScreen`, then opens either `NovelReaderScreen` or `MangaReaderScreen`. Both screens collect state, handle the same effects, present the same chapter-list sheet, navigate between chapters, enqueue downloads, and load content through `ChapterRepository`. Their ViewModels duplicate most logic and differ mainly in how they interpret `ChapterContent` and mark progress/read state.

The rendered content genuinely differs: text is HTML displayed through a JavaScript-disabled WebView with scroll progress, while manhwa is an ordered image list with page progress. Sharing the screen does not require flattening those domain shapes. `ChapterContent` already provides the appropriate renderer boundary.

## Goals / Non-Goals

**Goals:**

- Use one Reader navigation destination, four-file screen set, state machine, and control layout for NOVEL and MANHWA.
- Retain dedicated text and page renderer composables selected from `ChapterContent`.
- Give both content types tap-to-toggle overlay controls based on the current MANHWA reader interaction.
- Preserve previous/next chapter, chapter selection, download, progress, read state, offline-first loading, and error behavior.
- Make Back and Retry actions functional.
- Remove duplicated reader implementations after callers and tests migrate.

**Non-Goals:**

- Converting novel HTML into image-like pages or converting manhwa pages into HTML.
- Changing the `Source` interface, `ChapterContent` variants, download format, Room schema, or chapter identity.
- Adding reader preferences, typography settings, orientation modes, prefetching, or new gestures.
- Redesigning `ReaderChapterListSheet` or changing chapter ordering.

## Decisions

### Decision: Use one route with explicit content type

The Reader destination carries `sourceId`, `seriesUrl`, `chapterUrl`, and `contentType`. `SeriesScreen` already knows the series content type, so passing it keeps the ViewModel independent of `SourceRegistry` and allows it to construct a correctly typed series stub before chapter content is loaded. Next/previous and chapter-sheet navigation preserve that route context.

Separate legacy reader destinations are removed rather than retained as compatibility aliases because they are internal navigation routes with no documented external reader deep links. Series deep links continue to open Series detail, not the reader.

### Decision: Let `ChapterContent` choose the renderer

Reader state holds the loaded `ChapterContent`. `ReaderContent` branches once:

- `ChapterContent.Text` uses the existing JavaScript-disabled WebView renderer and reports normalized scroll progress.
- `ChapterContent.Pages` uses the existing lazy vertical image renderer and reports the centered page index.

Unexpected content for the explicit route content type triggers one forced network fetch that bypasses `DownloadStore`, allowing a corrupt or stale mismatched download to recover. If the forced response still disagrees with the route type, Reader shows a visible retryable unexpected-content error. The sealed domain interface remains unchanged and no universal page model is introduced.

### Decision: Share screen chrome, not rendering internals

The current MANHWA overlay structure becomes shared Reader chrome: tapping content toggles top and bottom controls, the top bar displays chapter name and Back, and the bottom bar provides previous/next, chapter list, download, and progress. Text progress is shown as a percentage; page progress is shown as `current / total`. A small hidden-controls progress indicator remains available for both forms.

Renderer implementations stay private to Reader content unless reuse appears elsewhere. This avoids a new renderer abstraction hierarchy while satisfying the four-file screen rule.

### Decision: Preserve content-specific progress and read semantics

Text progress is normalized to `[0f, 1f]`, persisted by `(sourceId, chapterUrl)` for each distinct renderer-reported value, loaded from chapter state, and restored after WebView layout. A successfully displayed text chapter is marked read immediately, matching current behavior. Page content starts at page zero for each load, updates only in-screen current-page state, and marks the chapter read only when the final page is reached; page position is not persisted by this change. Chapter navigation and selection create a new Reader destination preserving source ID, series URL, and content type for the target chapter.

### Decision: Make advertised recovery and navigation functional

Back invokes the screen's navigation callback. Retry reloads the current chapter through the ViewModel without creating another destination. Cancellation is rethrown or allowed to cancel the active load rather than becoming an error. A newly loaded chapter clears content-specific state from the previous chapter before displaying loading feedback.

## Alternatives Rejected

- **Keep two screens and extract only controls:** rejected because the requested product behavior is one Reader screen and duplicated navigation/state/effect logic would remain.
- **Convert text into synthetic pages:** rejected because pagination requires layout-dependent splitting and would change novel progress, selection, and accessibility behavior.
- **Use one renderer abstraction with implementations:** rejected because a sealed `when` over the existing two content shapes is smaller and exhaustive.
- **Resolve content type from `SourceRegistry` in the ViewModel:** rejected because ViewModels may not depend on source infrastructure.
- **Infer type only after content loads:** rejected because correctly typed chapter-list context and mismatch validation are needed before and during loading.

## Risks / Trade-offs

- One state model contains fields meaningful to only one renderer; keeping them explicitly separated inside state is preferable to introducing polymorphic UI-state infrastructure.
- WebView gesture handling must coexist with tap-to-toggle controls without blocking scrolling or links.
- Navigation route replacement can break stale internal back-stack entries during process restoration; route tests and SavedStateHandle tests must cover the new arguments.
- Consolidation can accidentally change read/progress timing, so tests must lock existing semantics before deletion.

## Testing Expectations

- ViewModel tests cover loading `Text` and `Pages`, route-type mismatch, retry, cancellation, chapter navigation, chapter selection, download, progress, and read-state timing.
- Compose tests cover text and page renderers, shared controls, tap visibility, progress labels, Back, Retry, loading, errors, and chapter-list interaction.
- Navigation tests cover one Reader destination from both series types and chapter-to-chapter replacement.
- Repository tests continue proving downloaded Text and Pages are returned without a network call.
- Final review confirms the old screen implementations and routes are removed and canonical architecture/codemap guidance is synchronized.

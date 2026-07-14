## Why

NOVEL and MANHWA content currently use separate reader routes, screens, state models, ViewModels, and control layouts even though chapter navigation, download actions, chapter selection, loading, errors, and immersive controls are the same user workflow. This duplication has already drifted: MANHWA has tap-controlled overlay chrome, while NOVEL uses a fixed app bar, and both expose back/retry callbacks that do not perform their advertised actions.

## What Changes

- Replace the separate Novel and Manga reader routes and four-file screen sets with one Reader screen, state, ViewModel, and navigation destination.
- Keep specialized rendering inside the shared screen: HTML/WebView for `ChapterContent.Text` and vertical image pages for `ChapterContent.Pages`.
- Apply one MANHWA-style immersive reader chrome to both content forms, including tap-to-toggle controls, previous/next navigation, chapter list, download, progress, and back.
- Preserve each content form's existing read/progress semantics and offline-first `DownloadStore` behavior.
- Make loading failures retryable and wire back navigation instead of retaining inert UI actions.
- Update architectural and repository guidance from separate reader screens to one screen with content-specific renderers.

## Capabilities

### New Capabilities

- `unified-reader-screen`: one reader route and screen renders either text or pages behind shared controls while preserving content-specific behavior.

### Modified Capabilities

- `download-offline-reader`: the unified reader continues to prefer downloaded `Text` and `Pages` content before network access.
- `download-enqueue`: single-chapter downloads originate from the unified Reader instead of separate novel/manhwa screens without changing queue semantics.

## Impact

- **Architecture:** the invariant requiring separate reader screens changes. `ChapterContent` remains a sealed interface with exactly `Text(html)` and `Pages(imageUrls)`.
- **UI/navigation:** Novel and Manga reader screen packages and destinations converge into `ui/reader/` and one Reader destination. Series navigation supplies content type with chapter identity.
- **Presentation:** duplicated reader ViewModels, actions, effects, and state become one implementation with content-specific progress handling.
- **Data:** repositories, source contracts, download storage, series/chapter identity, and database schema do not change.
- **Documentation:** `architecture.md`, root `AGENTS.md`, and the repository codemap must reflect the unified screen and renderer boundary.
- **Dependencies:** No new dependency is introduced.

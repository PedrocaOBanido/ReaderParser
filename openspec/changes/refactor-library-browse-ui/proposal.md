## Why

Library and Browse present related series collections with inconsistent list-first UI, while missing titles from source detail parsing can leave saved library entries blank. The Library Unread filter is currently inert, so retaining it implies behavior the app does not define.

## What Changes

- Refactor Library and Browse's source catalog into a consistent, cover-first catalog presentation that adapts to available screen width.
- Preserve each surface's existing data and navigation behavior while using shared catalog UI patterns where appropriate.
- Ensure source-detail parsing supplies a usable title before a series is saved, and repair existing blank-title bookmarks from their source detail data.
- Remove the inert Library Unread filter rather than introducing unread-state semantics.

## Capabilities

### New Capabilities

- `adaptive-series-catalog-ui`: Library and Browse show series as a consistent cover-first catalog with an adaptive layout.
- `library-blank-title-repair`: existing bookmarked series with blank titles can be refreshed from their source detail data.

### Modified Capabilities

- `source-detail-parsing`: source detail results provide a usable series title at the parsing boundary before persistence.
- `library-filtering`: the unsupported Unread filter is no longer presented.
- `library-browse-catalog`: Library and Browse retain their current collection, search, and navigation behavior within the new catalog presentation.

## Impact

- **UI:** Library and Browse catalog composables, state, and tests change; the Series detail screen is explicitly out of scope.
- **Data flow:** bookmark/title repair is limited to the existing source-detail and repository flow. Series identity remains `(sourceId, url)`; the Source contract and database schema do not change.
- **Compatibility:** Samsung Search behavior, existing navigation, dependencies, and saved-series semantics remain unchanged.
- **Risks:** adaptive layouts must remain usable across compact and expanded widths, and title repair must tolerate unavailable or malformed source detail pages without losing existing bookmark data.

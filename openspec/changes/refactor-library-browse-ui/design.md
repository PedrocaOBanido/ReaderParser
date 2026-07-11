## Context

Library and Browse present related series collections through separate list-first UI. Source detail parsing may also leave a series title blank, which can persist into a saved library bookmark. Library already owns refresh and persistence paths for bookmarked series; repair must use those paths rather than introduce a new persistence mechanism. Series identity remains `(sourceId, url)`.

The app uses Jetpack Compose and Material theme tokens. Library must retain its Samsung Search integration and existing sort, search, and removal behavior. Browse must retain its source selection, series-detail navigation, and pagination behavior. The current Library Unread control has no defined behavior and is inert.

## Goals / Non-Goals

**Goals:**

- Parse a source detail title and use the incoming nonblank title only when parsing cannot provide one.
- Repair existing blank-title library bookmarks through the existing refresh and persistence flow without overwriting bookmark data when source retrieval or parsing fails.
- Make at most one repair attempt per blank bookmark during a Library screen lifecycle, so unavailable sources do not cause repeated requests.
- Present Library and Browse with one reusable, cover-first series card in an adaptive compact/expanded grid using Material theme colors and dimensions.
- Keep Browse search explicit, show loading and retry feedback, and prevent stale requests from replacing current results.
- Remove the inert Library Unread UI while preserving Library's existing supported behavior.

**Non-Goals:**

- Database schema, entity identity, Source interface, dependencies, navigation, and Series detail redesign.
- Defining unread-state semantics or replacing Samsung Search.
- Changing Browse source selection, pagination rules, or the meaning of an empty search query.
- Bulk migration or background repair of every saved bookmark.

## Decisions

### Decision: Resolve titles at the source-detail parsing boundary

Each source detail parser extracts the detail-page title. When forming the parsed result, that title is preferred; the incoming title is used only when it is nonblank and the parsed title is blank or unavailable. This keeps title correctness close to the source-specific markup and avoids adding a persistence-only title workaround.

Blank titles already persisted in Library are refreshed through the existing refresh/persistence flow. A successful refresh persists the repaired title; a failed request, malformed response, or parse failure leaves the existing bookmark unchanged. Library tracks repair attempts for its current screen lifecycle and does not retry the same blank bookmark endlessly until that lifecycle ends.

### Decision: Share a cover-first catalog card and adapt the container grid

Library and Browse use the same reusable card for series cover, title, and their existing surface-specific interaction. Their containers choose an adaptive grid that remains practical at compact and expanded widths rather than fixing a device-specific column count. The card and grid use Material theme color, typography, shape, and spacing tokens so the presentation follows the current theme without a parallel visual system.

### Decision: Make Browse search an explicit submission

Changing Browse modes or source selection does not fetch a blank query. A request begins only after an explicit search submission. Existing source selection, navigation to a series, and pagination remain intact. Browse exposes loading and retry feedback for failed requests, and request identity is guarded so a slower earlier request cannot overwrite the state for a later query, source, mode, or page.

### Decision: Remove unsupported Unread UI without changing supported Library behavior

The Unread control is removed because no unread-state behavior exists behind it. Samsung Search integration, sorting, text search, and bookmark removal remain on their established flows, preventing the visual refactor from changing Library semantics.

## Alternatives Rejected

- **Persist the incoming title before parsing:** rejected because an available detail-page title is more authoritative and the fallback must only cover missing parsed data.
- **Add a schema field, migration, or background title-repair job:** rejected because repair can use existing refresh and persistence behavior and must not broaden storage or lifecycle scope.
- **Separate Library and Browse card implementations:** rejected because the requested catalog presentation is shared and duplicate cards would drift.
- **Fetch Browse results on mode switch with an empty query:** rejected because it causes unrequested network work and does not reflect an explicit search intent.
- **Implement an Unread filter:** rejected because the app has no defined unread semantics.

## Risks / Trade-offs

- Source detail pages can be unavailable or their markup can change; preserving the existing bookmark on failure avoids data loss but leaves its blank title until a later lifecycle can retry.
- Lifecycle-scoped attempt tracking prevents request loops but deliberately does not continuously repair a failing bookmark while the Library screen remains open.
- Adaptive grids trade a stable list density for responsive card sizes; compact-width tests must verify titles and actions remain usable.
- Request guards add state coordination to Browse, but prevent incorrect results from stale asynchronous responses.

## Testing Expectations

- Add parser tests covering extracted detail titles and fallback to a nonblank incoming title only when extraction is blank or absent.
- Add repository/ViewModel tests showing blank-title repair persists a successful refresh, preserves existing bookmark data on errors, and makes no repeated attempt in one Library screen lifecycle.
- Add Compose UI tests for compact and expanded catalog layouts, shared card rendering, and removal of Unread UI while retaining supported Library search, sort, removal, and Samsung Search behavior.
- Add Browse ViewModel/UI tests for explicit submission, no blank-query fetch on mode switch, loading/retry feedback, pagination/source-selection preservation, and stale-request rejection.

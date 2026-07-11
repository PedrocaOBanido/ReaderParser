## 1. Source-detail title parsing

- [x] 1.1 Add parser cases proving each source-detail parser prefers a nonblank extracted detail-page title over the incoming title.
- [x] 1.2 Add parser cases proving a nonblank incoming title is retained only when the extracted detail-page title is blank or unavailable, and a blank incoming title is not treated as a usable fallback.
- [x] 1.3 Update each source-detail parser to extract and apply the detail-page title with the specified fallback behavior.

## 2. Library blank-title repair

- [x] 2.1 Add repository/ViewModel tests for a blank bookmarked title repaired by a successful existing source-detail refresh while preserving its `(sourceId, url)` identity and other bookmark data.
- [x] 2.2 Add repository/ViewModel tests that failed retrieval, response handling, or parsing leaves the existing blank-title bookmark unchanged.
- [x] 2.3 Add ViewModel tests proving a blank bookmark is attempted at most once during one Library screen lifecycle and may be attempted again in a new lifecycle.
- [x] 2.4 Trigger blank-title repair from the Library lifecycle through the existing refresh and persistence flow, recording attempted bookmark identities for that lifecycle.

## 3. Shared adaptive series catalog

- [x] 3.1 Add Compose tests for the shared card's cover, title, and caller-provided series interaction.
- [x] 3.2 Add Compose tests for usable compact and expanded adaptive catalog layouts, including readable titles and available interactions.
- [x] 3.3 Create one reusable Material-theme-aware cover-first series card for Library and Browse.
- [x] 3.4 Create the shared adaptive grid/container and apply it to Library and Browse collection results.

## 4. Library catalog behavior cleanup

- [x] 4.1 Update Library Compose tests to assert that Unread is absent and supported sort, text search, removal, and Samsung Search behavior remains available in the catalog presentation.
- [x] 4.2 Remove the inert Unread filtering control and any now-unused UI state or presentation logic without changing supported Library behavior.

## 5. Browse explicit search and request handling

- [x] 5.1 Add Browse ViewModel tests proving mode or source changes with no submitted query issue no blank-query request, while an explicit submission uses the selected source and mode.
- [x] 5.2 Add Browse ViewModel tests for loading state, retrying the submitted failed request, preserved pagination/source selection, and rejection of stale query, source, mode, and page responses.
- [x] 5.3 Add Browse Compose tests for explicit search submission plus loading and retry feedback, while retaining series-detail navigation and pagination interactions.
- [x] 5.4 Update Browse state and ViewModel so searches begin only on explicit submission, failures can retry the submitted request, and request identity prevents stale responses from replacing current results.
- [x] 5.5 Update Browse content to submit searches explicitly and render loading/retry feedback using the shared catalog presentation.

## 6. Final verification

- [x] 6.1 Run the targeted source parser, Library repository/ViewModel, Browse ViewModel, and Compose UI tests added or updated for this change.
- [ ] 6.2 Run `./gradlew :app:testDebugUnitTest`.
- [ ] 6.3 Run `./gradlew :app:assembleDebug`.

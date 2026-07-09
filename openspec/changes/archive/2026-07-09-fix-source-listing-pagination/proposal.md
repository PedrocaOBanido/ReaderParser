## Why

The change is now broader than simple listing pagination. The app needs infinite-scroll style Browse behavior, FreeWebNovel chapter-list aggregation across paginated chapter pages, and a regression-proof split between AsuraScans latest vs popular parsing.

## What Changes

- Add auto-load-on-end behavior to browse UI while keeping the manual Load more fallback.
- Fix FreeWebNovel chapter-list aggregation so all paginated chapter pages are included without changing the Source contract.
- Add regression tests proving AsuraScans latest and popular remain distinct at the URL/parser level.
- Preserve existing single-page source modes where intentionally terminal.
- Update tests/specs to prove each slice before any production change.

## Capabilities

### New Capabilities

- Infinite-scroll browse loading with manual fallback.
- Complete chapter list aggregation for paginated novel sources.
- Regression-proof distinction between AsuraScans latest and popular listing modes.

### Modified Capabilities

- Browse load-more behavior.
- Chapter-list fetching for novel sources.
- Pagination/regression tests for affected source flows.

## Impact

This should primarily affect UI/content tests and a small number of source implementations. No Source contract changes are expected. Existing findings remain useful: FreeWebNovel popular/search stay single-page, AsuraScans latest stays homepage-only, and the earlier listing-pagination evidence still holds.

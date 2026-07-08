## Why

Library search currently filters series titles in memory, which bypasses Samsung Search and can surface stale or non-indexed rows.

## What Changes

- Library search uses Samsung Search's public ContentProvider query API when the search box is non-blank
- Query hits are resolved back to local Room series rows so the UI shows the stored display title and cover
- Search results are restricted to rows that are both in the user's library and indexable (downloaded)
- Provider failures are surfaced separately from empty search results
- Blank search still shows the normal library list

## Capabilities

### New Capabilities
- `use-samsung-library-search`: Library search is backed by Samsung Search query results instead of local title matching.

### Modified Capabilities
- `samsung-search-indexable-series`: indexed content remains limited to downloaded series.

## Impact

- **LibraryViewModel**: switches from local title filtering to repository-backed Samsung Search lookups for non-blank queries
- **SeriesRepositoryImpl**: adds a library-search path that queries Samsung Search and maps hits back to local Room rows
- **SamsungSearchClient**: adds a query wrapper over `ContentResolver.query()`
- **Tests**: update repository, ViewModel, and Samsung Search client coverage

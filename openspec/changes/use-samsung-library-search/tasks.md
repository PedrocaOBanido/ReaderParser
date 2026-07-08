## 1. OpenSpec and contracts

- [x] 1.1 Add a change spec for Samsung-backed Library search.
- [x] 1.2 Add a pure domain result type that distinguishes search success from provider failure.
- [x] 1.3 Extend `SeriesRepository` with a Library-search method.

## 2. Samsung Search query support

- [x] 2.1 Extend `SearchProviderDelegate` and `ContentResolverDelegate` with `query()`.
- [x] 2.2 Add a Samsung Search query wrapper that returns hits or a failure state.
- [x] 2.3 Cover query success, empty cursor, null cursor, and exception cases in `SamsungSearchClientTest.kt`.

## 3. Repository and Room mapping

- [x] 3.1 Add a DAO lookup that only returns local rows that are both in-library and downloaded/indexable.
- [x] 3.2 Implement Library search in `SeriesRepositoryImpl` using Samsung Search results and preserve provider order.
- [x] 3.3 Keep browse search behavior unchanged.

## 4. Library UI

- [x] 4.1 Update `LibraryViewModel` to use repository-backed Samsung search for non-blank queries.
- [x] 4.2 Keep blank-query library display reactive and sorted locally.
- [x] 4.3 Surface provider failure separately from empty search results.
- [x] 4.4 Revalidate active Samsung search when library membership or indexable state changes.

## 5. Tests and verification

- [x] 5.1 Update the fake repository and repository/ViewModel tests for the new search path.
- [x] 5.2 Run targeted unit tests and the debug assemble.

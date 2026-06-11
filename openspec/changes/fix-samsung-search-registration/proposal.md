## Why

Samsung Search integration never activates on device because of two bugs in `SamsungSearchClient`:

1. **Availability probe is wrong.** `isAvailable()` calls `ContentResolver.getType()` against the Samsung Search authority URI, but that provider returns `null` from `getType()`. The result: `isAvailable()` always returns `false`, so `register_schema` is never called at startup and the entire search integration is skipped.

2. **`register_schema` extras are incomplete.** The current call sends `schema-content` bytes in the extras bundle but omits the required `"name"` key. The Samsung Search provider reads the schema name from `extras["name"]`, not from the `arg` parameter. Without it, registration fails even if the availability probe were correct.

Both bugs prevent the `samsung-search-indexable-series` integration from working on any device.

## What Changes

- Replace the `getType()` availability probe with a `ContentResolver.call()` using method `request_search_api_version`. A non-null bundle containing a plausible `response_search_api_version` means Samsung Search is installed and reachable.
- Add `extras.putString("name", SCHEMA_NAME)` alongside the existing `extras.putByteArray("schema-content", schemaBytes)` in `registerSchema()`.
- Update existing androidTest assertions for `isAvailable()` to match the new probe semantics (request_search_api_version call instead of getType).
- Verify on device with `adb` that schema registration succeeds and index directories are created.

## Capabilities

### Modified Capabilities
- `samsung-search-indexable-series`: the availability probe and schema registration requirements are tightened to match the actual Samsung Search public API contract.

### New Capabilities

## Impact

- **`SamsungSearchClient.kt`** (primary): `isAvailable()` and `registerSchema()` methods change; `SearchProviderDelegate` interface unchanged.
- **`SamsungSearchClientTest.kt`**: test assertions for availability probe updated.
- **No App/SearchIndexSyncer flow changes**: the startup gate in `App.kt` (`isAvailable → registerSchema → startObserving`) stays identical.
- **No schema XML or manifest changes**: the bundled schema asset and permission declarations are unaffected.

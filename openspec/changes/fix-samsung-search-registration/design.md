## Context

Samsung Smart Suggestions' v2 public ContentProvider API (`content://com.samsung.android.smartsuggestions.search/v2`) does not implement `getType()` — it returns `null`. The correct way to probe availability is `ContentResolver.call()` with method `request_search_api_version`, which returns a `Bundle` containing `response_search_api_version`. The provider also requires the schema name in `extras["name"]` when calling `register_schema`, not in the `arg` parameter.

ReaderParser's `SamsungSearchClient` currently uses both incorrect patterns, so the integration never activates.

## Goals / Non-Goals

**Goals:**
- Fix the availability probe to use `request_search_api_version`
- Fix `registerSchema()` to include the schema name in extras
- Keep the existing startup flow (`App.kt`) unchanged
- Update tests to match the corrected behavior

**Non-Goals:**
- Redesigning `SearchIndexSyncer` or `App.kt` startup flow
- Adding new capabilities or fields to the schema
- Changing the `SearchProviderDelegate` interface

## Decisions

### 1. Availability probe via `request_search_api_version`

**Decision:** Replace `delegate.getType(AUTHORITY_URI) != null` with `delegate.call(AUTHORITY_URI, "request_search_api_version", null, null)` and treat the provider as available only when the returned bundle is non-null **and** contains a plausible `response_search_api_version` value (present and ≥ 1).

**Rationale:** This is the documented Samsung Search public API probe. The provider returns a `Bundle` with `response_search_api_version` (an int ≥ 1). A non-null bundle with a missing or zero version would indicate an unexpected provider state; rejecting it avoids silent misoperation. The `getType()` method is not implemented by this provider.

### 2. Schema name in extras

**Decision:** Add `extras.putString("name", SCHEMA_NAME)` before calling `delegate.call(AUTHORITY_URI, METHOD_REGISTER_SCHEMA, SCHEMA_NAME, extras)`.

**Rationale:** The Samsung Search `PublicSchemaManager.registerSchema()` reads the schema name from `extras.getString("name")`, not from the `arg` parameter. Without this, registration silently fails because the name is null.

### 3. Minimal diff

**Decision:** Change only `isAvailable()` and `registerSchema()` in `SamsungSearchClient.kt`. Do not touch `App.kt`, `SearchIndexSyncer`, the schema XML, or the `SearchProviderDelegate` interface.

**Rationale:** The startup flow and sync logic are correct — the bug is isolated to the two client methods. Keeping the diff minimal reduces risk.

## Risks / Trade-offs

- **[Risk] Samsung Search API version changes** → The probe checks for a non-null bundle with `response_search_api_version` ≥ 1. Future API versions with a higher number will still pass. If the method name changes or the version value is removed, the probe will fail gracefully (exception → `isAvailable()` returns `false`).
- **[Risk] Existing tests break** → Availability test assertions change from `getType` to `call` semantics. Updated tests cover: probe success, probe failure, exception during probe.
- **[Trade-off] No version negotiation** → We check `response_search_api_version` against a minimum of 1 but don't negotiate a specific version. Acceptable because the current integration only uses basic `register_schema` and `bulkInsert`, which are stable across versions.

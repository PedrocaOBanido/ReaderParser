## Context

Library search should use Samsung Search's public query path so search behavior matches the device index. The query must be resolved back to local Room rows so the app continues to display the canonical series title and cover data.

## Goals / Non-Goals

**Goals:**
- Use Samsung Search query results for active Library search
- Preserve provider hit ordering while a query is active
- Show only downloaded/indexable series that are also in the user's library
- Distinguish provider failure from empty results

**Non-Goals:**
- Changing the Source contract
- Adding new schema fields or manifest permissions
- Reworking browse search or library sort/filter semantics outside search mode

## Decisions

### 1. Query through ContentProvider

**Decision:** Add a small `query()` wrapper to `SamsungSearchClient` and call `ContentResolver.query(SCHEMA_URI, projection, selection, selectionArgs, null)` for active Library search.

**Rationale:** This stays on the public API surface and avoids local fallback matching.

### 2. Query result shape

**Decision:** Return a sealed result from the repository search path so UI code can tell empty results apart from provider failures.

**Rationale:** `null` or an empty list alone cannot represent both cases.

### 3. Local row resolution

**Decision:** Resolve each Samsung Search hit back to a local Room row by composite key and only keep rows that are in-library and still indexable.

**Rationale:** The UI should use local display data, and the business rule requires the row to remain both downloaded and saved in the library.

### 4. Preserve provider order

**Decision:** Do not locally sort active search results.

**Rationale:** Samsung Search already ranks results; re-sorting would discard that ordering.

## Risks / Trade-offs

- **[Risk] Provider selection syntax may vary** → keep the selection simple and test against the fake delegate; failures are surfaced separately from empty results.
- **[Trade-off] One local lookup per hit** → simpler than building a custom SQL order-preserving join for a small search result set.

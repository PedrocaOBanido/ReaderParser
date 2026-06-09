## Context

Samsung Smart Suggestions exposes a public ContentProvider API (`content://com.samsung.android.smartsuggestions.search/v2`) that allows any app to register a search schema, insert/update/delete documents, and query them — all without modifying the search engine codebase. The API is documented in `search/wiki/api/publicApi.md` and implemented by `PublicSchemaManager`, `PublicProviderHelper`, and `PublicSearcherManager`.

ReaderParser (`com.opus.readerparser`) is an external Android app that reads webnovels and manhwa. Its searchable entities are **Series** (title, author, description, genres, status, type) and **Chapter** (name, number, seriesUrl). The app stores data in Room.

The v2 path requires changes only inside the ReaderParser app — it self-registers and self-manages its search index.

## Goals / Non-Goals

**Goals:**
- Make ReaderParser series and chapters searchable through Samsung Smart Suggestions' v2 public API
- ReaderParser self-manages its schema lifecycle (register on first launch, unregister on uninstall)
- ReaderParser syncs its Room database changes to the search index in near-real-time
- Support the full public API query DSL (match, prefix, fuzzy, bool) for rich search experiences

**Non-Goals:**
- S Finder integration (v2 public API indices are not automatically included in S Finder's cross-index search)
- Modifying the Samsung Search engine codebase
- Moneta/PDE integration
- Modifying ReaderParser's own UI beyond adding a search-via-Samsung-Search capability

## Decisions

### 1. Schema name and structure

**Decision:** Use schema name `com.opus.readerparser.library` (must start with caller package name per `PublicSchemaManager.validateSchemaName()`).

Fields:
- `_id` (string, key field) — composite: `{sourceId}:{seriesUrl}` for series, `{sourceId}:{seriesUrl}:{chapterUrl}` for chapters
- `title` (text with dual analyzer) — series title or chapter name
- `author` (text) — series author
- `description` (text, stored=false) — series description
- `genres` (text) — comma-separated genre list
- `doc_type` (string) — "series" or "chapter"
- `status` (string) — ONGOING, COMPLETED, etc.
- `type` (string) — NOVEL or MANHWA
- `source_url` (string, stored=true) — deep link back to ReaderParser
- `chapter_number` (numeric) — chapter number for sorting

**Rationale:** The schema name must pass `validateSchemaName()` which requires it to start with the caller's package name. The field set mirrors the v1 design for fair comparison.

### 2. Schema XML delivery

**Decision:** Bundle the schema XML as an asset in ReaderParser's APK and read it at runtime for `register_schema`.

**Rationale:** The public API's `register_schema` call requires `schema-content` as a `byte[]`. Bundling as an asset is simpler than generating XML programmatically and allows the schema to be version-controlled alongside the app code.

**Alternative considered:** Generate schema XML at runtime — rejected because it's error-prone and the schema is static.

### 3. Data sync strategy

**Decision:** Use a `RoomDatabaseCallback` + `Flow<List<...>>` observer pattern:
1. On app start, register schema (idempotent — `register_schema` overwrites)
2. Observe Room database for Series and Chapter table changes
3. On change, diff against last-synced state and call `bulkInsert()` / `update()` / `delete()` on the v2 ContentProvider
4. Handle `ACTION_UPDATE_INDEX` broadcast for full re-index on corruption

**Rationale:** ReaderParser already uses Room with Flow-based observation. The sync layer is a thin adapter between Room change events and ContentProvider calls. This avoids polling and keeps the index fresh.

**Alternative considered:** Periodic WorkManager sync — rejected because it introduces unnecessary latency and battery cost when Room already provides change notifications.

### 4. Query template registration

**Decision:** Register a default query template `search` that combines prefix + fuzzy matching on `title` and `author` fields, with boost on `title`.

**Rationale:** The public API supports query templates for reusable search patterns. A default template simplifies client-side search and ensures consistent ranking. Clients can still use raw `query-json` for custom queries.

### 5. Error handling and resilience

**Decision:** 
- Wrap all ContentProvider calls in try/catch with exponential backoff retry
- If Samsung Search is not installed (ContentProvider not found), silently disable the search integration
- Log all API responses and status codes for debugging
- On `ACTION_UPDATE_INDEX` broadcast, clear local sync state and re-index everything

**Rationale:** The public API is an optional enhancement — ReaderParser must function normally even if Samsung Search is unavailable.

## Risks / Trade-offs

- **[Risk] Samsung Search not installed** → Graceful degradation: catch `IllegalArgumentException` from ContentResolver, disable search integration, log warning
- **[Risk] Schema registration fails** → Check status code in response bundle; retry on next app launch; never block app startup
- **[Risk] Large bulk insert (1000+ documents)** → Batch inserts in chunks of 100; use `bulkInsert()` which is optimized for batch operations
- **[Risk] Schema version mismatch after app update** → Include `version` attribute in schema XML; `register_schema` overwrites previous schema; trigger full re-index on version change
- **[Trade-off] No S Finder integration** → v2 public API indices are not included in S Finder's cross-index search; this is a platform limitation, not a design choice

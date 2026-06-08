## Context

Samsung Smart Suggestions' search engine (`search/` module, `:search` process) indexes data from system apps via predefined Lucene indices. Each predefined index has: a schema XML in assets, a `config.json` entry, a `UriInfo` enum for URI routing, a `QueryInfo` subclass for query building, and either a `Collector` (for observed data sources) or manual ingestion (for pushed data like Settings).

ReaderParser (`com.opus.readerparser`) is an external Android app that reads webnovels and manhwa. Its searchable entities are **Series** (title, author, description, genres, status, type) and **Chapter** (name, number, seriesUrl). The app stores data in Room and is the sole source of truth for its library.

The v1 path requires changes inside the `search/` module to make ReaderParser a first-class predefined index.

## Goals / Non-Goals

**Goals:**
- Make ReaderParser series and chapters searchable through Samsung Smart Suggestions' v1 internal query pipeline
- Support fuzzy, prefix, synonym, and locale-aware search (Korean chosung, Chinese pinyin) on title and author fields
- Enable S Finder and other system search clients to surface ReaderParser results
- Gate the feature behind a `Rune` flag for safe rollout

**Non-Goals:**
- Semantic/hybrid search for ReaderParser (v1 uses keyword-only pipeline like Settings)
- Real-time sync — periodic or on-demand indexing is sufficient
- Moneta/PDE integration — this is purely a DeepSky search stack change
- Modifying ReaderParser's own UI or internal architecture

## Decisions

### 1. Single index vs separate series/chapter indices

**Decision:** Use a single index `com.opus.readerparser` with both Series and Chapter documents, distinguished by a `doc_type` field.

**Rationale:** Settings uses a single index for all its preference items. ReaderParser's entities are small and related (chapters belong to series). A single index simplifies query routing and avoids cross-index joins. If chapter volume becomes large, splitting can be done later.

**Alternative considered:** Separate `com.opus.readerparser.series` and `com.opus.readerparser.chapters` indices — rejected because it doubles the routing/config complexity and the query pipeline doesn't support cross-index joins natively.

### 2. Collector vs manual ingestion

**Decision:** Implement a `ReaderParserCollector` that pulls data from ReaderParser via a ContentProvider exposed by the ReaderParser app.

**Rationale:** Most predefined indices (Contacts, Calendar, Gallery) use collectors that observe ContentProvider change URIs and sync incrementally. This is the established pattern for data sources that change over time. ReaderParser's Room database changes when users add/remove series or download chapters, so a collector with change observation is appropriate.

**Alternative considered:** Manual push via `ProviderHelper.insertReaderParser()` (like Settings) — rejected because ReaderParser data changes frequently (new chapters, library updates) and manual push would require ReaderParser to know about Samsung Search internals.

### 3. Schema field design

**Decision:** Model the schema after Settings' pattern but with additional fields for genres and description.

Fields:
- `_id` (string, key field) — composite key: `{sourceId}:{seriesUrl}` for series, `{sourceId}:{seriesUrl}:{chapterUrl}` for chapters
- `title` (text) — series title or chapter name
- `author` (text) — series author
- `description` (text, stored=false) — series description for search matching only
- `genres` (text) — comma-separated genre list
- `doc_type` (string) — "series" or "chapter"
- `source_url` (string, stored=true) — deep link back to ReaderParser

Analyzers: reuse the same fieldType definitions as Settings (text, spell, chosung, pinyin, keyword_field) for consistency.

### 4. Query handling

**Decision:** Create `ReaderParserQueryInfo` extending `SuggestableQueryInfo`, modeled after `SettingsQueryInfo` but searching `title`, `author`, and `description` fields.

**Rationale:** `SettingsQueryInfo` is the closest analog — it builds fuzzy + prefix + synonym + chosung + pinyin queries on text fields. ReaderParser needs the same treatment on its title/author fields, plus description for broader matching.

### 5. Feature gating

**Decision:** Add a `Rune` flag `readerparser_search_enabled` in `feature-config/` that gates:
- Whether the collector starts
- Whether `ConfigManager.isAppEnabled()` returns true for the index
- Whether query routing includes the ReaderParser index

**Rationale:** All new predefined indices should be gated for safe rollout and quick rollback.

## Risks / Trade-offs

- **[Risk] ReaderParser not installed** → Collector returns empty; `ConfigManager` can check `installed_package_name` and disable gracefully
- **[Risk] Large library (1000+ series)** → Collector uses incremental sync with `LAST_MODIFIED` timestamp; batch inserts via `IndexableList`
- **[Risk] Cross-process data flow** → ReaderParser must expose a ContentProvider with read permission; standard Android IPC pattern, well-tested in existing collectors
- **[Trade-off] Single index for series+chapters** → Simpler routing but queries return mixed types; mitigate by including `doc_type` in projection so clients can filter

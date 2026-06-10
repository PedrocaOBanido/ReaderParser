## Context

ReaderParser (`com.opus.readerparser`) is an Android webnovel/manhwa reader with a Room database. Series are identified by composite key `(sourceId, url)`. Chapters have a `downloaded: Boolean` column that is set to `true` after files are written to disk and reset to `false` by `DownloadRepository.deleteDownload()`. Samsung Smart Suggestions exposes a v2 public ContentProvider API at `content://com.samsung.android.smartsuggestions.search/v2` that lets apps self-register schemas and push documents without modifying the search engine.

The existing `onboard-readerparser-v2` change covers the full v2 integration (series + chapters, library-driven). This change narrows the scope to a focused MVP: **only series with at least one downloaded chapter** are indexed, and the `source_url` deep link opens the Series screen.

## Goals / Non-Goals

**Goals:**
- Publish a Samsung Search index of series that have at least one chapter with `downloaded = true`
- Deep-link from search results to the existing Series screen
- Rebuild the index when chapter download state changes
- Support full re-index via `ACTION_UPDATE_INDEX` broadcast
- Degrade gracefully when Samsung Search is unavailable

**Non-Goals:**
- Indexing chapters as separate documents
- Indexing series without any downloaded chapters (library-only or browse-only series)
- Deep-linking to the reader from search results
- Query template registration (MVP uses raw `query-json` or the default template)
- Real-time incremental sync with diffing (MVP re-queries the projection and bulk-inserts the full set on each rebuild)
- Schema versioning or migration (first version, `register_schema` is idempotent)

## Decisions

### 1. Indexable series query

**Decision:** Add a Room DAO query that returns distinct series that have at least one chapter with `downloaded = true`.

```sql
SELECT DISTINCT s.*
FROM series s
INNER JOIN chapters c
  ON s.sourceId = c.sourceId AND s.url = c.seriesUrl
WHERE c.downloaded = 1
ORDER BY s.title ASC
```

**Rationale:** A SQL JOIN + DISTINCT is the simplest way to derive the indexable set from existing data. No new columns, no new tables, no new Room schema version. The query is re-entrant and always reflects current state.

**Alternative considered:** Observe individual `chapters.downloaded` changes and maintain a separate `search_index` table — rejected because it adds complexity and a second source of truth that can drift.

### 2. Schema name and fields

**Decision:** Schema name `com.opus.readerparser.series` (must start with caller package per `validateSchemaName()`). Fields:
- `_id` (string, key) — composite `{sourceId}:{seriesUrl}`
- `title` (text) — series title
- `author` (text) — series author
- `description` (text, stored=false) — for search matching only
- `genres` (text) — comma-separated
- `status` (string) — ONGOING, COMPLETED, etc.
- `type` (string) — NOVEL or MANHWA
- `source_url` (string, stored=true) — deep link to Series screen

**Rationale:** Minimal field set. No `chapter_name`, `chapter_number`, or `doc_type` — chapters are excluded from the index entirely. The `_id` format is `{sourceId}:{seriesUrl}` which is the series composite PK.

### 3. Deep link target

**Decision:** `source_url` uses an intent-based deep link or a custom URI scheme that resolves to `Destinations.SERIES` route (`series/{sourceId}/{seriesUrl}`).

**Rationale:** The Series screen already accepts `sourceId` (Long) and `seriesUrl` (String) as navigation arguments. A custom URI scheme (`readerparser://series/{sourceId}/{seriesUrl}`) registered in the manifest is the simplest way to deep-link from Samsung Search results.

**Alternative considered:** Use an Android Intent with explicit component — rejected because it couples Samsung Search to ReaderParser's internal Activity class name.

### 4. Sync strategy: bulk rebuild

**Decision:** On each sync trigger, query the indexable-series projection, delete all existing documents from the search index, and bulk-insert the current set. No incremental diffing.

**Rationale:** The indexable set is small (only series with downloads — typically dozens, not thousands). A full rebuild is cheap and avoids the complexity of tracking per-document insert/update/delete state. `ACTION_UPDATE_INDEX` uses the same code path.

**Alternative considered:** Incremental diff with insert/update/delete per document — rejected for MVP because it adds significant complexity for negligible performance gain on small sets.

### 5. Trigger for rebuild

**Decision:** Observe the `chapters` table via Flow (specifically the `observeChapters` or a new `observeDownloadState` query) and debounce rebuilds. Use WorkManager `OneTimeWorkRequest` for the `ACTION_UPDATE_INDEX` broadcast handler.

**Rationale:** Room Flow already notifies on any `chapters` table change (including `downloaded` flips). Debouncing prevents thrashing during batch downloads. WorkManager is appropriate for the broadcast-triggered rebuild because it's an external, fire-once event.

**Alternative considered:** Poll periodically — rejected because Flow observation is reactive and more efficient.

### 6. Graceful degradation

**Decision:** Wrap all ContentProvider calls in try/catch. If `isAvailable()` returns false (Samsung Search not installed or ContentProvider missing), skip all search operations. Log at warning level.

**Rationale:** ReaderParser must function normally without Samsung Search. The public API is an optional enhancement.

### 7. Stale `downloaded` caveat

**Decision:** Accept that `downloaded` can become stale if files are removed outside the normal app delete path (e.g. user deletes files via file manager). Document this as a known limitation. Do not add a validation step that checks file existence on every sync — it would be expensive and is out of scope for MVP.

**Rationale:** The normal delete path (`DownloadRepository.deleteDownload()`) correctly resets the flag. Stale entries are a minor UX issue (search returns a series whose chapters are gone), not a crash or data corruption risk.

## Risks / Trade-offs

- **[Risk] Samsung Search not installed** → Graceful degradation: catch exceptions, log, continue. No user-facing impact.
- **[Risk] Schema registration fails** → Retry on next app launch; never block startup.
- **[Risk] Stale `downloaded` flag** → Accept for MVP. Series may appear in search when files are gone. Follow-up: optional file-existence check in a later iteration.
- **[Trade-off] Full rebuild vs incremental** → Simpler code, negligible cost for small index. If index grows to 500+ series, revisit with diffing.
- **[Trade-off] No chapter documents** → Narrower utility. Users searching for a specific chapter title won't find it. This is intentional for MVP.

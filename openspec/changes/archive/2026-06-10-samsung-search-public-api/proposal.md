## Why

ReaderParser stores downloaded chapters locally, but users cannot discover those series from outside the app (e.g. Samsung Smart Suggestions search). Publishing a narrow public search index lets users jump directly from system search into a series they have already downloaded — without exposing chapters, library-only series, or the reader UI.

## What Changes

- ReaderParser registers a Samsung Search v2 public API schema exposing **only series** that have at least one chapter with `downloaded = true` in the Room database
- A Room projection query joins `series` and `chapters` (DISTINCT on series PK) to produce the indexable set
- Series documents include: `_id`, `title`, `author`, `description`, `genres`, `status`, `type`, `source_url` — no chapter fields
- The `source_url` deep link opens the existing **Series screen** (not the reader)
- A `SearchIndexSyncer` observes the Room database for changes to `chapters.downloaded` and rebuilds the index incrementally; a WorkManager one-time-request handles full rebuilds on `ACTION_UPDATE_INDEX`
- Graceful degradation: if Samsung Search is unavailable, the integration silently does nothing
- Known caveat: if chapter files are removed outside the normal app delete path, `downloaded` can become stale; this is acceptable for MVP

## Capabilities

### New Capabilities
- `samsung-search-indexable-series`: Public API integration where ReaderParser publishes an index of series with downloaded chapters to Samsung Search via the v2 ContentProvider contract. Covers schema registration, indexable-series projection query, sync logic, broadcast handling, manifest permissions, and graceful degradation.

### Modified Capabilities

## Impact

- **ReaderParser app** (primary): new `SearchIndexSyncer` module, Room projection query for indexable series, schema XML asset, manifest permission declarations, WorkManager one-time-request for full re-index
- **Samsung Search engine**: no code changes — uses existing public v2 API pipeline
- **Room**: new DAO query (no schema change — `downloaded` column already exists on `chapters`)
- **No domain model changes**: the projection is a read-only Room query; domain `Series` model is untouched

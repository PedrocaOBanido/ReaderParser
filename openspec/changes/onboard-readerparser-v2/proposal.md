## Why

ReaderParser (`com.opus.readerparser`) is a webnovel/manhwa reader app whose library content (series titles, authors, genres, chapters) should be discoverable through Samsung Smart Suggestions' search. Onboarding via the **v2 public API** lets ReaderParser self-register its schema and push data at runtime — no changes to the Samsung Search engine codebase required. This is the external integration path, suitable for third-party apps that ship independently of the system image.

## What Changes

- ReaderParser app registers a schema (`com.opus.readerparser.series`) via `ContentResolver.call("register_schema", ...)` against `content://com.samsung.android.smartsuggestions.search/v2`
- ReaderParser app registers an optional query template for common search patterns (prefix + fuzzy on title/author)
- ReaderParser app pushes Series and Chapter data via `bulkInsert()` on `content://.../v2/com.opus.readerparser.series`
- ReaderParser app handles `ACTION_UPDATE_INDEX` broadcast to re-index on corruption
- ReaderParser app declares required permissions (`WRITE`, `READ`, `UPDATE_INDEX`) in its AndroidManifest
- Schema defines fields: `_id`, `title`, `author`, `description`, `genres`, `status`, `type`, `chapter_name`, `chapter_number`, `source_url`
- ReaderParser observes its own Room database changes and syncs inserts/updates/deletes to the search index

## Capabilities

### New Capabilities
- `readerparser-v2-public-api`: Public API integration where ReaderParser self-registers its search schema and manages its own index lifecycle (register, insert, update, delete, search) through the Samsung Search v2 ContentProvider contract.

### Modified Capabilities

## Impact

- **ReaderParser app** (primary): new `SearchIndexer` module that observes Room database changes and syncs to Samsung Search via ContentProvider calls; schema XML bundled as asset or generated at runtime; manifest permission declarations
- **Samsung Search engine**: no code changes — uses existing `PublicSchemaManager`, `PublicProviderHelper`, `PublicSearcherManager` pipeline
- **Process boundary**: ReaderParser's `:app` process communicates with the `:search` process via standard ContentProvider IPC (`call()`, `bulkInsert()`, `query()`, `update()`, `delete()`)
- **No Moneta impact**: this change is entirely within the DeepSky search stack's public API surface
- **No feature flag needed**: the public API is already gated by permissions and schema validation

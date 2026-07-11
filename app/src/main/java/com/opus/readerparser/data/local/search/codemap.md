# data/local/search — Samsung Search v2 integration

This package integrates with Samsung Search v2 to make downloaded series
searchable from the device's universal search.

## Files

| File | Responsibility |
|---|---|
| `SearchIndexSyncer.kt` | Observes the indexable-series Room query and keeps the Samsung Search index in sync. Exposes `rebuildIndex()` (one-shot, called by the rebuild worker) and `startObserving()` (flow-based, called at app launch). Maps `SeriesEntity` → `ContentValues` matching the registered schema fields. |
| `SamsungSearchClient.kt` | Wraps ContentProvider calls: schema registration, bulk document insertion (batched), and index clearing. All methods catch exceptions internally so Samsung Search unavailability is silent. Also defines `SearchProviderDelegate` (ContentResolver abstraction) and the production `ContentResolverDelegate`. |
| `SamsungSearchSchema.kt` | Reads the schema XML asset (`search/samsung-search-indexable-series.xml`) and provides the raw `ByteArray` for the `register_schema` call. Exposes `fake()` for tests. |

## Data flow

1. `SearchIndexSyncer.startObserving()` collects `SeriesDao.observeIndexableSeries()` (series with downloaded chapters).
2. On each emission (debounced), or on explicit `rebuildIndex()`:
   - `SamsungSearchClient.isAvailable()` checks ContentProvider reachability.
   - `deleteAll()` clears the existing index.
   - `bulkInsert()` inserts the current series set as `ContentValues` documents.
3. `SamsungSearchRebuildWorker` calls `rebuildIndex()` on `ACTION_UPDATE_INDEX` broadcast.

## Schema fields

| Field | Source | Notes |
|---|---|---|
| `_id` | `"{sourceId}:{url}"` | Composite primary key |
| `title` | `SeriesEntity.title` | |
| `author` | `SeriesEntity.author` | May be null |
| `description` | `SeriesEntity.description` | `stored=false` in schema |
| `genres` | `genresJson` → comma-separated | Parsed via `GenreJson.jsonToGenres` |
| `status` | `SeriesEntity.status` | |
| `type` | `SeriesEntity.content.type` | NOVEL / MANHWA |
| `source_url` | Deep link to Series screen | `readerparser://series/{sourceId}/{encodedUrl}` |

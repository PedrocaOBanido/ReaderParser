## 1. Schema Definition and Bundling

- [x] 1.1 Create `app/src/main/assets/search/samsung-search-indexable-series.xml` with schema name `com.opus.readerparser.series` and fields: `_id` (string, key), `title` (text), `author` (text), `description` (text, stored=false), `genres` (text), `status` (string), `type` (string), `source_url` (string, stored=true). No chapter fields.
- [x] 1.2 Create `SamsungSearchSchema.kt` utility that reads the schema XML asset and returns it as `ByteArray` for the `register_schema` call.

## 2. Samsung Search API Client

- [x] 2.1 Create `SamsungSearchClient.kt` with methods: `registerSchema()`, `bulkInsert()`, `deleteAll()`. Each method wraps the corresponding `ContentResolver` call against `content://com.samsung.android.smartsuggestions.search/v2`.
- [x] 2.2 Implement `isAvailable()` check that probes the ContentProvider and returns `false` if Samsung Search is not installed.
- [x] 2.3 Add error handling with try/catch and logging to all `SamsungSearchClient` methods. Failed calls SHALL NOT throw to callers.
- [x] 2.4 Implement batch splitting in `bulkInsert()` — split documents into chunks of 100 and call `ContentResolver.bulkInsert()` for each chunk.

## 3. Manifest Permissions

- [x] 3.1 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.WRITE" />` to `AndroidManifest.xml`.
- [x] 3.2 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.READ" />` to `AndroidManifest.xml`.
- [x] 3.3 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.UPDATE_INDEX" />` to `AndroidManifest.xml`.

## 4. Room DAO Query — Indexable Series Projection

- [x] 4.1 Add `observeIndexableSeries()` query to `SeriesDao` that returns `Flow<List<SeriesEntity>>` using `SELECT DISTINCT s.* FROM series s INNER JOIN chapters c ON s.sourceId = c.sourceId AND s.url = c.seriesUrl WHERE c.downloaded = 1 ORDER BY s.title ASC`.
- [x] 4.2 Add `getIndexableSeries()` suspend counterpart to `SeriesDao` for one-shot retrieval (used by WorkManager rebuild).

## 5. Data Sync — SearchIndexSyncer

- [x] 5.1 Create `SearchIndexSyncer.kt` that observes the indexable-series Room query via Flow.
- [x] 5.2 Implement `toContentValues()` mapper that converts `SeriesEntity` to `ContentValues` with fields matching the registered schema (`_id`, `title`, `author`, `description`, `genres`, `status`, `type`, `source_url`).
- [x] 5.3 Implement full-rebuild sync: on each emissions (debounced), call `deleteAll()` then `bulkInsert()` with the current indexable series set.
- [x] 5.4 Implement `rebuildIndex()` public method for explicit full re-index (called by broadcast handler).
- [x] 5.5 Wire `SearchIndexSyncer` to start observation from `App.kt` `onCreate()` after schema registration, using a coroutine scope tied to the application lifecycle.

## 6. Schema Registration on App Start

- [x] 6.1 In `App.kt` `onCreate()`, call `SamsungSearchClient.registerSchema()` if `isAvailable()` returns true.
- [x] 6.2 Handle registration failure gracefully: log warning, skip sync setup, continue normal app operation.

## 7. Deep Link Handling

- [x] 7.1 Define a custom URI scheme (`readerparser://series/{sourceId}/{seriesUrl}`) in `AndroidManifest.xml` via an intent-filter on the main Activity.
- [x] 7.2 Implement deep link parsing in the Activity's `onCreate()` / `onNewIntent()` to extract `sourceId` and `seriesUrl` and navigate to the Series screen route.

## 8. ACTION_UPDATE_INDEX Broadcast Receiver

- [x] 8.1 Create `SamsungSearchUpdateReceiver.kt` extending `BroadcastReceiver`. On receive, schedule a WorkManager `OneTimeWorkRequest` that calls `SearchIndexSyncer.rebuildIndex()`.
- [x] 8.2 Declare the receiver in `AndroidManifest.xml` with `android:permission="com.samsung.android.smartsuggestions.search.permission.SEND_ACTION_UPDATE_INDEX"` and intent filter for `com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX`.

## 9. Hilt DI Wiring

- [x] 9.1 Create `SearchModule.kt` in `core/di/` that provides `SamsungSearchClient` and `SearchIndexSyncer` as singletons via Hilt.
- [x] 9.2 Inject `SearchIndexSyncer` into `App.kt` to start observation on app launch.

## 10. Testing and Verification

- [x] 10.1 Write tests for `SamsungSearchClient` verifying correct ContentResolver calls for register, bulkInsert, and deleteAll operations. (Tightened: introduced `SearchProviderDelegate` test seam with `FakeSearchProviderDelegate`; androidTest covers register success/failure/null, availability probe, batch splitting, deleteAll, and exception resilience. Moved to androidTest because the `SearchProviderDelegate` interface and `SamsungSearchClient` use `Bundle`, `Uri`, `ContentValues` — Android framework classes unavailable in local JVM tests.)
- [x] 10.2 Write tests for `SearchIndexSyncer` verifying rebuild path and `toContentValues()` mapping. (Rebuild-path tests — `rebuildIndex` clears/inserts, empty-list handling, unavailable-client skip, `startObserving` triggers rebuild and cancels previous — moved to androidTest alongside the mapping tests because the `FakeSearchClient`/`FakeSearchProviderDelegate` fakes use `ContentValues`, `Uri`, `Bundle`.)
- [x] 10.3 Write androidTest DAO tests for `observeIndexableSeries()` / `getIndexableSeries()`: series with downloaded chapters included, series without excluded, `inLibrary = false` with downloaded chapters included, DISTINCT dedup, title ordering, one-shot parity.
- [x] 10.4 Write unit tests for `SamsungSearchUpdateReceiver` verifying that receiving `ACTION_UPDATE_INDEX` schedules a WorkManager one-time request. (Tightened: JVM test verifies action gating; androidTest with `WorkManagerTestInitHelper` verifies actual scheduling via tag, null-safety, wrong-action rejection, and single-enqueue count.)
- [x] 10.5 Run `./gradlew :app:testDebugUnitTest` to verify no regressions.
- [x] 10.6 Run `./gradlew :app:assembleDebug` to verify compilation.

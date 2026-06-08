## 1. Schema Definition and Bundling

- [ ] 1.1 Create `app/src/main/assets/search/schema.xml` in ReaderParser with fields: `_id` (string, key), `title` (text with dual analyzer), `author` (text), `description` (text, stored=false), `genres` (text), `doc_type` (string), `status` (string), `type` (string), `source_url` (string, stored=true), `chapter_number` (numeric). Schema name attribute: `com.opus.readerparser.library`.
- [ ] 1.2 Create `SamsungSearchSchema.kt` utility in ReaderParser that reads the schema XML asset and returns it as `ByteArray` for the `register_schema` call.

## 2. Samsung Search API Client

- [ ] 2.1 Create `SamsungSearchClient.kt` in ReaderParser with methods: `registerSchema()`, `registerQueryTemplate()`, `bulkInsert()`, `update()`, `delete()`, `query()`. Each method wraps the corresponding `ContentResolver` call against `content://com.samsung.android.smartsuggestions.search/v2`.
- [ ] 2.2 Implement `isAvailable()` check in `SamsungSearchClient` that probes the ContentProvider and returns `false` if Samsung Search is not installed.
- [ ] 2.3 Add error handling with try/catch and logging to all `SamsungSearchClient` methods. Failed calls SHALL NOT throw to callers.
- [ ] 2.4 Implement batch splitting in `bulkInsert()` — split documents into chunks of 100 and call `ContentResolver.bulkInsert()` for each chunk.

## 3. Manifest Permissions

- [ ] 3.1 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.WRITE" />` to ReaderParser's `AndroidManifest.xml`.
- [ ] 3.2 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.READ" />` to ReaderParser's `AndroidManifest.xml`.
- [ ] 3.3 Add `<uses-permission android:name="com.samsung.android.smartsuggestions.search.permission.UPDATE_INDEX" />` to ReaderParser's `AndroidManifest.xml`.

## 4. Data Sync — Room Observer

- [ ] 4.1 Create `SearchIndexSyncer.kt` in ReaderParser that observes Room database Series and Chapter table changes via Flow queries.
- [ ] 4.2 Implement diff logic in `SearchIndexSyncer`: compare current Room state against last-synced snapshot to determine inserts, updates, and deletes.
- [ ] 4.3 Implement `toContentValues()` mapper that converts `Series` and `Chapter` domain models to `ContentValues` with fields matching the registered schema.
- [ ] 4.4 Wire `SearchIndexSyncer` to call `SamsungSearchClient.bulkInsert()` for new documents, `update()` for changed documents, and `delete()` for removed documents.
- [ ] 4.5 Start `SearchIndexSyncer` from `App.kt` `onCreate()` after schema registration, using a coroutine scope tied to the application lifecycle.

## 5. Schema Registration on App Start

- [ ] 5.1 In `App.kt` `onCreate()`, call `SamsungSearchClient.registerSchema()` if `isAvailable()` returns true.
- [ ] 5.2 After successful schema registration, call `SamsungSearchClient.registerQueryTemplate()` with the default `search` template (prefix + fuzzy on title/author, title boosted 2x).
- [ ] 5.3 Handle registration failure gracefully: log warning, skip sync setup, continue normal app operation.

## 6. ACTION_UPDATE_INDEX Broadcast Receiver

- [ ] 6.1 Create `SamsungSearchUpdateReceiver.kt` in ReaderParser extending `BroadcastReceiver`. On receive, clear last-synced state and trigger full re-index via `SearchIndexSyncer`.
- [ ] 6.2 Declare the receiver in `AndroidManifest.xml` with `android:permission="com.samsung.android.smartsuggestions.search.permission.SEND_ACTION_UPDATE_INDEX"` and intent filter for `com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX`.

## 7. Hilt DI Wiring

- [ ] 7.1 Create `SearchModule.kt` in `core/di/` that provides `SamsungSearchClient` and `SearchIndexSyncer` as singletons via Hilt.
- [ ] 7.2 Inject `SearchIndexSyncer` into `App.kt` to start observation on app launch.

## 8. Testing and Verification

- [ ] 8.1 Write unit tests for `SamsungSearchClient` verifying correct ContentResolver calls for register, insert, update, delete, and query operations.
- [ ] 8.2 Write unit tests for `SearchIndexSyncer` diff logic: given old and new Room snapshots, verify correct insert/update/delete operations are generated.
- [ ] 8.3 Write unit tests for `toContentValues()` mapper verifying all domain model fields are correctly mapped.
- [ ] 8.4 Write integration test: register schema → insert documents → query → verify results returned.
- [ ] 8.5 Run `./gradlew :app:testDebugUnitTest` in ReaderParser to verify no regressions.
- [ ] 8.6 Run `./gradlew :app:assembleDebug` in ReaderParser to verify compilation.

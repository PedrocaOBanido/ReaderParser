## ADDED Requirements

### Requirement: Schema registration via public API
ReaderParser SHALL register a search schema named `com.opus.readerparser.library` by calling `ContentResolver.call()` with method `register_schema` against `content://com.samsung.android.smartsuggestions.search/v2`. The schema XML SHALL be bundled as an asset and sent as `byte[]` in the `schema-content` extra. The schema name SHALL start with the app's package name to pass `validateSchemaName()`.

#### Scenario: Successful schema registration
- **WHEN** ReaderParser launches for the first time on a device with Samsung Search installed
- **THEN** it SHALL call `register_schema` with the bundled schema XML and receive a status code of `0` (SUCCESS)

#### Scenario: Samsung Search not installed
- **WHEN** ReaderParser launches and Samsung Search ContentProvider is not available
- **THEN** ReaderParser SHALL catch the exception, log a warning, and continue normal operation without search integration

#### Scenario: Idempotent re-registration
- **WHEN** ReaderParser launches again after schema was already registered
- **THEN** `register_schema` SHALL overwrite the existing schema and return SUCCESS

### Requirement: Data insertion via bulkInsert
ReaderParser SHALL insert Series and Chapter documents into the search index by calling `ContentResolver.bulkInsert()` on `content://com.samsung.android.smartsuggestions.search/v2/com.opus.readerparser.library`. Each `ContentValues` entry SHALL include `_id`, `title`, `doc_type`, and `source_url` at minimum.

#### Scenario: Initial library index
- **WHEN** ReaderParser has 50 series and 500 chapters in its Room database
- **THEN** it SHALL call `bulkInsert()` with all 550 documents and the search index SHALL contain all of them

#### Scenario: Batch size handling
- **WHEN** the document count exceeds 100
- **THEN** ReaderParser SHALL split the insert into batches of 100 to avoid OOM and transaction timeouts

### Requirement: Data update via ContentProvider update
ReaderParser SHALL update existing documents in the search index by calling `ContentResolver.update()` on the v2 URI with a JSON selection query targeting the document's `_id` field and new `ContentValues` for changed fields.

#### Scenario: Series title change
- **WHEN** a series title changes in ReaderParser's Room database
- **THEN** ReaderParser SHALL call `update()` with a selection matching the series `_id` and new `title` value, and the search index SHALL reflect the updated title

### Requirement: Data deletion via ContentProvider delete
ReaderParser SHALL remove documents from the search index by calling `ContentResolver.delete()` on the v2 URI with a JSON selection query targeting the document's `_id` field.

#### Scenario: Series removed from library
- **WHEN** a user removes a series from ReaderParser's library
- **THEN** ReaderParser SHALL call `delete()` with a selection matching the series `_id` and all associated chapter `_id`s, and those documents SHALL be removed from the search index

### Requirement: Room database change observation
ReaderParser SHALL observe its Room database for changes to Series and Chapter tables using Flow-based queries. On each change, it SHALL diff against the last-synced state and issue the appropriate `bulkInsert()`, `update()`, or `delete()` calls to the v2 ContentProvider.

#### Scenario: New series added
- **WHEN** a new series is added to the Room database
- **THEN** the observer SHALL detect the insertion and call `bulkInsert()` with the new series document within 5 seconds

#### Scenario: Chapter downloaded
- **WHEN** a new chapter is downloaded and stored in the Room database
- **THEN** the observer SHALL detect the insertion and call `bulkInsert()` with the new chapter document

### Requirement: Query template registration
ReaderParser SHALL register a default query template named `search` via `ContentResolver.call()` with method `register_query_template`. The template SHALL define prefix + fuzzy matching on `title` and `author` fields with title boosted 2x.

#### Scenario: Template registered successfully
- **WHEN** ReaderParser registers the query template after schema registration
- **THEN** subsequent queries using `query-template-name: "search"` SHALL use the defined matching strategy

### Requirement: ACTION_UPDATE_INDEX broadcast handling
ReaderParser SHALL declare a BroadcastReceiver for `com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX` protected by permission `com.samsung.android.smartsuggestions.search.permission.SEND_ACTION_UPDATE_INDEX`. On receiving this broadcast, ReaderParser SHALL clear its sync state and perform a full re-index.

#### Scenario: Index corruption recovery
- **WHEN** Samsung Search sends `ACTION_UPDATE_INDEX` because the index was corrupted
- **THEN** ReaderParser SHALL clear its last-synced state and call `bulkInsert()` with all current Series and Chapter documents

### Requirement: Manifest permission declarations
ReaderParser SHALL declare the following permissions in its AndroidManifest.xml: `com.samsung.android.smartsuggestions.search.permission.WRITE`, `com.samsung.android.smartsuggestions.search.permission.READ`, and `com.samsung.android.smartsuggestions.search.permission.UPDATE_INDEX`.

#### Scenario: Permissions declared
- **WHEN** the app is installed
- **THEN** the AndroidManifest SHALL contain all three Samsung Search permissions

### Requirement: Graceful degradation
ReaderParser SHALL function normally even when Samsung Search is not available. All ContentProvider calls SHALL be wrapped in try/catch blocks. Failed API calls SHALL be logged but SHALL NOT crash the app or block user interactions.

#### Scenario: Search unavailable does not block app
- **WHEN** Samsung Search is not installed and the user opens ReaderParser
- **THEN** the app SHALL start normally, log that search integration is disabled, and all non-search features SHALL work correctly

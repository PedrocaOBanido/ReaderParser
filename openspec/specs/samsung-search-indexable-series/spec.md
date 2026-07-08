# samsung-search-indexable-series

## Purpose

Integrates ReaderParser with Samsung Smart Suggestions Search so that
downloaded series appear as indexable documents in the Samsung Search results.
Covers schema registration, indexable-series projection, document field
mapping, deep-link navigation, index lifecycle (rebroadcast, rebuild,
debounced updates), manifest permissions, graceful degradation, and bulk
insert batching.

---

## ADDED Requirements

### Requirement: Schema registration via public API
ReaderParser SHALL register a search schema named `com.opus.readerparser.series` by calling `ContentResolver.call()` with method `register_schema` against `content://com.samsung.android.smartsuggestions.search/v2`. The schema XML SHALL be bundled as an asset and sent as `byte[]` in the `schema-content` extra. The schema name SHALL be sent in the `extras` bundle under key `"name"` — the provider does not read the schema name from the `arg` parameter. The schema name SHALL start with the app's package name to pass `validateSchemaName()`.

The bundled schema asset SHALL use a `<schema>` root element with `name`, `package`, `version`, and `keyFieldName` attributes, and SHALL declare `<fieldType>` elements for each field type used (e.g. `StringField`, `TextField`). The `<search-scheme>` root format is not accepted by Samsung Search.

ReaderParser SHALL determine Samsung Search availability by calling `ContentResolver.call()` with method `request_search_api_version` against the same authority URI. The provider is considered available only when the returned bundle is non-null **and** contains a plausible `response_search_api_version` value (present and ≥ 1). `ContentResolver.getType()` SHALL NOT be used as an availability probe because the Samsung Search provider returns `null` from `getType()`.

#### Scenario: Successful schema registration
- **WHEN** ReaderParser launches for the first time on a device with Samsung Search installed
- **THEN** it SHALL call `register_schema` with extras containing both `"name"` (the schema name string) and `"schema-content"` (the bundled schema XML as `byte[]`), and receive a status code of `0` (SUCCESS)

#### Scenario: Availability probe via request_search_api_version
- **WHEN** ReaderParser checks whether Samsung Search is available
- **THEN** it SHALL call `ContentResolver.call()` with method `request_search_api_version` against `content://com.samsung.android.smartsuggestions.search/v2`
- **AND** a non-null return bundle containing `response_search_api_version` ≥ 1 SHALL be treated as "available"

#### Scenario: Availability probe returns null
- **WHEN** the `request_search_api_version` call returns a `null` bundle, or the bundle lacks `response_search_api_version` / the value is < 1
- **THEN** `isAvailable()` SHALL return `false` and ReaderParser SHALL skip schema registration and sync

#### Scenario: Availability probe throws
- **WHEN** the `request_search_api_version` call throws an exception (e.g. provider not installed)
- **THEN** `isAvailable()` SHALL catch the exception, log a warning, and return `false`

#### Scenario: Samsung Search not installed
- **WHEN** ReaderParser launches and Samsung Search ContentProvider is not available
- **THEN** ReaderParser SHALL catch the exception, log a warning, and continue normal operation without search integration

#### Scenario: Idempotent re-registration
- **WHEN** ReaderParser launches again after schema was already registered
- **THEN** `register_schema` SHALL overwrite the existing schema and return SUCCESS

#### Scenario: register_schema extras contain name key
- **WHEN** ReaderParser calls `register_schema`
- **THEN** the extras bundle SHALL contain `extras["name"]` set to `"com.opus.readerparser.series"` and `extras["schema-content"]` set to the schema XML bytes

#### Scenario: register_schema passes null for arg
- **WHEN** ReaderParser calls `register_schema`
- **THEN** the `arg` parameter of `ContentResolver.call()` SHALL be `null` — the schema name in `extras["name"]` is the authoritative source

#### Scenario: Schema XML uses schema root with fieldTypes
- **WHEN** ReaderParser loads the bundled schema asset
- **THEN** the XML SHALL have a `<schema>` root element (not `<search-scheme>`) with `name`, `package`, `version`, and `keyFieldName` attributes, and SHALL declare `<fieldType>` elements for each field type used

### Requirement: Indexable series projection query
ReaderParser SHALL maintain a Room DAO query that returns distinct series having at least one chapter with `downloaded = true`. The query SHALL join the `series` and `chapters` tables on `(series.sourceId, series.url) = (chapters.sourceId, chapters.seriesUrl)` and select `DISTINCT series.*`. The result set SHALL be ordered by series title ascending.

#### Scenario: Series with downloaded chapters is included
- **WHEN** a series has three chapters and one chapter has `downloaded = true`
- **THEN** the series SHALL appear in the indexable series projection

#### Scenario: Series with no downloaded chapters is excluded
- **WHEN** a series has five chapters and none have `downloaded = true`
- **THEN** the series SHALL NOT appear in the indexable series projection

#### Scenario: Series not in library but with downloaded chapters is included
- **WHEN** a series has `inLibrary = false` but has at least one chapter with `downloaded = true`
- **THEN** the series SHALL appear in the indexable series projection (searchability is driven by download state, not library membership)

### Requirement: Series document schema fields
Each series document inserted into the Samsung Search index SHALL contain the following fields: `_id` (string, key — format `{sourceId}:{seriesUrl}`), `title` (text), `author` (text), `description` (text, stored=false), `genres` (text — comma-separated), `status` (string), `type` (string), `source_url` (string, stored=true — deep link to Series screen). No chapter fields SHALL be present.

#### Scenario: Document contains all required fields
- **WHEN** a series with title "Tower of God", author "SIU", genres "action,fantasy", status "ONGOING", type "MANHWA", sourceId 1, url "https://example.com/tog" is indexed
- **THEN** the document SHALL have `_id = "1:https://example.com/tog"`, `title = "Tower of God"`, `author = "SIU"`, `genres = "action,fantasy"`, `status = "ONGOING"`, `type = "MANHWA"`, and a non-empty `source_url`

#### Scenario: Chapter fields are absent
- **WHEN** a series document is inserted
- **THEN** the document SHALL NOT contain `chapter_name`, `chapter_number`, or `doc_type` fields

### Requirement: Deep link to Series screen
The `source_url` field in each indexed series document SHALL resolve to the existing ReaderParser Series screen. The deep link SHALL navigate to the series detail view, NOT the reader.

#### Scenario: Tapping search result opens Series screen
- **WHEN** a user taps a ReaderParser search result in Samsung Smart Suggestions
- **THEN** ReaderParser SHALL open and navigate to the Series screen for that series

#### Scenario: Deep link does not open reader
- **WHEN** a user taps a ReaderParser search result
- **THEN** ReaderParser SHALL NOT open the Novel Reader or Manga Reader screen directly

### Requirement: Index rebuild on chapter download state change
ReaderParser SHALL observe the Room `chapters` table for changes and trigger a search index rebuild when any chapter's `downloaded` flag changes. The rebuild SHALL query the indexable-series projection, delete all existing documents from the search index, and bulk-insert the current result set. Rebuilds SHALL be debounced to avoid thrashing during batch downloads.

#### Scenario: New chapter downloaded triggers rebuild
- **WHEN** a chapter is downloaded and `downloaded` flips from `false` to `true`
- **THEN** the search index SHALL be rebuilt within a reasonable debounce window (e.g. 2 seconds)

#### Scenario: Chapter download deleted triggers rebuild
- **WHEN** a chapter's `downloaded` flag is reset to `false` via `deleteDownload()`
- **THEN** the search index SHALL be rebuilt; if the series has no remaining downloaded chapters, it SHALL be removed from the index

#### Scenario: Batch download does not thrash
- **WHEN** 10 chapters are downloaded in rapid succession
- **THEN** the index SHALL be rebuilt at most once (after debouncing), not 10 times

### Requirement: ACTION_UPDATE_INDEX broadcast handling
ReaderParser SHALL declare a BroadcastReceiver for `com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX` protected by permission `com.samsung.android.smartsuggestions.search.permission.SEND_ACTION_UPDATE_INDEX`. On receiving this broadcast, ReaderParser SHALL schedule a WorkManager `OneTimeWorkRequest` to perform a full re-index using the indexable-series projection.

#### Scenario: Index corruption recovery
- **WHEN** Samsung Search sends `ACTION_UPDATE_INDEX` because the index was corrupted
- **THEN** ReaderParser SHALL schedule a WorkManager one-time request that deletes all existing documents and re-inserts the current indexable series set

#### Scenario: WorkManager ensures execution
- **WHEN** the `ACTION_UPDATE_INDEX` broadcast is received while the app is in the background
- **THEN** WorkManager SHALL execute the re-index work even if the app process is killed

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

### Requirement: Bulk insert with batching
When inserting series documents, ReaderParser SHALL split the insert into batches of 100 or fewer documents and call `ContentResolver.bulkInsert()` for each batch.

#### Scenario: Large indexable set is batched
- **WHEN** the indexable series projection returns 250 series
- **THEN** ReaderParser SHALL call `bulkInsert()` three times (100 + 100 + 50)

#### Scenario: Small indexable set is single batch
- **WHEN** the indexable series projection returns 15 series
- **THEN** ReaderParser SHALL call `bulkInsert()` once with all 15 documents

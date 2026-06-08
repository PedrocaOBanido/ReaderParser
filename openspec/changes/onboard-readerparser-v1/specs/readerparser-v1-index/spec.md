## ADDED Requirements

### Requirement: Schema definition for ReaderParser index
The system SHALL define a Lucene schema at `search/src/main/assets/config/com.opus.readerparser/schema.xml` with fields: `_id` (string, key), `title` (text), `author` (text), `description` (text, stored=false), `genres` (text), `doc_type` (string), `source_url` (string, stored=true). The schema SHALL include fieldType definitions for text, spell (Korean chosung), pinyin, and keyword analyzers consistent with existing predefined indices.

#### Scenario: Schema loaded at startup
- **WHEN** the search engine initializes and `com.opus.readerparser` is in `PREDEFINED_INDICES`
- **THEN** `SchemaManager` SHALL parse the schema XML from assets and make it available for indexing and querying

#### Scenario: Schema includes required fields
- **WHEN** the schema is parsed
- **THEN** it SHALL contain at minimum: `_id`, `title`, `author`, `doc_type`, and `source_url` fields

### Requirement: Config registration for ReaderParser
The system SHALL include a `com.opus.readerparser` entry in `search/src/main/assets/config/config.json` with `enabled: true`, an `installed_package_name` of `com.opus.readerparser`, and a unique `settings_db_enabled_bit_pos`.

#### Scenario: App enabled when installed
- **WHEN** `ConfigManager.isAppEnabled("com.opus.readerparser")` is called and the ReaderParser app is installed
- **THEN** it SHALL return `true`

#### Scenario: App disabled when not installed
- **WHEN** `ConfigManager.isAppEnabled("com.opus.readerparser")` is called and the ReaderParser app is not installed
- **THEN** it SHALL return `false` and log the disabled reason

### Requirement: URI routing for ReaderParser queries
The system SHALL add a `READERPARSER_CODE` entry to `UriInfo` with path `readerparser` under `v1`, mapping to index name `com.opus.readerparser`. The `UriInfoMatcher` SHALL route `content://com.samsung.android.smartsuggestions.search/v1/readerparser` to this code.

#### Scenario: Query URI matched correctly
- **WHEN** a query arrives at `content://com.samsung.android.smartsuggestions.search/v1/readerparser`
- **THEN** `UriInfoMatcher.strictMatch()` SHALL return `READERPARSER_CODE` with index name `com.opus.readerparser`

### Requirement: Query handling for ReaderParser
The system SHALL create a `ReaderParserQueryInfo` class that builds Lucene queries with fuzzy, prefix, synonym, chosung, and pinyin matching on `title` and `author` fields. `QueryHandler.create()` SHALL route `READERPARSER_CODE` to `CommonQueryHandler`. `QueryInfoGenerator.getClientQuery()` SHALL create `ReaderParserQueryInfo` for `READERPARSER_CODE`.

#### Scenario: Fuzzy title search
- **WHEN** a query with `title = "solo levelling"` arrives at the ReaderParser index
- **THEN** the system SHALL return documents where `title` fuzzy-matches "solo levelling" including minor spelling variations

#### Scenario: Author prefix search
- **WHEN** a query with `author = "chu"` arrives at the ReaderParser index
- **THEN** the system SHALL return documents where `author` starts with "chu"

### Requirement: Data ingestion via ReaderParserCollector
The system SHALL implement a `ReaderParserCollector` that pulls Series and Chapter data from ReaderParser's ContentProvider, converts them to `Indexable` objects, and writes them to the Lucene index via `IndexManager`. The collector SHALL be registered in `CollectorFactory` and `AppObserver`.

#### Scenario: Initial full index
- **WHEN** the collector runs for the first time and ReaderParser has 50 series in its library
- **THEN** all 50 series documents SHALL be indexed in the `com.opus.readerparser` Lucene index

#### Scenario: Incremental update on new chapter
- **WHEN** ReaderParser's ContentProvider notifies a change URI for a new chapter
- **THEN** the collector SHALL index only the new chapter document without re-indexing existing series

### Requirement: Predefined index registration
The system SHALL add `com.opus.readerparser` to `SchemaConstants.PREDEFINED_INDICES` so the schema is loaded from assets at engine startup.

#### Scenario: Index available at startup
- **WHEN** the search engine starts
- **THEN** `SchemaConstants.PREDEFINED_INDICES` SHALL contain `com.opus.readerparser` and the schema SHALL be loaded

### Requirement: Feature flag gating
The system SHALL add a `readerparser_search_enabled` flag to `Rune` in `feature-config/` that gates collector startup, config enablement, and query routing for the ReaderParser index.

#### Scenario: Feature flag disabled
- **WHEN** `Rune.readerparser_search_enabled` is `false`
- **THEN** the collector SHALL not start and queries to `v1/readerparser` SHALL return an empty cursor

#### Scenario: Feature flag enabled
- **WHEN** `Rune.readerparser_search_enabled` is `true` and the app is installed
- **THEN** the collector SHALL start and queries SHALL return indexed results

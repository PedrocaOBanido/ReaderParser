## 1. Schema and Config Setup

- [ ] 1.1 Create `search/src/main/assets/config/com.opus.readerparser/schema.xml` with fields: `_id` (string, key), `title` (text), `author` (text), `description` (text, stored=false), `genres` (text), `doc_type` (string), `source_url` (string, stored=true). Include fieldType definitions for text, spell (chosung), pinyin, and keyword analyzers matching Settings schema pattern.
- [ ] 1.2 Add `com.opus.readerparser` entry to `search/src/main/assets/config/config.json` with `enabled: true`, `installed_package_name: "com.opus.readerparser"`, and a unique `settings_db_enabled_bit_pos` (next available bit).
- [ ] 1.3 Add `ReaderParser.PACKAGE_NAME = "com.opus.readerparser"` constant to `SchemaConstants.java` inner class `ReaderParser`.
- [ ] 1.4 Add `ReaderParser.PACKAGE_NAME` to `SchemaConstants.PREDEFINED_INDICES` list in the static initializer block.

## 2. URI Routing

- [ ] 2.1 Add `READERPARSER_PATH = "readerparser"` constant to `UriConstants.java`.
- [ ] 2.2 Add `READERPARSER_CODE(18, VERSION_1, READERPARSER_PATH, SchemaConstants.ReaderParser.PACKAGE_NAME, null)` to `UriInfo.java` enum (use next available code number).
- [ ] 2.3 Verify `UriInfoMatcher` automatically picks up the new enum entry (it iterates all `UriInfo.values()`).

## 3. Query Handling

- [ ] 3.1 Create `ReaderParserQueryInfo.java` in `search/src/main/java/.../client/sfinder/` extending `SuggestableQueryInfo`. Implement `getQuery()` with fuzzy + prefix + synonym + chosung + pinyin builders on `title` and `author` fields, modeled after `SettingsQueryInfo.java`.
- [ ] 3.2 Add `case READERPARSER_CODE:` to `QueryInfoGenerator.getClientQuery()` switch statement, creating `new ReaderParserQueryInfo(selection, queryArgs)`.
- [ ] 3.3 Add `READERPARSER_CODE` to the `CommonQueryHandler` case list in `QueryHandler.create()` switch expression.

## 4. Data Ingestion — Collector

- [ ] 4.1 Add `READERPARSER` enum value to `AppObserver.java` with appropriate ContentProvider URI, key field `_id`, and package identifier `com.opus.readerparser`.
- [ ] 4.2 Create `ReaderParserCollector.java` in `search/src/main/java/.../data/collector/readerparser/` extending `BaseCollector`. Implement `collect()` to query ReaderParser's ContentProvider for Series and Chapter data, convert to `IndexableList`, and call `IndexManager.addIndex()`.
- [ ] 4.3 Add `case READERPARSER:` to `CollectorFactory.createCollector()` switch statement, returning `new ReaderParserCollector(...)`.
- [ ] 4.4 Implement incremental sync in `ReaderParserCollector` using ContentObserver on ReaderParser's change URI to detect new/updated/deleted documents.

## 5. Feature Flag

- [ ] 5.1 Add `readerparser_search_enabled` flag to `Rune.kt` in `feature-config/` module, defaulting to `false`.
- [ ] 5.2 Gate `CollectorManager.getCollector()` for ReaderParser package behind the `Rune.readerparser_search_enabled` flag.
- [ ] 5.3 Gate `ConfigManager.isAppEnabled("com.opus.readerparser")` behind the same flag.

## 6. ReaderParser App — ContentProvider Exposure

- [ ] 6.1 In ReaderParser app (`~/projects/ReaderParser`), create a `SearchContentProvider.kt` that exposes Series and Chapter data from Room database via standard ContentProvider `query()` method with a defined URI matcher.
- [ ] 6.2 Declare the ContentProvider in ReaderParser's `AndroidManifest.xml` with read permission `com.samsung.android.smartsuggestions.search.permission.READ`.
- [ ] 6.3 Implement change notification URIs so the search engine's `ReaderParserCollector` can observe data changes.

## 7. Testing and Verification

- [ ] 7.1 Write unit tests for `ReaderParserQueryInfo` verifying fuzzy, prefix, and synonym query generation.
- [ ] 7.2 Write unit tests for `UriInfoMatcher` verifying `v1/readerparser` routes to `READERPARSER_CODE`.
- [ ] 7.3 Write integration test verifying end-to-end: insert test data via ReaderParser ContentProvider → trigger collector → query via `v1/readerparser` → verify results.
- [ ] 7.4 Run `./gradlew :search:testDebugUnitTest` to verify no regressions in existing search tests.
- [ ] 7.5 Run `./gradlew :search:assembleDebug` to verify compilation.

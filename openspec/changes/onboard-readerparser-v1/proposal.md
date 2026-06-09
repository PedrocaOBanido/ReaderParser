## Why

ReaderParser (`com.opus.readerparser`) is a webnovel/manhwa reader app whose library content (series titles, authors, genres, chapters) should be discoverable through Samsung Smart Suggestions' built-in search. Onboarding via the **v1 internal path** makes ReaderParser a first-class predefined index alongside Settings, Contacts, and Gallery — giving it the same query pipeline (fuzzy, prefix, synonym, chosung, pinyin) and S Finder integration that system apps receive.

## What Changes

- Add a new predefined search index `com.opus.readerparser` to the Samsung Search engine
- Ship a schema XML asset defining searchable fields for Series and Chapter entities (title, author, description, genres, chapter name, chapter number)
- Register the app in `config.json` with enable/disable controls and a `settings_db_enabled_bit_pos`
- Add a `UriInfo` enum entry and `v1/readerparser` URI path for query routing
- Add a `QueryInfo` subclass for ReaderParser queries (fuzzy + prefix + synonym on title/author fields)
- Add query routing in `QueryHandler.create()` and `QueryInfoGenerator.getClientQuery()`
- Implement a `ReaderParserCollector` (or manual ingestion via `ProviderHelper`) to index Series/Chapter data from ReaderParser's Room database into the Lucene index
- Add the index name to `SchemaConstants.PREDEFINED_INDICES` so the engine loads the schema from assets at startup

## Capabilities

### New Capabilities
- `readerparser-v1-index`: Predefined Lucene index for ReaderParser series and chapter data, including schema definition, config registration, URI routing, query handling, and data ingestion into the Samsung Search engine.

### Modified Capabilities

## Impact

- **search/ module** (primary): schema assets, `config.json`, `UriInfo`, `UriConstants`, `SchemaConstants`, `QueryHandler`, `QueryInfoGenerator`, `CollectorFactory`, `CollectorManager`, new `ReaderParserCollector` or `ProviderHelper` ingestion methods
- **ReaderParser app**: must expose a ContentProvider or AIDL interface for the search engine to pull Series/Chapter data, or push data via `SearchProvider.insert()`/`bulkInsert()` on `v1/readerparser`
- **Process boundary**: data flows from ReaderParser's `:app` process into the `:search` process via ContentProvider or direct insertion
- **Feature flag**: a `Rune` flag in `feature-config/` should gate the new index until validated
- **No Moneta impact**: this change is entirely within the DeepSky search stack

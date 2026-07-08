## MODIFIED Requirements

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

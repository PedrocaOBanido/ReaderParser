## 1. Fix availability probe

- [x] 1.1 Replace `delegate.getType(AUTHORITY_URI) != null` in `SamsungSearchClient.isAvailable()` with `delegate.call(AUTHORITY_URI, "request_search_api_version", null, null)`, then check the returned bundle for a non-null `response_search_api_version` value ≥ 1. Catch exceptions as before, returning `false`.
- [x] 1.2 Add a `METHOD_REQUEST_API_VERSION` constant to `SamsungSearchClient.Companion` for the probe method name.

## 2. Fix register_schema extras

- [x] 2.1 Add `extras.putString("name", SCHEMA_NAME)` in `SamsungSearchClient.registerSchema()` before the `delegate.call()` invocation, so the provider receives the schema name via extras.
- [x] 2.2 Change `register_schema` call to pass `null` for `arg` instead of `SCHEMA_NAME` — the provider reads the name from extras, not arg.

## 3. Fix schema XML asset

- [x] 3.1 Replace `<search-scheme>` root with `<schema>` root including `name`, `package`, `version`, `keyFieldName` attributes and `<fieldType>` declarations for `string_field` and `text_field`.

## 4. Update tests

- [x] 4.1 Update `SamsungSearchClientAvailabilityTest` in `SamsungSearchClientTest.kt`: adjust test cases to verify `isAvailable()` calls `delegate.call()` with `request_search_api_version` and returns `true` only when the bundle is non-null **and** `response_search_api_version` is present and ≥ 1. Return `false` when the bundle is null, when the key is missing, or when it throws.
- [x] 4.2 Add or update a test verifying `registerSchema()` captures the extras bundle passed to `delegate.call()` and asserts both `extras["name"]` equals `"com.opus.readerparser.series"` and `extras["schema-content"]` is non-null (the schema XML bytes).
- [x] 4.3 Update `registerSchema` tests to expect `lastCallArg == null` instead of the schema name string.
- [x] 4.4 Strengthen schema asset test to verify `<schema>` root tag and `keyFieldName` attribute.

## 5. Verification

- [x] 5.1 Run `./gradlew :app:assembleDebug` to verify compilation.
- [ ] 5.2 Run the Samsung Search instrumented tests for `SamsungSearchClientTest.kt` (e.g. via Android Studio, or with an instrumentation-class filter appropriate for this repo such as `-Pandroid.testInstrumentationRunnerArguments.class=...`).
- [ ] 5.3 On a device with Samsung Search installed: install the debug APK, launch the app, then run `adb logcat -d -v threadtime | rg -i "App|SamsungSearchClient|SearchProvider|PublicSchemaManager|register_schema|search integration disabled"` and verify that the line `Samsung Search not available — search integration disabled` no longer appears.
- [ ] 5.4 Verify the schema file exists: `adb shell ls /data/user/0/com.samsung.android.smartsuggestions/files/aisearch/client/com.opus.readerparser.series/schema.xml`.
- [ ] 5.5 Verify the index directory exists after sync/insert: `adb shell ls /data/user/0/com.samsung.android.smartsuggestions/files/aisearch/indexes/com.opus.readerparser.series/`.

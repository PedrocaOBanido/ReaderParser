# Adding a source

1. One directory per site, lowercase: `sources/<sitename>/`.
2. One file: `<SiteName>.kt` extending `HtmlSource`.
3. Override `chapterTextParse` for novels OR `chapterPagesParse` for manhwa, never both.
4. Register in `core/di/SourceModule.kt`.
5. Add HTML fixtures under `app/src/test/resources/fixtures/<sitename>/`.
6. Add a test using `MockEngine` and the fixtures.

Use `selectFirst(...)` + null-safety, not `select(...).first()`. Always
`absUrl(...)` for hrefs and image sources. Always `.trim()` text nodes.
- For Android API reference while implementing a source, load
  `skill("android-cli")` and use `android docs search <query>`.

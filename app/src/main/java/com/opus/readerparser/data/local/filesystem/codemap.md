# app/src/main/java/com/opus/readerparser/data/local/filesystem/

## Responsibility

File-system cache for downloaded chapter content (novel HTML and manhwa page
images). Provides a `DownloadStore` interface that can be faked in JVM tests,
and a `DownloadStoreImpl` backed by app-private storage.

## Files

| File | Kind | Role |
|---|---|---|
| `DownloadStore.kt` | Interface | Abstraction over the download cache. Methods: `read()`, `writeNovel()`, `writeManhwa()`, `delete()`. |
| `DownloadStoreImpl.kt` | Implementation | Real file-system implementation using `java.io.File` under `filesDir/downloads/`. |

## Design

**Interface + Impl for testability.** `DownloadStore` uses only domain types
(`Chapter`, `ChapterContent`) so tests can fake it without Android context.
The implementation injects a `File` root directory (`@DownloadRoot`) — in
production `context.filesDir`/downloads, in tests a `TemporaryFolder`.

**Directory layout (relative to root):**
```
{sourceId}/
  {sha1(seriesUrl)[:16]}/
    {sha1(chapterUrl)[:16]}/
      meta.json          # { type, originalUrls[], pageCount, downloadedAt }
      content.html       # NOVEL only
      001.jpg            # MANHWA only (1-indexed, zero-padded 3 digits)
      002.jpg
      ...
```

**Atomicity via meta.json.** For manhwa chapters, `meta.json` is written
**last**. If the process dies mid-download, no meta.json means `read()` returns
`null`, signaling the caller to retry the full download. Page files that were
partially written may remain but will be overwritten on retry.

**Network is the caller's responsibility.** `writeManhwa()` accepts a
`fetchBytes: suspend (url: String) -> ByteArray` lambda. The caller (the
WorkManager worker) provides the network call, keeping `DownloadStoreImpl`
free of Ktor/HTTP dependencies and trivially fakeable.

**Dispatchers.IO for all file I/O.** Every method wraps its work in
`withContext(Dispatchers.IO)`. Room and DataStore have their own dispatcher
management; filesystem operations do not.

## Flow

```
ChapterDownloadWorker (or repository)
  → downloadStore.writeNovel(chapter, html)
     → mkdirs(), write meta.json, write content.html

App when offline / reading from cache:
  repository → downloadStore.read(chapter)
     → exists? parse meta.json → return ChapterContent.Text(html) | ChapterContent.Pages(file URIs)
     else → fall through to SourceRegistry.getChapterContent() (network)
```

## Integration

| Connects to | Direction | Mechanism |
|---|---|---|
| `domain/model/` | Uses | `Chapter`, `ChapterContent` (returned from `read()`) |
| `core/di/FilesystemModule.kt` | Wired by | `@Binds DownloadStoreImpl → DownloadStore`; `@DownloadRoot File` provided |
| `data/repository/` | (Future) Called by | `ChapterRepositoryImpl.getContent()` will check `DownloadStore` before network |
| `workers/` (future) | Called by | `ChapterDownloadWorker` will call `writeNovel()`/`writeManhwa()` |

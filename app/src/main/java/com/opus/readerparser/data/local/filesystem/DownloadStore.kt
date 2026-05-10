package com.opus.readerparser.data.local.filesystem

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent

/**
 * Abstraction over the app-private file-system download cache.
 *
 * The interface uses only domain types so it can be faked in JVM unit tests
 * without an Android context.  The real implementation lives in Phase 7 and
 * will use [android.content.Context.getFilesDir] via Hilt.
 *
 * Layout (relative to `filesDir/downloads/`):
 * ```
 * {sourceId}/
 *   {seriesUrlHash}/
 *     {chapterUrlHash}/
 *       meta.json          # { type, originalUrls[], pageCount, downloadedAt }
 *       content.html       # NOVEL only
 *       001.jpg            # MANHWA only — zero-padded, 3-digit
 *       002.jpg
 *       ...
 * ```
 * Hashes are SHA-1 truncated to 16 hex chars, of the absolute URL.
 */
interface DownloadStore {

    /**
     * Returns the cached [ChapterContent] for [chapter] if it was previously
     * downloaded, or `null` if no local copy exists.
     */
    suspend fun read(chapter: Chapter): ChapterContent?

    /**
     * Persists a novel chapter's HTML to disk.
     *
     * The caller is responsible for ensuring [html] has already been sanitised.
     */
    suspend fun writeNovel(chapter: Chapter, html: String)

    /**
     * Persists a manhwa chapter's pages to disk.
     *
     * @param imageUrls The ordered list of page URLs from the source.
     *
     * The concrete implementation (Phase 7) is responsible for fetching the
     * image bytes from the network. The interface accepts only domain types so
     * it remains Android-free and trivially fakeable in JVM tests.
     */
    suspend fun writeManhwa(chapter: Chapter, imageUrls: List<String>)

    /** Removes the downloaded files for [chapter], if they exist. */
    suspend fun delete(chapter: Chapter)
}

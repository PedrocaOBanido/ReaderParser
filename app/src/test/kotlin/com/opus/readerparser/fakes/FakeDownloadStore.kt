package com.opus.readerparser.fakes

import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent

/**
 * Hand-rolled in-memory fake for [DownloadStore].
 *
 * Tracks every write and delete call so tests can assert on the exact chapters
 * that were persisted or removed.  No disk I/O is performed.
 *
 * Usage:
 * ```
 * val store = FakeDownloadStore()
 * store.storedContent[chapter] = ChapterContent.Text("<p>Hello</p>")
 * ```
 */
class FakeDownloadStore : DownloadStore {

    /**
     * Pre-populate this map to make [read] return a specific [ChapterContent].
     * Tests can also inspect it after a write to verify what was stored.
     */
    val storedContent: MutableMap<Chapter, ChapterContent> = mutableMapOf()

    /** Records every [writeNovel] call as a (chapter, html) pair. */
    val novelWrites: MutableList<Pair<Chapter, String>> = mutableListOf()

    /** Records every [writeManhwa] call as a (chapter, imageUrls) pair. */
    val manhwaWrites: MutableList<Pair<Chapter, List<String>>> = mutableListOf()

    /** Records every [delete] call. */
    val deleteCalls: MutableList<Chapter> = mutableListOf()

    override suspend fun read(chapter: Chapter): ChapterContent? = storedContent[chapter]

    override suspend fun writeNovel(chapter: Chapter, html: String) {
        novelWrites.add(chapter to html)
        storedContent[chapter] = ChapterContent.Text(html)
    }

    override suspend fun writeManhwa(
        chapter: Chapter,
        imageUrls: List<String>,
        fetchBytes: suspend (url: String) -> ByteArray,
    ) {
        imageUrls.forEach { url -> fetchBytes(url) }   // invoke so callers can assert on it
        manhwaWrites.add(chapter to imageUrls)
        storedContent[chapter] = ChapterContent.Pages(imageUrls)
    }

    override suspend fun delete(chapter: Chapter) {
        deleteCalls.add(chapter)
        storedContent.remove(chapter)
    }
}

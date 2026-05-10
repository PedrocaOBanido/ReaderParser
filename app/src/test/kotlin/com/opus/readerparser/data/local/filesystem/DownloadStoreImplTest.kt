package com.opus.readerparser.data.local.filesystem

import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadStoreImplTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private val chapter = TestFixtures.testChapter(
        seriesUrl = "https://test.invalid/series/test",
        url = "https://test.invalid/chapter/1",
        sourceId = 1L,
    )

    // Fixed fake image bytes (JPEG magic bytes) returned by the fetchBytes stub
    private val fakeImageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte())

    private val json = Json { ignoreUnknownKeys = true }

    private fun makeStore(): DownloadStoreImpl = DownloadStoreImpl(root = tmpDir.root, json = json)

    /** Stub fetchBytes lambda that returns [fakeImageBytes] for any URL. */
    private val fetchBytes: suspend (String) -> ByteArray = { fakeImageBytes }

    @Test
    fun `read on empty store returns null`() = runTest {
        val store = makeStore()
        assertNull(store.read(chapter))
    }

    @Test
    fun `writeNovel then read returns ChapterContent Text with original html`() = runTest {
        val store = makeStore()
        val html = "<p>Hello world</p>"

        store.writeNovel(chapter, html)

        val result = store.read(chapter)
        assertNotNull(result)
        assertTrue(result is ChapterContent.Text)
        assertEquals(html, (result as ChapterContent.Text).html)
    }

    @Test
    fun `writeManhwa then read returns ChapterContent Pages with matching count`() = runTest {
        val imageUrls = listOf(
            "https://cdn.invalid/page1.jpg",
            "https://cdn.invalid/page2.jpg",
            "https://cdn.invalid/page3.jpg",
        )
        val store = makeStore()

        store.writeManhwa(chapter, imageUrls, fetchBytes)

        val result = store.read(chapter)
        assertNotNull(result)
        assertTrue(result is ChapterContent.Pages)
        assertEquals(imageUrls.size, (result as ChapterContent.Pages).imageUrls.size)
    }

    @Test
    fun `delete after writeNovel causes read to return null`() = runTest {
        val store = makeStore()
        store.writeNovel(chapter, "<p>content</p>")

        store.delete(chapter)

        assertNull(store.read(chapter))
    }

    @Test
    fun `delete after writeManhwa causes read to return null`() = runTest {
        val store = makeStore()
        store.writeManhwa(chapter, listOf("https://cdn.invalid/page1.jpg"), fetchBytes)

        store.delete(chapter)

        assertNull(store.read(chapter))
    }

    @Test
    fun `writeNovel creates expected file layout`() = runTest {
        val store = makeStore()
        store.writeNovel(chapter, "<p>content</p>")

        val dir = tmpDir.root
            .resolve("${chapter.sourceId}")
            .resolve(hashUrl(chapter.seriesUrl))
            .resolve(hashUrl(chapter.url))

        assertTrue("Expected chapter directory to exist", dir.exists())
        assertTrue("Expected meta.json", dir.resolve("meta.json").exists())
        assertTrue("Expected content.html", dir.resolve("content.html").exists())
    }

    @Test
    fun `directory structure uses hashed urls as path components`() = runTest {
        val store = makeStore()
        store.writeNovel(chapter, "<p>content</p>")

        val seriesHash = hashUrl(chapter.seriesUrl)
        val chapterHash = hashUrl(chapter.url)

        val expectedDir = tmpDir.root
            .resolve("${chapter.sourceId}")
            .resolve(seriesHash)
            .resolve(chapterHash)

        assertTrue(
            "Expected dir $expectedDir to exist",
            expectedDir.isDirectory,
        )
    }

    @Test
    fun `writeManhwa writes pages with 1-based 3-digit zero-padded names`() = runTest {
        val imageUrls = listOf(
            "https://cdn.invalid/page1.jpg",
            "https://cdn.invalid/page2.jpg",
        )
        val store = makeStore()
        store.writeManhwa(chapter, imageUrls, fetchBytes)

        val dir = tmpDir.root
            .resolve("${chapter.sourceId}")
            .resolve(hashUrl(chapter.seriesUrl))
            .resolve(hashUrl(chapter.url))

        assertTrue("001.jpg should exist", dir.resolve("001.jpg").exists())
        assertTrue("002.jpg should exist", dir.resolve("002.jpg").exists())
    }

    @Test
    fun `delete on non-existent chapter does not throw`() = runTest {
        val store = makeStore()
        // Should complete without exception even when nothing was written
        store.delete(chapter)
    }
}

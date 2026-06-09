package com.opus.readerparser.workers

import com.opus.readerparser.fakes.FakeDownloadStore
import com.opus.readerparser.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for the download worker's Pages branch.
 *
 * Full end-to-end worker tests (with WorkManager lifecycle, constraints, and
 * retry logic) live in the instrumented test source set
 * (`src/androidTest/.../workers/ChapterDownloadWorkerTest.kt`) where
 * [androidx.work.testing.WorkManagerTestInitHelper] is available.
 *
 * These tests verify that [FakeDownloadStore.writeManhwa] correctly invokes the
 * [fetchBytes] lambda for every image URL — behaviour required so callers can
 * assert on the lambda (SF2) and so the worker's network call is exercised even
 * when using the fake.
 */
class ChapterDownloadWorkerTest {

    private val chapter = TestFixtures.testChapter(
        sourceId = 1L,
        seriesUrl = "https://test.invalid/series/1",
        url = "https://test.invalid/chapter/1",
    )

    /**
     * Verifies that [FakeDownloadStore.writeManhwa] calls [fetchBytes] once per
     * image URL, and that [FakeDownloadStore.manhwaWrites] records the chapter
     * and URL list correctly.
     *
     * This is the JVM-side check for the Pages branch that the worker exercises
     * via `downloads.writeManhwa(chapter, content.imageUrls) { url -> client.get(url).bodyAsBytes() }`.
     */
    @Test
    fun `writeManhwa invokes fetchBytes for every image url`() = runTest {
        val store = FakeDownloadStore()
        val imageUrls = listOf(
            "https://cdn.test.invalid/page1.jpg",
            "https://cdn.test.invalid/page2.jpg",
            "https://cdn.test.invalid/page3.jpg",
        )
        val fetchedUrls = mutableListOf<String>()

        store.writeManhwa(chapter, imageUrls, fetchBytes = { url ->
            fetchedUrls.add(url)
            ByteArray(0)
        })

        assertEquals(
            "fetchBytes should be called once per image URL",
            imageUrls,
            fetchedUrls,
        )
    }

    @Test
    fun `writeManhwa records chapter and imageUrls in manhwaWrites`() = runTest {
        val store = FakeDownloadStore()
        val imageUrls = listOf(
            "https://cdn.test.invalid/page1.jpg",
            "https://cdn.test.invalid/page2.jpg",
        )

        store.writeManhwa(chapter, imageUrls, fetchBytes = { ByteArray(0) })

        assertEquals("manhwaWrites should have exactly one entry", 1, store.manhwaWrites.size)
        val (recordedChapter, recordedUrls) = store.manhwaWrites.first()
        assertEquals(chapter, recordedChapter)
        assertEquals(imageUrls, recordedUrls)
    }

    @Test
    fun `writeManhwa with empty imageUrls does not invoke fetchBytes`() = runTest {
        val store = FakeDownloadStore()
        var fetchBytesCalled = false

        store.writeManhwa(chapter, emptyList(), fetchBytes = { url ->
            fetchBytesCalled = true
            ByteArray(0)
        })

        assertTrue("fetchBytes should not be called for empty URL list", !fetchBytesCalled)
        assertTrue("manhwaWrites should contain the chapter entry", store.manhwaWrites.isNotEmpty())
    }

    @Test
    fun `writeManhwa invokes onPageDownloaded after each page`() = runTest {
        val store = FakeDownloadStore()
        val imageUrls = listOf(
            "https://cdn.test.invalid/page1.jpg",
            "https://cdn.test.invalid/page2.jpg",
            "https://cdn.test.invalid/page3.jpg",
        )
        val progressUpdates = mutableListOf<Pair<Int, Int>>()

        store.writeManhwa(chapter, imageUrls, { ByteArray(0) }) { downloaded, total ->
            progressUpdates.add(downloaded to total)
        }

        assertEquals(
            "onPageDownloaded should be called once per page",
            listOf(1 to 3, 2 to 3, 3 to 3),
            progressUpdates,
        )
    }

    @Test
    fun `writeManhwa does not invoke onPageDownloaded for empty imageUrls`() = runTest {
        val store = FakeDownloadStore()
        var callbackInvoked = false

        store.writeManhwa(chapter, emptyList(), { ByteArray(0) }) { _, _ ->
            callbackInvoked = true
        }

        assertTrue("onPageDownloaded should not be called for empty URL list", !callbackInvoked)
    }

    @Test
    fun `writeManhwa passes page count as totalPages in callback`() = runTest {
        val store = FakeDownloadStore()
        val imageUrls = listOf(
            "https://cdn.test.invalid/page1.jpg",
            "https://cdn.test.invalid/page2.jpg",
        )
        val totalPagesSeen = mutableListOf<Int>()

        store.writeManhwa(chapter, imageUrls, { ByteArray(0) }) { _, total ->
            totalPagesSeen.add(total)
        }

        assertEquals("totalPages should be 2 for all callbacks", listOf(2, 2), totalPagesSeen)
    }

    @Test
    fun `writeNovel records chapter and html in novelWrites`() = runTest {
        val store = FakeDownloadStore()
        val html = "<p>Chapter content</p>"

        store.writeNovel(chapter, html)

        assertEquals("novelWrites should have exactly one entry", 1, store.novelWrites.size)
        val (recordedChapter, recordedHtml) = store.novelWrites.first()
        assertEquals(chapter, recordedChapter)
        assertEquals(html, recordedHtml)
    }
}

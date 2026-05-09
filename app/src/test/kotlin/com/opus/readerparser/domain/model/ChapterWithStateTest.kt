package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterWithStateTest {

    private val sampleChapter = Chapter(
        seriesUrl = "https://example.com/series/foo",
        sourceId = 1234L,
        url = "https://example.com/series/foo/ch1",
        name = "Chapter 1",
        number = 1.0f,
    )

    @Test
    fun `constructs with all required fields`() {
        val cws = ChapterWithState(
            chapter = sampleChapter,
            read = true,
            downloaded = false,
            progress = 0.5f,
        )

        assertEquals(sampleChapter, cws.chapter)
        assertTrue(cws.read)
        assertFalse(cws.downloaded)
        assertEquals(0.5f, cws.progress)
    }

    @Test
    fun `constructs with default-like values`() {
        val cws = ChapterWithState(
            chapter = sampleChapter,
            read = false,
            downloaded = false,
            progress = 0f,
        )

        assertFalse(cws.read)
        assertFalse(cws.downloaded)
        assertEquals(0f, cws.progress)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = ChapterWithState(
            chapter = sampleChapter,
            read = true,
            downloaded = true,
            progress = 1.0f,
        )

        val updated = original.copy(read = false)

        assertEquals(original.chapter, updated.chapter)
        assertFalse(updated.read)
        assertEquals(original.downloaded, updated.downloaded)
        assertEquals(original.progress, updated.progress)
    }

    @Test
    fun `equals and hashCode use data class semantics`() {
        val a = ChapterWithState(sampleChapter, read = true, downloaded = false, progress = 0.3f)
        val b = ChapterWithState(sampleChapter, read = true, downloaded = false, progress = 0.3f)
        val c = ChapterWithState(sampleChapter, read = false, downloaded = false, progress = 0.3f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())

        assertFalse(a == c)
    }
}

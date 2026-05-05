package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterTest {

    @Test
    fun `chapter is a data class with all required fields`() {
        val chapter = Chapter(
            seriesUrl = "https://example.com/series/foo",
            sourceId = 1234L,
            url = "https://example.com/series/foo/ch1",
            name = "Chapter 1",
            number = 1.0f,
        )

        assertEquals("https://example.com/series/foo", chapter.seriesUrl)
        assertEquals(1234L, chapter.sourceId)
        assertEquals("https://example.com/series/foo/ch1", chapter.url)
        assertEquals("Chapter 1", chapter.name)
        assertEquals(1.0f, chapter.number)
    }

    @Test
    fun `uploadDate defaults to null`() {
        val chapter = Chapter(
            seriesUrl = "url",
            sourceId = 1L,
            url = "url",
            name = "name",
            number = 0f,
        )

        assertNull(chapter.uploadDate)
    }

    @Test
    fun `copy works for all fields`() {
        val original = Chapter(
            seriesUrl = "url1",
            sourceId = 1L,
            url = "url1",
            name = "name1",
            number = 1.0f,
            uploadDate = 1000L,
        )

        val copy = original.copy(
            seriesUrl = "url2",
            sourceId = 2L,
            url = "url2",
            name = "name2",
            number = 2.0f,
            uploadDate = 2000L,
        )

        assertEquals("url2", copy.seriesUrl)
        assertEquals(2L, copy.sourceId)
        assertEquals("url2", copy.url)
        assertEquals("name2", copy.name)
        assertEquals(2.0f, copy.number)
        assertEquals(2000L, copy.uploadDate)
    }

    @Test
    fun `number can be minus one for unparseable chapters`() {
        val chapter = Chapter(
            seriesUrl = "url",
            sourceId = 1L,
            url = "url",
            name = "Prologue",
            number = -1f,
        )

        assertEquals(-1f, chapter.number)
    }
}

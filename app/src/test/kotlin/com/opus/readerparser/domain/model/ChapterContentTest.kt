package com.opus.readerparser.domain.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ChapterContentTest {

    @Test
    fun `text variant constructs with html string`() {
        val text = ChapterContent.Text("some html")
        assertEquals("some html", text.html)
    }

    @Test
    fun `pages variant constructs with image urls list`() {
        val pages = ChapterContent.Pages(listOf("a", "b"))
        assertEquals(listOf("a", "b"), pages.imageUrls)
    }

    @Test
    fun `chapterContent is a sealed interface`() {
        val text: ChapterContent = ChapterContent.Text("html")
        val pages: ChapterContent = ChapterContent.Pages(listOf("url1"))

        assertTrue(text is ChapterContent.Text)
        assertTrue(pages is ChapterContent.Pages)
    }
}

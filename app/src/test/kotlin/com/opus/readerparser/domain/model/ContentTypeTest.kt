package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ContentTypeTest {

    @Test
    fun `has exactly two values`() {
        assertEquals(2, ContentType.entries.size)
    }

    @Test
    fun `contains NOVEL`() {
        val novel = ContentType.valueOf("NOVEL")
        assertSame(ContentType.NOVEL, novel)
    }

    @Test
    fun `contains MANHWA`() {
        val manhwa = ContentType.valueOf("MANHWA")
        assertSame(ContentType.MANHWA, manhwa)
    }

    @Test
    fun `NOVEL name is NOVEL`() {
        assertEquals("NOVEL", ContentType.NOVEL.name)
    }

    @Test
    fun `MANHWA name is MANHWA`() {
        assertEquals("MANHWA", ContentType.MANHWA.name)
    }
}

package com.opus.readerparser.core.util

import com.opus.readerparser.domain.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeSourceIdTest {

    @Test
    fun `returns a non-negative Long in 32-bit range`() {
        val result = computeSourceId("TestSource", "en", ContentType.NOVEL)
        // Must be in range 0 .. 0xFFFFFFFF due to bitwise AND with 0xFFFFFFFF
        assertTrue(result in 0L..0xFFFFFFFFL)
    }

    @Test
    fun `same inputs produce same output`() {
        val a = computeSourceId("ExampleManhwa", "en", ContentType.MANHWA)
        val b = computeSourceId("ExampleManhwa", "en", ContentType.MANHWA)
        assertEquals(a, b)
    }

    @Test
    fun `different name produces different output`() {
        val a = computeSourceId("SourceA", "en", ContentType.NOVEL)
        val b = computeSourceId("SourceB", "en", ContentType.NOVEL)
        assertNotEquals(a, b)
    }

    @Test
    fun `different lang produces different output`() {
        val a = computeSourceId("Source", "en", ContentType.NOVEL)
        val b = computeSourceId("Source", "pt", ContentType.NOVEL)
        assertNotEquals(a, b)
    }

    @Test
    fun `different type produces different output`() {
        val a = computeSourceId("Source", "en", ContentType.NOVEL)
        val b = computeSourceId("Source", "en", ContentType.MANHWA)
        assertNotEquals(a, b)
    }

    @Test
    fun `matches documented formula`() {
        val name = "ExampleManhwa"
        val lang = "en"
        val type = ContentType.MANHWA

        val expected =
            ("$name/$lang/${type.name}".hashCode().toLong() and 0xFFFFFFFFL)
        val actual = computeSourceId(name, lang, type)

        assertEquals(expected, actual)
    }
}

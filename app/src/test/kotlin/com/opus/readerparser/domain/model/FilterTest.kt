package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterTest {

    @Test
    fun `Filter is a sealed interface`() {
        assertTrue(Filter::class.java.isSealed)
        assertTrue(Filter::class.java.isInterface)
    }

    @Test
    fun `Text can be constructed with key and value`() {
        val text = Filter.Text("genre", "action")
        assertEquals("genre", text.key)
        assertEquals("action", text.value)
    }

    @Test
    fun `Select can be constructed with key and value`() {
        val select = Filter.Select("status", "completed")
        assertEquals("status", select.key)
        assertEquals("completed", select.value)
    }

    @Test
    fun `Toggle can be constructed with key and boolean value`() {
        val toggle = Filter.Toggle("nsfw", true)
        assertEquals("nsfw", toggle.key)
        assertTrue(toggle.value)
    }

    @Test
    fun `Toggle can have false value`() {
        val toggle = Filter.Toggle("nsfw", false)
        assertFalse(toggle.value)
    }

    @Test
    fun `exactly three variants exist`() {
        val variants = Filter::class.java.permittedSubclasses!!
        assertEquals(3, variants.size)
    }

    @Test
    fun `all variants implement Filter`() {
        val text: Filter = Filter.Text("key", "val")
        val select: Filter = Filter.Select("key", "val")
        val toggle: Filter = Filter.Toggle("key", true)

        assertTrue(text is Filter.Text)
        assertTrue(select is Filter.Select)
        assertTrue(toggle is Filter.Toggle)
    }
}

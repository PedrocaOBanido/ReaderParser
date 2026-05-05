package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListTest {

    @Test
    fun `filters defaults to emptyList`() {
        val filterList = FilterList()
        assertTrue(filterList.filters.isEmpty())
    }

    @Test
    fun `can construct with list of filters`() {
        val filters = listOf(
            Filter.Text("genre", "action"),
            Filter.Select("status", "ongoing"),
            Filter.Toggle("nsfw", false),
        )
        val filterList = FilterList(filters)
        assertEquals(3, filterList.filters.size)
        assertEquals("action", (filterList.filters[0] as Filter.Text).value)
        assertEquals("ongoing", (filterList.filters[1] as Filter.Select).value)
        assertTrue(!(filterList.filters[2] as Filter.Toggle).value)
    }

    @Test
    fun `is a data class with structural equality`() {
        val a = FilterList(listOf(Filter.Text("genre", "action")))
        val b = FilterList(listOf(Filter.Text("genre", "action")))
        assertEquals(a, b)
    }
}

package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesPageTest {

    @Test
    fun `constructs with empty list and hasNextPage false`() {
        val page = SeriesPage(series = emptyList(), hasNextPage = false)

        assertTrue(page.series.isEmpty())
        assertFalse(page.hasNextPage)
    }

    @Test
    fun `constructs with list of series and hasNextPage true`() {
        val series = listOf(
            Series(sourceId = 1L, url = "https://example.com/a", title = "A", type = ContentType.NOVEL),
            Series(sourceId = 2L, url = "https://example.com/b", title = "B", type = ContentType.MANHWA),
        )
        val page = SeriesPage(series = series, hasNextPage = true)

        assertEquals(2, page.series.size)
        assertEquals("A", page.series[0].title)
        assertEquals("B", page.series[1].title)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `copy works preserving immutability`() {
        val original = SeriesPage(
            series = listOf(
                Series(sourceId = 1L, url = "https://example.com/x", title = "X", type = ContentType.NOVEL),
            ),
            hasNextPage = false,
        )

        val updated = original.copy(hasNextPage = true)

        assertEquals(original.series, updated.series)
        assertFalse(original.hasNextPage)
        assertTrue(updated.hasNextPage)
    }

    @Test
    fun `equals and hashCode use data class semantics`() {
        val series = listOf(
            Series(sourceId = 1L, url = "https://example.com/s", title = "S", type = ContentType.NOVEL),
        )
        val p1 = SeriesPage(series = series, hasNextPage = true)
        val p2 = SeriesPage(series = series, hasNextPage = true)

        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }
}

package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesStatusTest {

    @Test
    fun `SeriesStatus has exactly five values`() {
        assertEquals(5, SeriesStatus.entries.size)
    }

    @Test
    fun `valueOf returns correct enum constant`() {
        assertEquals(SeriesStatus.ONGOING, SeriesStatus.valueOf("ONGOING"))
        assertEquals(SeriesStatus.COMPLETED, SeriesStatus.valueOf("COMPLETED"))
        assertEquals(SeriesStatus.HIATUS, SeriesStatus.valueOf("HIATUS"))
        assertEquals(SeriesStatus.CANCELLED, SeriesStatus.valueOf("CANCELLED"))
        assertEquals(SeriesStatus.UNKNOWN, SeriesStatus.valueOf("UNKNOWN"))
    }

    @Test
    fun `all expected values are present`() {
        val expected = setOf(
            SeriesStatus.UNKNOWN,
            SeriesStatus.ONGOING,
            SeriesStatus.COMPLETED,
            SeriesStatus.HIATUS,
            SeriesStatus.CANCELLED,
        )
        assertEquals(expected, SeriesStatus.entries.toSet())
    }
}

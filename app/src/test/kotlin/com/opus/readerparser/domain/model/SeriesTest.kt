package com.opus.readerparser.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesTest {

    @Test
    fun `constructs with minimal fields and defaults`() {
        val series = Series(
            sourceId = 123L,
            url = "https://example.com/series/1",
            title = "Test Series",
            type = ContentType.NOVEL,
        )

        assertEquals(123L, series.sourceId)
        assertEquals("https://example.com/series/1", series.url)
        assertEquals("Test Series", series.title)
        assertEquals(ContentType.NOVEL, series.type)
        assertNull(series.author)
        assertNull(series.artist)
        assertNull(series.description)
        assertNull(series.coverUrl)
        assertTrue(series.genres.isEmpty())
        assertEquals(SeriesStatus.UNKNOWN, series.status)
    }

    @Test
    fun `constructs with all fields`() {
        val series = Series(
            sourceId = 456L,
            url = "https://example.com/series/2",
            title = "Full Series",
            author = "Author Name",
            artist = "Artist Name",
            description = "A description",
            coverUrl = "https://example.com/cover.jpg",
            genres = listOf("Action", "Romance"),
            status = SeriesStatus.ONGOING,
            type = ContentType.MANHWA,
        )

        assertEquals(456L, series.sourceId)
        assertEquals("https://example.com/series/2", series.url)
        assertEquals("Full Series", series.title)
        assertEquals("Author Name", series.author)
        assertEquals("Artist Name", series.artist)
        assertEquals("A description", series.description)
        assertEquals("https://example.com/cover.jpg", series.coverUrl)
        assertEquals(listOf("Action", "Romance"), series.genres)
        assertEquals(SeriesStatus.ONGOING, series.status)
        assertEquals(ContentType.MANHWA, series.type)
    }

    @Test
    fun `copy works for all fields`() {
        val original = Series(
            sourceId = 1L,
            url = "https://example.com/series/3",
            title = "Original",
            type = ContentType.NOVEL,
        )

        val updated = original.copy(
            title = "Updated",
            author = "New Author",
            status = SeriesStatus.COMPLETED,
        )

        assertEquals(original.sourceId, updated.sourceId)
        assertEquals(original.url, updated.url)
        assertEquals("Updated", updated.title)
        assertEquals("New Author", updated.author)
        assertEquals(SeriesStatus.COMPLETED, updated.status)
        assertEquals(original.type, updated.type)
        assertEquals(original.genres, updated.genres)
    }

    @Test
    fun `genres defaults to immutable empty list`() {
        val series = Series(
            sourceId = 1L,
            url = "https://example.com/series/4",
            title = "No Genres",
            type = ContentType.MANHWA,
        )

        assertTrue(series.genres.isEmpty())
    }

    @Test
    fun `equals and hashCode use data class semantics`() {
        val s1 = Series(1L, "url", "Title", type = ContentType.NOVEL)
        val s2 = Series(1L, "url", "Title", type = ContentType.NOVEL)

        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
    }
}

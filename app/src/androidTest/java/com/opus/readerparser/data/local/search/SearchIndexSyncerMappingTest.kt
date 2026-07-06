package com.opus.readerparser.data.local.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the [SeriesEntity.toContentValues] mapper.
 *
 * These verify that entity fields are correctly mapped to Samsung Search
 * schema fields. Requires the Android framework (ContentValues, Uri.encode)
 * and therefore lives in androidTest rather than local JVM tests.
 */
@RunWith(AndroidJUnit4::class)
class SearchIndexSyncerMappingTest {

    @Test
    fun toContentValues_maps_all_fields_correctly() {
        val entity = SeriesEntity(
            sourceId = 42L,
            url = "https://example.com/series/tog",
            title = "Tower of God",
            author = "SIU",
            artist = "SIU",
            description = "A man enters a mysterious tower",
            coverUrl = "https://example.com/cover.jpg",
            genresJson = """["action","fantasy"]""",
            status = "ONGOING",
            type = "MANHWA",
            inLibrary = true,
            addedAt = 1000L,
        )

        val cv = entity.toContentValues()

        assertEquals("42:https://example.com/series/tog", cv.getAsString("_id"))
        assertEquals("Tower of God", cv.getAsString("title"))
        assertEquals("SIU", cv.getAsString("author"))
        assertEquals("A man enters a mysterious tower", cv.getAsString("description"))
        assertEquals("action,fantasy", cv.getAsString("genres"))
        assertEquals("ONGOING", cv.getAsString("status"))
        assertEquals("MANHWA", cv.getAsString("type"))
        assertTrue(cv.getAsString("source_url")!!.startsWith("readerparser://series/42/"))
    }

    @Test
    fun toContentValues_encodes_URL_in_source_url() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://example.com/series/hello world",
            title = "Test",
            genresJson = "[]",
            status = "UNKNOWN",
            type = "NOVEL",
        )

        val cv = entity.toContentValues()

        // URL-encoded space should be %20
        assertTrue(cv.getAsString("source_url")!!.contains("hello%20world"))
    }

    @Test
    fun toContentValues_handles_null_author_and_description() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://test.invalid/series/1",
            title = "Test Series",
            author = null,
            description = null,
            genresJson = "[]",
            status = "UNKNOWN",
            type = "NOVEL",
        )

        val cv = entity.toContentValues()

        assertNull(cv.getAsString("author"))
        assertNull(cv.getAsString("description"))
        assertEquals("1:https://test.invalid/series/1", cv.getAsString("_id"))
        assertEquals("Test Series", cv.getAsString("title"))
        assertEquals("", cv.getAsString("genres"))
        assertEquals("UNKNOWN", cv.getAsString("status"))
        assertEquals("NOVEL", cv.getAsString("type"))
    }

    @Test
    fun toContentValues_does_not_include_chapter_fields() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://test.invalid/series/1",
            title = "Test",
            genresJson = "[]",
            status = "COMPLETED",
            type = "NOVEL",
        )

        val cv = entity.toContentValues()
        val keys = cv.keySet()

        assertTrue("Should not contain chapter_name", !keys.contains("chapter_name"))
        assertTrue("Should not contain chapter_number", !keys.contains("chapter_number"))
        assertTrue("Should not contain doc_type", !keys.contains("doc_type"))
    }

    @Test
    fun toContentValues_id_format_is_sourceId_colon_url() {
        val entity = SeriesEntity(
            sourceId = 999L,
            url = "https://example.com/my/series",
            title = "X",
            genresJson = "[]",
            status = "UNKNOWN",
            type = "MANHWA",
        )

        val cv = entity.toContentValues()

        assertEquals("999:https://example.com/my/series", cv.getAsString("_id"))
    }

    @Test
    fun toContentValues_decodes_genresJson_array_to_comma_separated_string() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://example.com/s",
            title = "T",
            genresJson = """["romance","drama","slice of life"]""",
            status = "ONGOING",
            type = "NOVEL",
        )

        val cv = entity.toContentValues()

        assertEquals("romance,drama,slice of life", cv.getAsString("genres"))
    }

    @Test
    fun toContentValues_handles_empty_genresJson_array() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://example.com/s",
            title = "T",
            genresJson = "[]",
            status = "ONGOING",
            type = "NOVEL",
        )

        val cv = entity.toContentValues()

        assertEquals("", cv.getAsString("genres"))
    }
}

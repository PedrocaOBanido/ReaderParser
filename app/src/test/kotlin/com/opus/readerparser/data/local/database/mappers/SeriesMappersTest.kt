package com.opus.readerparser.data.local.database.mappers

import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.SeriesStatus
import com.opus.readerparser.testutil.TestFixtures
import org.junit.Test

class SeriesMappersTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://test.invalid/series/test",
            title = "Test Series",
            author = "Author",
            artist = "Artist",
            description = "Description",
            coverUrl = "https://test.invalid/cover.jpg",
            genresJson = """["Action","Fantasy"]""",
            status = "ONGOING",
            type = "NOVEL",
            inLibrary = true,
            addedAt = 1000L,
        )

        val domain = entity.toDomain()

        assertThat(domain.sourceId).isEqualTo(1L)
        assertThat(domain.url).isEqualTo("https://test.invalid/series/test")
        assertThat(domain.title).isEqualTo("Test Series")
        assertThat(domain.author).isEqualTo("Author")
        assertThat(domain.artist).isEqualTo("Artist")
        assertThat(domain.description).isEqualTo("Description")
        assertThat(domain.coverUrl).isEqualTo("https://test.invalid/cover.jpg")
        assertThat(domain.genres).containsExactly("Action", "Fantasy").inOrder()
        assertThat(domain.status).isEqualTo(SeriesStatus.ONGOING)
        assertThat(domain.type).isEqualTo(ContentType.NOVEL)
    }

    @Test
    fun `toDomain handles null fields`() {
        val entity = SeriesEntity(
            sourceId = 2L,
            url = "https://test.invalid/series/nulls",
            title = "Minimal Series",
            author = null,
            artist = null,
            description = null,
            coverUrl = null,
            genresJson = "[]",
            status = "UNKNOWN",
            type = "MANHWA",
        )

        val domain = entity.toDomain()

        assertThat(domain.author).isNull()
        assertThat(domain.artist).isNull()
        assertThat(domain.description).isNull()
        assertThat(domain.coverUrl).isNull()
        assertThat(domain.genres).isEmpty()
    }

    @Test
    fun `toDomain falls back to UNKNOWN for invalid status`() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://test.invalid/series/bad",
            title = "Bad Status",
            genresJson = "[]",
            status = "INVALID_STATUS",
            type = "NOVEL",
        )

        val domain = entity.toDomain()

        assertThat(domain.status).isEqualTo(SeriesStatus.UNKNOWN)
    }

    @Test
    fun `toDomain falls back to NOVEL for invalid type`() {
        val entity = SeriesEntity(
            sourceId = 1L,
            url = "https://test.invalid/series/badtype",
            title = "Bad Type",
            genresJson = "[]",
            status = "ONGOING",
            type = "INVALID_TYPE",
        )

        val domain = entity.toDomain()

        assertThat(domain.type).isEqualTo(ContentType.NOVEL)
    }

    @Test
    fun `toEntity maps all domain fields`() {
        val series = TestFixtures.testSeries(
            sourceId = 1L,
            url = "https://test.invalid/series/test",
            title = "Test Series",
            author = "Author",
            artist = "Artist",
            description = "Description",
            coverUrl = "https://test.invalid/cover.jpg",
            genres = listOf("Action", "Fantasy"),
            status = SeriesStatus.ONGOING,
            type = ContentType.NOVEL,
        )

        val entity = series.toEntity()

        assertThat(entity.sourceId).isEqualTo(1L)
        assertThat(entity.url).isEqualTo("https://test.invalid/series/test")
        assertThat(entity.title).isEqualTo("Test Series")
        assertThat(entity.author).isEqualTo("Author")
        assertThat(entity.artist).isEqualTo("Artist")
        assertThat(entity.description).isEqualTo("Description")
        assertThat(entity.coverUrl).isEqualTo("https://test.invalid/cover.jpg")
        assertThat(entity.genresJson).isEqualTo("""["Action","Fantasy"]""")
        assertThat(entity.status).isEqualTo("ONGOING")
        assertThat(entity.type).isEqualTo("NOVEL")
    }

    @Test
    fun `toEntity sets inLibrary false and addedAt null`() {
        val series = TestFixtures.testSeries()

        val entity = series.toEntity()

        assertThat(entity.inLibrary).isFalse()
        assertThat(entity.addedAt).isNull()
    }

    @Test
    fun `round-trip preserves all domain fields`() {
        val original = TestFixtures.testSeries(
            sourceId = 42L,
            url = "https://example.com/series/round-trip",
            title = "Round Trip Series",
            author = "Author Name",
            artist = "Artist Name",
            description = "A series for round-trip testing",
            coverUrl = "https://example.com/cover.png",
            genres = listOf("Action", "Drama", "Sci-Fi"),
            status = SeriesStatus.COMPLETED,
            type = ContentType.MANHWA,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `round-trip with null fields preserves nulls`() {
        val original = TestFixtures.testSeries(
            author = null,
            artist = null,
            description = null,
            coverUrl = null,
            genres = emptyList(),
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `genres JSON round-trip preserves content`() {
        val genres = listOf("Action", "Fantasy", "Romance", "Sci-Fi")
        val series = TestFixtures.testSeries(genres = genres)

        val roundTripped = series.toEntity().toDomain()

        assertThat(roundTripped.genres).isEqualTo(genres)
    }

    @Test
    fun `round-trip with empty genres`() {
        val series = TestFixtures.testSeries(genres = emptyList())

        val roundTripped = series.toEntity().toDomain()

        assertThat(roundTripped.genres).isEmpty()
    }
}
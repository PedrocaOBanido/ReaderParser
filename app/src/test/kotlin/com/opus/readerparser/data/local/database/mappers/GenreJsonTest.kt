package com.opus.readerparser.data.local.database.mappers

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GenreJsonTest {

    @Test
    fun `round-trip preserves genres`() {
        val genres = listOf("Action", "Fantasy", "Romance")
        val json = GenreJson.genresToJson(genres)
        val result = GenreJson.jsonToGenres(json)
        assertThat(result).isEqualTo(genres)
    }

    @Test
    fun `empty list serializes to empty array`() {
        val genres = emptyList<String>()
        val json = GenreJson.genresToJson(genres)
        assertThat(json).isEqualTo("[]")
        val result = GenreJson.jsonToGenres(json)
        assertThat(result).isEmpty()
    }

    @Test
    fun `single genre round-trips`() {
        val genres = listOf("Action")
        val json = GenreJson.genresToJson(genres)
        assertThat(json).isEqualTo("""["Action"]""")
        val result = GenreJson.jsonToGenres(json)
        assertThat(result).containsExactly("Action")
    }

    @Test
    fun `special characters are preserved`() {
        val genres = listOf("Sci-Fi", "Isekai (Magic)", "Boys' Love")
        val json = GenreJson.genresToJson(genres)
        val result = GenreJson.jsonToGenres(json)
        assertThat(result).isEqualTo(genres)
    }

    @Test
    fun `empty string returns empty list`() {
        assertThat(GenreJson.jsonToGenres("")).isEmpty()
    }

    @Test
    fun `blank string returns empty list`() {
        assertThat(GenreJson.jsonToGenres("   ")).isEmpty()
    }

    @Test
    fun `empty array string deserializes correctly`() {
        assertThat(GenreJson.jsonToGenres("[]")).isEmpty()
    }
}

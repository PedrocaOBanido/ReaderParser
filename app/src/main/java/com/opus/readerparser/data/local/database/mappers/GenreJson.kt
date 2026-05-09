package com.opus.readerparser.data.local.database.mappers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object GenreJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun genresToJson(genres: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), genres)

    fun jsonToGenres(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return GenreJson.json.decodeFromString(ListSerializer(String.serializer()), json)
    }
}

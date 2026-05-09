package com.opus.readerparser.ui.navigation

import android.net.Uri

object Destinations {
    const val LIBRARY = "library"
    const val BROWSE = "browse"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    const val SERIES = "series/{sourceId}/{seriesUrl}"
    const val NOVEL_READER = "novel_reader/{sourceId}/{seriesUrl}/{chapterUrl}"
    const val MANGA_READER = "manga_reader/{sourceId}/{seriesUrl}/{chapterUrl}"

    fun series(sourceId: Long, seriesUrl: String): String =
        "series/$sourceId/${Uri.encode(seriesUrl)}"

    fun novelReader(sourceId: Long, seriesUrl: String, chapterUrl: String): String =
        "novel_reader/$sourceId/${Uri.encode(seriesUrl)}/${Uri.encode(chapterUrl)}"

    fun mangaReader(sourceId: Long, seriesUrl: String, chapterUrl: String): String =
        "manga_reader/$sourceId/${Uri.encode(seriesUrl)}/${Uri.encode(chapterUrl)}"
}

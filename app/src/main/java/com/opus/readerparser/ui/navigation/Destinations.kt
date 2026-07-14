package com.opus.readerparser.ui.navigation

import android.net.Uri

object Destinations {
    const val LIBRARY = "library"
    const val BROWSE = "browse"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"

    const val SERIES = "series/{sourceId}/{seriesUrl}"
    const val READER = "reader/{sourceId}/{seriesUrl}/{chapterUrl}/{contentType}"

    fun series(sourceId: Long, seriesUrl: String): String =
        "series/$sourceId/${Uri.encode(seriesUrl)}"

    fun reader(sourceId: Long, seriesUrl: String, chapterUrl: String, contentType: String): String =
        "reader/$sourceId/${Uri.encode(seriesUrl)}/${Uri.encode(chapterUrl)}/$contentType"
}

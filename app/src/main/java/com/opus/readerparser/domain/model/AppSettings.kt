package com.opus.readerparser.domain.model

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val novelFontSize: Int = 16,
    val novelFontFamily: String = "Default",
    val manhwaLayout: ManhwaLayout = ManhwaLayout.WEBTOON,
    val manhwaZoom: ManhwaZoom = ManhwaZoom.FIT_WIDTH,
)

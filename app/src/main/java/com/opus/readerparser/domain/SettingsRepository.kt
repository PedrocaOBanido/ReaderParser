package com.opus.readerparser.domain

import com.opus.readerparser.domain.model.AppSettings
import com.opus.readerparser.domain.model.AppTheme
import com.opus.readerparser.domain.model.ManhwaLayout
import com.opus.readerparser.domain.model.ManhwaZoom
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setNovelFontSize(size: Int)
    suspend fun setNovelFontFamily(family: String)
    suspend fun setManhwaLayout(layout: ManhwaLayout)
    suspend fun setManhwaZoom(zoom: ManhwaZoom)
}

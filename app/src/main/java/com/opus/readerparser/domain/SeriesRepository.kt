package com.opus.readerparser.domain

import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun observeLibrary(): Flow<List<Series>>

    suspend fun fetchPopular(sourceId: Long, page: Int): SeriesPage
    suspend fun fetchLatest(sourceId: Long, page: Int): SeriesPage
    suspend fun search(sourceId: Long, query: String, page: Int, filters: FilterList): SeriesPage

    suspend fun refreshDetails(series: Series): Series

    suspend fun addToLibrary(series: Series)
    suspend fun removeFromLibrary(series: Series)
}

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

    /**
     * Marks the given series as saved in the user's library.
     *
     * This method performs an UPDATE-only operation and is intentionally a no-op
     * when the series row does not yet exist in the database. In normal app flow
     * this never occurs because [refreshDetails] (which upserts the row) is always
     * called before the user can trigger this action. If the series is absent the
     * call silently succeeds with no effect.
     */
    suspend fun addToLibrary(series: Series)
    suspend fun removeFromLibrary(series: Series)
}

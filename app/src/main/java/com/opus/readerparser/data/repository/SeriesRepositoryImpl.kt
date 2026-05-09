package com.opus.readerparser.data.repository

import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.mappers.toDomain
import com.opus.readerparser.data.local.database.mappers.toEntity
import com.opus.readerparser.data.source.SourceRegistry
import com.opus.readerparser.domain.SeriesRepository
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val sourceRegistry: SourceRegistry,
    private val seriesDao: SeriesDao,
) : SeriesRepository {

    override fun observeLibrary(): Flow<List<Series>> =
        seriesDao.observeLibrary().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun fetchPopular(sourceId: Long, page: Int): SeriesPage {
        val result = sourceRegistry[sourceId].getPopular(page)
        seriesDao.upsertAll(result.series.map { it.toEntity() })
        return result
    }

    override suspend fun fetchLatest(sourceId: Long, page: Int): SeriesPage {
        val result = sourceRegistry[sourceId].getLatest(page)
        seriesDao.upsertAll(result.series.map { it.toEntity() })
        return result
    }

    override suspend fun search(
        sourceId: Long,
        query: String,
        page: Int,
        filters: FilterList,
    ): SeriesPage {
        val result = sourceRegistry[sourceId].search(query, page, filters)
        seriesDao.upsertAll(result.series.map { it.toEntity() })
        return result
    }

    override suspend fun refreshDetails(series: Series): Series {
        val updated = sourceRegistry[series.sourceId].getSeriesDetails(series)
        seriesDao.upsert(updated.toEntity())
        return updated
    }

    override suspend fun addToLibrary(series: Series) {
        seriesDao.upsert(series.toEntity().copy(inLibrary = true, addedAt = System.currentTimeMillis()))
    }

    override suspend fun removeFromLibrary(series: Series) {
        seriesDao.removeFromLibrary(series.sourceId, series.url)
    }
}

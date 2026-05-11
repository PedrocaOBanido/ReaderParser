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
        result.series.forEach { saveSeries(it) }
        return result
    }

    override suspend fun fetchLatest(sourceId: Long, page: Int): SeriesPage {
        val result = sourceRegistry[sourceId].getLatest(page)
        result.series.forEach { saveSeries(it) }
        return result
    }

    override suspend fun search(
        sourceId: Long,
        query: String,
        page: Int,
        filters: FilterList,
    ): SeriesPage {
        val result = sourceRegistry[sourceId].search(query, page, filters)
        result.series.forEach { saveSeries(it) }
        return result
    }

    override suspend fun refreshDetails(series: Series): Series {
        val updated = sourceRegistry[series.sourceId].getSeriesDetails(series)
        saveSeries(updated)
        return updated
    }

    override suspend fun addToLibrary(series: Series) {
        seriesDao.addToLibrary(series.sourceId, series.url, System.currentTimeMillis())
    }

    override suspend fun removeFromLibrary(series: Series) {
        seriesDao.removeFromLibrary(series.sourceId, series.url)
    }

    override suspend fun isInLibrary(sourceId: Long, url: String): Boolean =
        seriesDao.getByUrl(sourceId, url)?.inLibrary ?: false

    private suspend fun saveSeries(series: Series) {
        val entity = series.toEntity()
        val updated = seriesDao.updateDetails(
            sourceId = entity.sourceId,
            url = entity.url,
            title = entity.title,
            author = entity.author,
            artist = entity.artist,
            description = entity.description,
            coverUrl = entity.coverUrl,
            genresJson = entity.genresJson,
            status = entity.status,
            type = entity.type,
        )
        if (updated == 0) {
            seriesDao.insert(entity)
        }
    }
}

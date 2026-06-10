package com.opus.readerparser.data.local.search

import android.content.ContentValues
import android.util.Log
import com.opus.readerparser.data.local.database.dao.SeriesDao
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import com.opus.readerparser.data.local.database.mappers.GenreJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the indexable-series Room query and keeps the Samsung Search
 * index in sync. Rebuilds are debounced to avoid thrashing during batch
 * downloads.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Singleton
class SearchIndexSyncer @Inject constructor(
    private val seriesDao: SeriesDao,
    private val client: SamsungSearchClient,
) {

    private var observationJob: kotlinx.coroutines.Job? = null

    /**
     * Starts observing the indexable-series Flow and rebuilding the search
     * index whenever the source data changes. Safe to call multiple times —
     * only the most recent observation is active.
     */
    fun startObserving(scope: CoroutineScope) {
        observationJob?.cancel()
        observationJob = scope.launch {
            seriesDao.observeIndexableSeries()
                .distinctUntilChanged()
                .debounce(DEBOUNCE_MS)
                .catch { e ->
                    Log.w(TAG, "Indexable-series flow failed — observer paused", e)
                }
                .collect { series ->
                    try {
                        rebuildFromSeries(series)
                    } catch (e: Exception) {
                        Log.w(TAG, "Rebuild failed — will retry on next change", e)
                    }
                }
        }
        Log.i(TAG, "Index observation started")
    }

    /**
     * Performs a full re-index: queries the current indexable series,
     * clears the search index, and inserts the current set.
     *
     * Called explicitly by [com.opus.readerparser.workers.SamsungSearchRebuildWorker]
     * and by the observation flow.
     *
     * @param ensureRegistered when `true`, checks availability and registers
     *   the schema before rebuilding. Used by the worker path which may
     *   cold-start the app before [App.onCreate] finishes.
     * @return `true` if the index was (re)built successfully, `false` on
     *   failure or when Samsung Search is unavailable.
     */
    suspend fun rebuildIndex(ensureRegistered: Boolean = false): Boolean {
        if (ensureRegistered) {
            if (!client.isAvailable()) {
                Log.d(TAG, "Samsung Search unavailable — cannot rebuild")
                return false
            }
            if (!client.registerSchema()) {
                Log.w(TAG, "Schema registration failed — cannot rebuild")
                return false
            }
        }
        val series = seriesDao.getIndexableSeries()
        return rebuildFromSeries(series)
    }

    private suspend fun rebuildFromSeries(series: List<SeriesEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            if (!client.isAvailable()) {
                Log.d(TAG, "Samsung Search unavailable — skipping rebuild")
                return@withContext false
            }
            if (!client.deleteAll()) {
                Log.w(TAG, "deleteAll failed — aborting rebuild")
                return@withContext false
            }
            val documents = series.map { it.toContentValues() }
            val insertOk = client.bulkInsert(documents)
            Log.i(TAG, "Rebuilt index: ${documents.size} series (insert=$insertOk)")
            insertOk
        }
    }

    companion object {
        private const val TAG = "SearchIndexSyncer"
        private const val DEBOUNCE_MS = 2000L
    }
}

/**
 * Maps a [SeriesEntity] to [ContentValues] matching the registered
 * Samsung Search schema fields.
 *
 * - `_id`: composite `{sourceId}:{seriesUrl}`
 * - `title`: series title
 * - `author`: series author (may be null)
 * - `description`: series description (may be null, stored=false so not surfaced)
 * - `genres`: comma-separated genres from `genresJson`
 * - `status`: series status string
 * - `type`: content type string (NOVEL / MANHWA)
 * - `source_url`: deep link to the Series screen
 */
fun SeriesEntity.toContentValues(): ContentValues = ContentValues().apply {
    put("_id", "$sourceId:$url")
    put("title", title)
    put("author", author)
    put("description", description)
    put("genres", GenreJson.jsonToGenres(genresJson).joinToString(","))
    put("status", status)
    put("type", type)
    put("source_url", "readerparser://series/$sourceId/${android.net.Uri.encode(url)}")
}

package com.opus.readerparser.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE sourceId = :sourceId AND seriesUrl = :seriesUrl ORDER BY number ASC")
    fun observeChapters(sourceId: Long, seriesUrl: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE sourceId = :sourceId AND url = :url")
    suspend fun getByUrl(sourceId: Long, url: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET read = :read WHERE sourceId = :sourceId AND url = :url")
    suspend fun markRead(sourceId: Long, url: String, read: Boolean)

    @Query("UPDATE chapters SET progress = :progress WHERE sourceId = :sourceId AND url = :url")
    suspend fun setProgress(sourceId: Long, url: String, progress: Float)

    @Query("UPDATE chapters SET downloaded = :downloaded WHERE sourceId = :sourceId AND url = :url")
    suspend fun markDownloaded(sourceId: Long, url: String, downloaded: Boolean)

    /** One-shot counterpart to [observeChapters]; returns the current list without subscribing to updates. */
    @Query("SELECT * FROM chapters WHERE sourceId = :sourceId AND seriesUrl = :seriesUrl ORDER BY number ASC")
    suspend fun getChaptersForSeries(sourceId: Long, seriesUrl: String): List<ChapterEntity>

    @Query("DELETE FROM chapters WHERE sourceId = :sourceId AND seriesUrl = :seriesUrl")
    suspend fun deleteBySeries(sourceId: Long, seriesUrl: String)
}

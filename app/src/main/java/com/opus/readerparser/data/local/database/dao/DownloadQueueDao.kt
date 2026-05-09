package com.opus.readerparser.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import kotlinx.coroutines.flow.Flow

data class DownloadQueueWithDetails(
    val sourceId: Long,
    val chapterUrl: String,
    val state: String,
    val progress: Float,
    val errorMessage: String?,
    val chapterName: String,
    val seriesTitle: String,
)

@Dao
interface DownloadQueueDao {

    @Query("SELECT * FROM download_queue ORDER BY sourceId ASC")
    fun observeAll(): Flow<List<DownloadQueueEntity>>

    @Query("""
        SELECT dq.sourceId, dq.chapterUrl, dq.state, dq.progress, dq.errorMessage,
               COALESCE(c.name, '') AS chapterName,
               COALESCE(s.title, '') AS seriesTitle
        FROM download_queue dq
        LEFT JOIN chapters c ON dq.sourceId = c.sourceId AND dq.chapterUrl = c.url
        LEFT JOIN series s ON dq.sourceId = s.sourceId AND c.seriesUrl = s.url
        ORDER BY dq.rowid ASC
    """)
    fun observeAllWithDetails(): Flow<List<DownloadQueueWithDetails>>

    @Query("SELECT * FROM download_queue WHERE state = 'QUEUED' ORDER BY sourceId ASC")
    fun observeQueued(): Flow<List<DownloadQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DownloadQueueEntity)

    @Query("UPDATE download_queue SET state = :state, progress = :progress WHERE sourceId = :sourceId AND chapterUrl = :chapterUrl")
    suspend fun updateState(sourceId: Long, chapterUrl: String, state: String, progress: Float)

    @Query("DELETE FROM download_queue WHERE sourceId = :sourceId AND chapterUrl = :chapterUrl")
    suspend fun delete(sourceId: Long, chapterUrl: String)
}
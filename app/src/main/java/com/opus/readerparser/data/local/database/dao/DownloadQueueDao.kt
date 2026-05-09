package com.opus.readerparser.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opus.readerparser.data.local.database.entities.DownloadQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadQueueDao {

    @Query("SELECT * FROM download_queue ORDER BY sourceId ASC")
    fun observeAll(): Flow<List<DownloadQueueEntity>>

    @Query("SELECT * FROM download_queue WHERE state = 'QUEUED' ORDER BY sourceId ASC")
    fun observeQueued(): Flow<List<DownloadQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DownloadQueueEntity)

    @Query("UPDATE download_queue SET state = :state, progress = :progress WHERE sourceId = :sourceId AND chapterUrl = :chapterUrl")
    suspend fun updateState(sourceId: Long, chapterUrl: String, state: String, progress: Float)

    @Query("DELETE FROM download_queue WHERE sourceId = :sourceId AND chapterUrl = :chapterUrl")
    suspend fun delete(sourceId: Long, chapterUrl: String)
}
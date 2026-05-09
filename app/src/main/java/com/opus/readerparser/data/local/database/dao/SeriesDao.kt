package com.opus.readerparser.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.opus.readerparser.data.local.database.entities.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series WHERE inLibrary = 1 ORDER BY addedAt DESC")
    fun observeLibrary(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE sourceId = :sourceId AND url = :url")
    suspend fun getByUrl(sourceId: Long, url: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(series: SeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(series: List<SeriesEntity>)

    @Query("UPDATE series SET inLibrary = 1, addedAt = :addedAt WHERE sourceId = :sourceId AND url = :url")
    suspend fun addToLibrary(sourceId: Long, url: String, addedAt: Long)

    @Query("UPDATE series SET inLibrary = 0 WHERE sourceId = :sourceId AND url = :url")
    suspend fun removeFromLibrary(sourceId: Long, url: String)

    @Query("DELETE FROM series WHERE sourceId = :sourceId AND url = :url")
    suspend fun delete(sourceId: Long, url: String)
}
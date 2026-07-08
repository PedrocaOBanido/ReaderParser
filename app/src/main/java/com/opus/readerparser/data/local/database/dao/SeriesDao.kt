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

    @Query("SELECT * FROM series WHERE sourceId = :sourceId")
    suspend fun getBySourceId(sourceId: Long): List<SeriesEntity>

    @Query(
        """
        SELECT DISTINCT s.*
        FROM series s
        INNER JOIN chapters c
          ON s.sourceId = c.sourceId AND s.url = c.seriesUrl
        WHERE s.sourceId = :sourceId
          AND s.url = :url
          AND s.inLibrary = 1
          AND c.downloaded = 1
        LIMIT 1
        """,
    )
    suspend fun getLibraryIndexableSeries(sourceId: Long, url: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(series: SeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(series: List<SeriesEntity>)

    /**
     * Updates only the content columns of an existing series row.
     * Does **not** touch [inLibrary] or [addedAt] and therefore cannot
     * trigger a cascading delete of child chapters.
     *
     * @return the number of rows updated (0 if the series does not yet exist).
     */
    @Query(
        """UPDATE series SET
            title = :title,
            author = :author,
            artist = :artist,
            description = :description,
            coverUrl = :coverUrl,
            genresJson = :genresJson,
            status = :status,
            type = :type
          WHERE sourceId = :sourceId AND url = :url""",
    )
    suspend fun updateDetails(
        sourceId: Long,
        url: String,
        title: String,
        author: String?,
        artist: String?,
        description: String?,
        coverUrl: String?,
        genresJson: String,
        status: String,
        type: String,
    ): Int

    @Insert
    suspend fun insert(series: SeriesEntity)

    @Query("UPDATE series SET inLibrary = 1, addedAt = :addedAt WHERE sourceId = :sourceId AND url = :url")
    suspend fun addToLibrary(sourceId: Long, url: String, addedAt: Long)

    @Query("UPDATE series SET inLibrary = 0 WHERE sourceId = :sourceId AND url = :url")
    suspend fun removeFromLibrary(sourceId: Long, url: String)

    @Query("DELETE FROM series WHERE sourceId = :sourceId AND url = :url")
    suspend fun delete(sourceId: Long, url: String)

    /**
     * Returns distinct series that have at least one chapter with
     * [com.opus.readerparser.data.local.database.entities.ChapterEntity.downloaded] = `true`.
     *
     * Searchability is driven by download state, not library membership — a
     * series not in the library but with downloaded chapters **is** included.
     */
    @Query(
        """
        SELECT DISTINCT s.*
        FROM series s
        INNER JOIN chapters c
          ON s.sourceId = c.sourceId AND s.url = c.seriesUrl
        WHERE c.downloaded = 1
        ORDER BY s.title ASC
        """,
    )
    fun observeIndexableSeries(): Flow<List<SeriesEntity>>

    /** One-shot counterpart to [observeIndexableSeries]. */
    @Query(
        """
        SELECT DISTINCT s.*
        FROM series s
        INNER JOIN chapters c
          ON s.sourceId = c.sourceId AND s.url = c.seriesUrl
        WHERE c.downloaded = 1
        ORDER BY s.title ASC
        """,
    )
    suspend fun getIndexableSeries(): List<SeriesEntity>
}

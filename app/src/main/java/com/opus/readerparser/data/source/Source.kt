package com.opus.readerparser.data.source

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Filter
import com.opus.readerparser.domain.model.FilterList
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage

/**
 * Central contract that all site plugins implement.
 * Every retrieval method is [suspend]; sources throw on error and never return null sentinels.
 */
interface Source {
    /** Stable hash used as a foreign key in the database. */
    val id: Long

    /** User-visible name of the source. */
    val name: String

    /** ISO 639-1 language code. */
    val lang: String

    /** Root URL of the source site. */
    val baseUrl: String

    /** Whether this source serves novels or manhwa. */
    val type: ContentType

    /**
     * Returns `true` if this source supports the given filter kind at all.
     * Default implementation returns `true` — override to restrict.
     */
    fun supports(filter: Filter): Boolean = true

    /** Returns a page of popular series. */
    suspend fun getPopular(page: Int): SeriesPage

    /** Returns a page of the latest series. */
    suspend fun getLatest(page: Int): SeriesPage

    /** Searches for series matching [query] with optional [filters]. */
    suspend fun search(query: String, page: Int, filters: FilterList): SeriesPage

    /** Fills in details not present in a series listing (description, genres, etc.). */
    suspend fun getSeriesDetails(series: Series): Series

    /** Returns all chapters for the given [series], in reading order. */
    suspend fun getChapterList(series: Series): List<Chapter>

    /** Returns the content of a single [chapter]. */
    suspend fun getChapterContent(chapter: Chapter): ChapterContent
}

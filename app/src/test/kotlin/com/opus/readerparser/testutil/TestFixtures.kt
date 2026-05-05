package com.opus.readerparser.testutil

import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.ContentType
import com.opus.readerparser.domain.model.Series
import com.opus.readerparser.domain.model.SeriesPage
import com.opus.readerparser.domain.model.SeriesStatus

/**
 * Factory functions that produce domain model instances with sensible
 * defaults, so tests can create them in one line and override only what
 * they need via named arguments.
 */
object TestFixtures {

    /**
     * Returns a [Series] with default values linking to a test source.
     */
    fun testSeries(
        sourceId: Long = 1L,
        url: String = "https://test.invalid/series/test",
        title: String = "Test Series",
        author: String? = null,
        artist: String? = null,
        description: String? = null,
        coverUrl: String? = null,
        genres: List<String> = emptyList(),
        status: SeriesStatus = SeriesStatus.UNKNOWN,
        type: ContentType = ContentType.NOVEL,
    ): Series = Series(
        sourceId = sourceId,
        url = url,
        title = title,
        author = author,
        artist = artist,
        description = description,
        coverUrl = coverUrl,
        genres = genres,
        status = status,
        type = type,
    )

    /**
     * Returns a [Chapter] whose [Chapter.seriesUrl] defaults to
     * [testSeries].url so the two are linked by default.
     */
    fun testChapter(
        seriesUrl: String = testSeries().url,
        sourceId: Long = 1L,
        url: String = "https://test.invalid/chapter/1",
        name: String = "Chapter 1",
        number: Float = 1f,
        uploadDate: Long? = null,
    ): Chapter = Chapter(
        seriesUrl = seriesUrl,
        sourceId = sourceId,
        url = url,
        name = name,
        number = number,
        uploadDate = uploadDate,
    )

    /**
     * Returns a [SeriesPage] containing one entry from [testSeries]
     * and [hasNextPage] = false.
     */
    fun testSeriesPage(
        series: List<Series> = listOf(testSeries()),
        hasNextPage: Boolean = false,
    ): SeriesPage = SeriesPage(
        series = series,
        hasNextPage = hasNextPage,
    )

    /**
     * Convenience [ChapterContent.Text] for novel tests.
     */
    fun testTextContent(html: String = "<p>Test chapter content</p>"): ChapterContent =
        ChapterContent.Text(html)

    /**
     * Convenience [ChapterContent.Pages] for manhwa tests.
     */
    fun testPagesContent(
        imageUrls: List<String> = listOf("https://test.invalid/page/1.jpg"),
    ): ChapterContent = ChapterContent.Pages(imageUrls)
}

package com.opus.readerparser.data.local.database.mappers

import com.google.common.truth.Truth.assertThat
import com.opus.readerparser.data.local.database.entities.ChapterEntity
import com.opus.readerparser.testutil.TestFixtures
import org.junit.Test

class ChapterMappersTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val entity = ChapterEntity(
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            seriesUrl = "https://test.invalid/series/test",
            name = "Chapter 1",
            number = 1f,
            uploadDate = 1700000000000L,
            read = true,
            progress = 0.75f,
            downloaded = true,
        )

        val domain = entity.toDomain()

        assertThat(domain.seriesUrl).isEqualTo("https://test.invalid/series/test")
        assertThat(domain.sourceId).isEqualTo(1L)
        assertThat(domain.url).isEqualTo("https://test.invalid/chapter/1")
        assertThat(domain.name).isEqualTo("Chapter 1")
        assertThat(domain.number).isEqualTo(1f)
        assertThat(domain.uploadDate).isEqualTo(1700000000000L)
    }

    @Test
    fun `toDomain drops read progress and downloaded state`() {
        val entity = ChapterEntity(
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            seriesUrl = "https://test.invalid/series/test",
            name = "Chapter 1",
            number = 1f,
            read = true,
            progress = 0.5f,
            downloaded = true,
        )

        val domain = entity.toDomain()

        assertThat(domain.seriesUrl).isEqualTo("https://test.invalid/series/test")
        assertThat(domain.sourceId).isEqualTo(1L)
        assertThat(domain.url).isEqualTo("https://test.invalid/chapter/1")
    }

    @Test
    fun `toDomain handles null uploadDate`() {
        val entity = ChapterEntity(
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            seriesUrl = "https://test.invalid/series/test",
            name = "Chapter 1",
            number = 1f,
            uploadDate = null,
        )

        val domain = entity.toDomain()

        assertThat(domain.uploadDate).isNull()
    }

    @Test
    fun `toEntity maps domain fields and sets defaults`() {
        val chapter = TestFixtures.testChapter(
            seriesUrl = "https://test.invalid/series/test",
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            name = "Chapter 1",
            number = 1f,
            uploadDate = 1700000000000L,
        )

        val entity = chapter.toEntity()

        assertThat(entity.sourceId).isEqualTo(1L)
        assertThat(entity.url).isEqualTo("https://test.invalid/chapter/1")
        assertThat(entity.seriesUrl).isEqualTo("https://test.invalid/series/test")
        assertThat(entity.name).isEqualTo("Chapter 1")
        assertThat(entity.number).isEqualTo(1f)
        assertThat(entity.uploadDate).isEqualTo(1700000000000L)
        assertThat(entity.read).isFalse()
        assertThat(entity.progress).isEqualTo(0f)
        assertThat(entity.downloaded).isFalse()
    }

    @Test
    fun `toEntity with explicit seriesUrl overrides default`() {
        val chapter = TestFixtures.testChapter(
            seriesUrl = "https://test.invalid/series/original",
        )

        val entity = chapter.toEntity(seriesUrl = "https://test.invalid/series/override")

        assertThat(entity.seriesUrl).isEqualTo("https://test.invalid/series/override")
    }

    @Test
    fun `round-trip preserves all domain fields`() {
        val original = TestFixtures.testChapter(
            seriesUrl = "https://test.invalid/series/test",
            sourceId = 42L,
            url = "https://test.invalid/chapter/round-trip",
            name = "Chapter 42",
            number = 42.5f,
            uploadDate = 1700000000000L,
        )

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `round-trip with null uploadDate`() {
        val original = TestFixtures.testChapter(uploadDate = null)

        val roundTripped = original.toEntity().toDomain()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `toChapterWithState maps all state fields`() {
        val entity = ChapterEntity(
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            seriesUrl = "https://test.invalid/series/test",
            name = "Chapter 1",
            number = 1f,
            uploadDate = null,
            read = true,
            progress = 0.75f,
            downloaded = true,
        )

        val withState = entity.toChapterWithState()

        assertThat(withState.chapter).isEqualTo(entity.toDomain())
        assertThat(withState.read).isTrue()
        assertThat(withState.progress).isEqualTo(0.75f)
        assertThat(withState.downloaded).isTrue()
    }

    @Test
    fun `toChapterWithState defaults to false and zero`() {
        val entity = ChapterEntity(
            sourceId = 1L,
            url = "https://test.invalid/chapter/1",
            seriesUrl = "https://test.invalid/series/test",
            name = "Chapter 1",
            number = 1f,
        )

        val withState = entity.toChapterWithState()

        assertThat(withState.read).isFalse()
        assertThat(withState.progress).isEqualTo(0f)
        assertThat(withState.downloaded).isFalse()
    }
}
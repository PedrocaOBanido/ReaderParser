package com.opus.readerparser.data.local.filesystem

import com.opus.readerparser.core.di.DownloadRoot
import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.domain.model.Chapter
import com.opus.readerparser.domain.model.ChapterContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Real file-system implementation of [DownloadStore].
 *
 * The root directory is injected so JVM tests can point it at a temp folder
 * without needing an Android context. In production Hilt provides
 * `File(context.filesDir, "downloads")`.
 *
 * Directory layout relative to [root]:
 * ```
 * {sourceId}/
 *   {hashUrl(chapter.seriesUrl)}/
 *     {hashUrl(chapter.url)}/
 *       meta.json
 *       content.html   (NOVEL)
 *       001.jpg        (MANHWA, 1-based, zero-padded 3 digits)
 *       002.jpg
 *       ...
 * ```
 */
class DownloadStoreImpl @Inject constructor(
    @param:DownloadRoot private val root: File,
    private val json: Json,
) : DownloadStore {

    @Serializable
    private data class Meta(
        val type: String,
        @SerialName("originalUrls") val originalUrls: List<String> = emptyList(),
        val pageCount: Int = 0,
        val downloadedAt: Long,
    )

    // ---- path helpers -------------------------------------------------------

    private fun chapterDir(chapter: Chapter): File =
        root.resolve("${chapter.sourceId}")
            .resolve(hashUrl(chapter.seriesUrl))
            .resolve(hashUrl(chapter.url))

    private fun metaFile(dir: File): File = dir.resolve("meta.json")
    private fun contentFile(dir: File): File = dir.resolve("content.html")
    private fun pageFile(dir: File, index: Int): File =
        dir.resolve("%03d.jpg".format(index))

    // ---- DownloadStore ------------------------------------------------------

    override suspend fun read(chapter: Chapter): ChapterContent? =
        withContext(Dispatchers.IO) {
            val dir = chapterDir(chapter)
            val metaF = metaFile(dir)
            if (!metaF.exists()) return@withContext null

            val meta = json.decodeFromString<Meta>(metaF.readText())
            when (meta.type) {
                "NOVEL" -> {
                    val html = contentFile(dir).takeIf { it.exists() }?.readText()
                        ?: return@withContext null
                    ChapterContent.Text(html)
                }
                "MANHWA" -> {
                    // Collect page files in sorted order; return their absolute paths.
                    // listFiles() returns null when the path is not a directory or an
                    // I/O error occurs — treat that as a corrupt download so the caller
                    // falls back to a fresh network fetch.
                    val files = dir.listFiles { f -> f.name.endsWith(".jpg") }
                        ?: throw IOException("listFiles returned null for existing directory: $dir")
                    val pages = files.sortedBy { it.name }.map { it.toURI().toString() }
                    ChapterContent.Pages(pages)
                }
                else -> null
            }
        }

    override suspend fun writeNovel(chapter: Chapter, html: String) {
        val dir = chapterDir(chapter)
        val meta = Meta(
            type = "NOVEL",
            pageCount = 0,
            downloadedAt = System.currentTimeMillis(),
        )
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            metaFile(dir).writeText(json.encodeToString(meta))
            contentFile(dir).writeText(html)
        }
    }

    override suspend fun writeManhwa(
        chapter: Chapter,
        imageUrls: List<String>,
        fetchBytes: suspend (url: String) -> ByteArray,
    ) {
        val dir = chapterDir(chapter)
        withContext(Dispatchers.IO) { dir.mkdirs() }

        imageUrls.forEachIndexed { index, url ->
            val bytes = fetchBytes(url)          // network — caller's dispatcher
            withContext(Dispatchers.IO) {         // disk — IO dispatcher
                pageFile(dir, index + 1).writeBytes(bytes)
            }
        }

        val meta = Meta(
            type = "MANHWA",
            originalUrls = imageUrls,
            pageCount = imageUrls.size,
            downloadedAt = System.currentTimeMillis(),
        )
        // meta.json is written last: if the process dies mid-download, no meta means
        // read() returns null and the next attempt overwrites any partial page files.
        withContext(Dispatchers.IO) {
            metaFile(dir).writeText(json.encodeToString(meta))
        }
    }

    override suspend fun delete(chapter: Chapter): Unit =
        withContext(Dispatchers.IO) {
            chapterDir(chapter).deleteRecursively()
        }
}

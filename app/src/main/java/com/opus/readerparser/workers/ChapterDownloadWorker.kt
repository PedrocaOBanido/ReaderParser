package com.opus.readerparser.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.opus.readerparser.core.util.hashUrl
import com.opus.readerparser.data.local.filesystem.DownloadStore
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.DownloadRepository
import com.opus.readerparser.domain.model.ChapterContent
import com.opus.readerparser.domain.model.DownloadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import java.util.concurrent.TimeUnit

/**
 * Downloads a single chapter to app-private storage.
 *
 * Input data keys:
 * - [KEY_SOURCE_ID]: Long — the source this chapter belongs to.
 * - [KEY_CHAPTER_URL]: String — the chapter's canonical URL (its identity).
 *
 * Retries up to 2 times (3 total attempts) with 30-second exponential backoff
 * before giving up and returning [Result.failure].
 */
@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val chapterRepository: ChapterRepository,
    private val downloadRepository: DownloadRepository,
    private val downloads: DownloadStore,
    private val client: HttpClient,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val sourceId = inputData.getLong(KEY_SOURCE_ID, -1L)
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL)
            ?: return Result.failure()

        return try {
            val chapter = chapterRepository.findByUrl(sourceId, chapterUrl)
                ?: run {
                    downloadRepository.updateQueueState(
                        sourceId = sourceId,
                        chapterUrl = chapterUrl,
                        state = DownloadState.FAILED,
                        progress = 0f,
                        errorMessage = "Chapter not found in local database",
                    )
                    return Result.failure()
                }

            downloadRepository.updateQueueState(sourceId, chapterUrl, DownloadState.RUNNING, 0f)

            // getContent delegates to the source internally; the worker never
            // references SourceRegistry directly. HttpClient is used only to
            // provide the fetchBytes lambda for writeManhwa.
            val content = chapterRepository.getContent(chapter)

            when (content) {
                is ChapterContent.Text -> downloads.writeNovel(chapter, content.html)
                is ChapterContent.Pages -> downloads.writeManhwa(chapter, content.imageUrls) { url ->
                    client.get(url).bodyAsBytes()
                }
            }

            chapterRepository.markDownloaded(chapter, true)
            downloadRepository.updateQueueState(sourceId, chapterUrl, DownloadState.COMPLETED, 1f)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork failed for sourceId=$sourceId url=$chapterUrl", e)
            downloadRepository.updateQueueState(
                sourceId = sourceId,
                chapterUrl = chapterUrl,
                state = DownloadState.FAILED,
                progress = 0f,
                errorMessage = e.message,
            )
            // runAttemptCount is 0-based: 0, 1, 2 → retry on first two failures (attempts 1 and 2),
            // fail permanently after the third attempt (runAttemptCount == 2).
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "ChapterDownloadWorker"
        const val KEY_SOURCE_ID = "sourceId"
        const val KEY_CHAPTER_URL = "chapterUrl"

        private val CONSTRAINTS = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Builds a [OneTimeWorkRequest] tagged with a deterministic tag so duplicate
         * downloads for the same chapter can be detected or cancelled.
         */
        fun buildRequest(sourceId: Long, chapterUrl: String): OneTimeWorkRequest {
            val tag = "download-$sourceId-${hashUrl(chapterUrl)}"
            val inputData = Data.Builder()
                .putLong(KEY_SOURCE_ID, sourceId)
                .putString(KEY_CHAPTER_URL, chapterUrl)
                .build()
            return OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
                .setConstraints(CONSTRAINTS)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(tag)
                .build()
        }
    }
}

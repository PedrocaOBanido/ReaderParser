package com.opus.readerparser.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.opus.readerparser.domain.ChapterRepository
import com.opus.readerparser.domain.SeriesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the chapter list for every series in the user's library.
 *
 * Runs every 6 hours when a network connection is available. Retries up to 2 times
 * (3 total attempts) with exponential backoff before returning [Result.failure].
 */
@HiltWorker
class LibraryUpdateWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val seriesRepository: SeriesRepository,
    private val chapterRepository: ChapterRepository,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = try {
        val library = seriesRepository.observeLibrary().first()
        library.forEach { series ->
            chapterRepository.refreshChapters(series)
        }
        Result.success()
    } catch (e: Exception) {
        // runAttemptCount is 0-based; retry on first two failures, fail on the third.
        if (runAttemptCount < 2) Result.retry() else Result.failure()
    }

    companion object {
        private const val REPEAT_INTERVAL_HOURS = 6L

        private val CONSTRAINTS = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Builds a [PeriodicWorkRequest] that fires every 6 hours on any network. */
        fun buildPeriodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<LibraryUpdateWorker>(REPEAT_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(CONSTRAINTS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("library-update")
                .build()
    }
}

package com.opus.readerparser.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opus.readerparser.data.local.search.SearchIndexSyncer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time WorkManager worker that performs a full search-index rebuild.
 *
 * Triggered by [com.opus.readerparser.workers.SamsungSearchUpdateReceiver]
 * when Samsung Search sends the `ACTION_UPDATE_INDEX` broadcast.
 */
@HiltWorker
class SamsungSearchRebuildWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val syncer: SearchIndexSyncer,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val success = syncer.rebuildIndex(ensureRegistered = true)
            if (success) Result.success() else Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Search index rebuild failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "SearchRebuildWorker"
    }
}

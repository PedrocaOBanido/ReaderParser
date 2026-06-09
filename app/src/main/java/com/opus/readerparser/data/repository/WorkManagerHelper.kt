package com.opus.readerparser.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest

/**
 * Abstraction over [androidx.work.WorkManager] for testability.
 *
 * Production implementation delegates to `WorkManager.getInstance(context)`.
 * Test fakes can record calls without requiring an Android context.
 */
interface WorkManagerHelper {
    fun enqueueUniqueWork(
        workName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    )

    /**
     * Enqueues a chain of work requests that execute sequentially.
     *
     * The first request runs immediately (subject to constraints); each
     * subsequent request starts only after the previous one completes
     * successfully. If any request fails, the remaining requests are
     * cancelled.
     *
     * All requests share [workName] so they can be cancelled as a unit.
     */
    fun enqueueChain(
        workName: String,
        policy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    )

    fun cancelAllWorkByTag(tag: String)
}

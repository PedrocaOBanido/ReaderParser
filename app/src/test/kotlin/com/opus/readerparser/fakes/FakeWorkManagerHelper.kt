package com.opus.readerparser.fakes

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import com.opus.readerparser.data.repository.WorkManagerHelper

/**
 * Hand-rolled fake for [WorkManagerHelper].
 *
 * Records all calls so tests can verify WorkManager interactions
 * without requiring an Android context.
 */
class FakeWorkManagerHelper : WorkManagerHelper {

    data class EnqueueCall(
        val workName: String,
        val policy: ExistingWorkPolicy,
        val request: OneTimeWorkRequest,
    )

    data class EnqueueChainCall(
        val workName: String,
        val policy: ExistingWorkPolicy,
        val requests: List<OneTimeWorkRequest>,
    )

    val enqueueCalls: MutableList<EnqueueCall> = mutableListOf()
    val enqueueChainCalls: MutableList<EnqueueChainCall> = mutableListOf()
    val cancelCalls: MutableList<String> = mutableListOf()

    override fun enqueueUniqueWork(
        workName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ) {
        enqueueCalls.add(EnqueueCall(workName, policy, request))
    }

    override fun enqueueChain(
        workName: String,
        policy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ) {
        enqueueChainCalls.add(EnqueueChainCall(workName, policy, requests))
    }

    override fun cancelAllWorkByTag(tag: String) {
        cancelCalls.add(tag)
    }
}

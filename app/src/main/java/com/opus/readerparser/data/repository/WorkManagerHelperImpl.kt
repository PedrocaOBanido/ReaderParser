package com.opus.readerparser.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkManagerHelper {

    override fun enqueueUniqueWork(
        workName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest,
    ) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, policy, request)
    }

    override fun enqueueChain(
        workName: String,
        policy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ) {
        require(requests.isNotEmpty()) { "Cannot enqueue an empty work chain" }
        val wm = WorkManager.getInstance(context)
        var chain = wm.beginUniqueWork(workName, policy, requests.first())
        for (request in requests.drop(1)) {
            chain = chain.then(request)
        }
        chain.enqueue()
    }

    override fun cancelAllWorkByTag(tag: String) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(tag)
    }
}

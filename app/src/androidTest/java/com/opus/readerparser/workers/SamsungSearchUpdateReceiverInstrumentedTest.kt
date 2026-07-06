package com.opus.readerparser.workers

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [SamsungSearchUpdateReceiver].
 *
 * Verifies that receiving `ACTION_UPDATE_INDEX` actually schedules a
 * WorkManager one-time request tagged as the receiver.
 */
@RunWith(AndroidJUnit4::class)
class SamsungSearchUpdateReceiverInstrumentedTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            androidx.work.Configuration.Builder()
                .setWorkerFactory(NoOpWorkerFactory())
                .build(),
        )

        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun onReceive_withCorrectAction_enqueuesRebuildWork() {
        val intent = Intent(SamsungSearchUpdateReceiver.ACTION_UPDATE_INDEX)

        SamsungSearchUpdateReceiver().onReceive(context, intent)

        val workInfos = workManager.getWorkInfosByTag("SamsungSearchUpdateReceiver").get()
        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.first().state).isAnyOf(
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.SUCCEEDED,
        )
    }

    @Test
    fun onReceive_withWrongAction_doesNotEnqueueWork() {
        val intent = Intent("com.some.other.ACTION")

        SamsungSearchUpdateReceiver().onReceive(context, intent)

        val workInfos = workManager.getWorkInfosByTag("SamsungSearchUpdateReceiver").get()
        assertThat(workInfos).isEmpty()
    }

    @Test
    fun onReceive_withNullIntent_doesNotCrash() {
        // Should not throw — receiver guards against null action
        SamsungSearchUpdateReceiver().onReceive(context, null)

        val workInfos = workManager.getWorkInfosByTag("SamsungSearchUpdateReceiver").get()
        assertThat(workInfos).isEmpty()
    }

    @Test
    fun onReceive_schedulesExactlyOneWorkRequest() {
        val intent = Intent(SamsungSearchUpdateReceiver.ACTION_UPDATE_INDEX)

        SamsungSearchUpdateReceiver().onReceive(context, intent)

        val workInfos = workManager.getWorkInfosByTag("SamsungSearchUpdateReceiver").get()
        assertThat(workInfos).hasSize(1)
    }
}

/**
 * Minimal [WorkerFactory] that creates a no-op [ListenableWorker] for
 * [SamsungSearchRebuildWorker]. We only need to verify scheduling, not
 * actual execution.
 */
private class NoOpWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        if (workerClassName != SamsungSearchRebuildWorker::class.java.name) return null
        return NoOpCoroutineWorker(appContext, workerParameters)
    }
}

private class NoOpCoroutineWorker(
    context: Context,
    params: WorkerParameters,
) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}

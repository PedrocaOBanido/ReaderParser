package com.opus.readerparser.workers

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Receives `ACTION_UPDATE_INDEX` broadcasts from Samsung Search and
 * schedules a one-time WorkManager request to rebuild the search index.
 *
 * Declared in `AndroidManifest.xml` with the required
 * `SEND_ACTION_UPDATE_INDEX` permission.
 */
class SamsungSearchUpdateReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: android.content.Intent?) {
        if (intent?.action != ACTION_UPDATE_INDEX) {
            Log.w(TAG, "Unexpected action: ${intent?.action}")
            return
        }
        Log.i(TAG, "Received ACTION_UPDATE_INDEX — scheduling rebuild")
        val request = OneTimeWorkRequestBuilder<SamsungSearchRebuildWorker>()
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val TAG = "SamsungSearchUpdateReceiver"
        const val ACTION_UPDATE_INDEX =
            "com.samsung.android.smartsuggestions.search.ACTION_UPDATE_INDEX"
    }
}

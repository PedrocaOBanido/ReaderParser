package com.opus.readerparser

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.opus.readerparser.data.local.search.SamsungSearchClient
import com.opus.readerparser.data.local.search.SearchIndexSyncer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var searchClient: SamsungSearchClient

    @Inject
    lateinit var searchSyncer: SearchIndexSyncer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initSamsungSearch()
    }

    private fun initSamsungSearch() {
        appScope.launch(Dispatchers.IO) {
            try {
                if (!searchClient.isAvailable()) {
                    Log.i(TAG, "Samsung Search not available — search integration disabled")
                    return@launch
                }
                if (!searchClient.registerSchema()) {
                    Log.w(TAG, "Samsung Search schema registration failed — sync disabled")
                    return@launch
                }
                searchSyncer.startObserving(appScope)
            } catch (e: Exception) {
                Log.w(TAG, "Samsung Search init failed — continuing without search", e)
            }
        }
    }

    companion object {
        private const val TAG = "App"
    }
}

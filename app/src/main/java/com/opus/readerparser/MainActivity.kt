package com.opus.readerparser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavController
import com.opus.readerparser.ui.navigation.AppNavGraph
import com.opus.readerparser.ui.navigation.Destinations
import com.opus.readerparser.ui.theme.ReaderParserTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var navController: NavController? = null
    private var pendingDeepLink: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture the initial deep link BEFORE setContent so that the
        // onNavGraphReady callback always sees it.  Only on fresh create —
        // on recreation the nav graph re-navigates automatically via
        // savedStateHandle, so re-processing would duplicate the navigation.
        if (savedInstanceState == null) {
            intent?.data?.let { uri ->
                if (uri.scheme == "readerparser" && uri.host == "series") {
                    pendingDeepLink = uri
                }
            }
        }

        setContent {
            ReaderParserTheme {
                AppNavGraph(onNavGraphReady = { controller ->
                    navController = controller
                    // Process any pending deep link once the nav graph is ready
                    pendingDeepLink?.let { uri ->
                        navigateFromDeepLink(uri)
                        pendingDeepLink = null
                    }
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data ?: return
        if (uri.scheme != "readerparser" || uri.host != "series") return

        val controller = navController
        if (controller != null) {
            navigateFromDeepLink(uri)
        } else {
            pendingDeepLink = uri
        }
    }

    private fun navigateFromDeepLink(uri: Uri) {
        val pathSegments = uri.pathSegments
        if (pathSegments.size < 2) return

        val sourceId = pathSegments[0].toLongOrNull() ?: return
        val seriesUrl = pathSegments[1]

        navController?.navigate(Destinations.series(sourceId, seriesUrl))
    }
}

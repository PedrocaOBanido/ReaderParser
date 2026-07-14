package com.opus.readerparser.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opus.readerparser.ui.browse.BrowseScreen
import com.opus.readerparser.ui.downloads.DownloadsScreen
import com.opus.readerparser.ui.library.LibraryScreen
import com.opus.readerparser.ui.reader.ReaderScreen
import com.opus.readerparser.ui.series.SeriesScreen
import com.opus.readerparser.ui.settings.SettingsScreen

private val bottomNavRoutes = setOf(
    Destinations.LIBRARY,
    Destinations.BROWSE,
    Destinations.DOWNLOADS,
)

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun AppNavGraph(onNavGraphReady: (NavController) -> Unit = {}) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(navController) {
        onNavGraphReady(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Destinations.LIBRARY,
            label = "Library",
            icon = { Icon(Icons.Filled.CollectionsBookmark, contentDescription = "Library") },
        ),
        BottomNavItem(
            route = Destinations.BROWSE,
            label = "Browse",
            icon = { Icon(Icons.Filled.Explore, contentDescription = "Browse") },
        ),
        BottomNavItem(
            route = Destinations.DOWNLOADS,
            label = "Downloads",
            icon = { Icon(Icons.Filled.Download, contentDescription = "Downloads") },
        ),
    )

    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val hierarchy = navBackStackEntry?.destination?.hierarchy
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Destinations.LIBRARY,
        ) {
            composable(Destinations.LIBRARY) {
                LibraryScreen(
                    onNavigateToSeries = { series ->
                        navController.navigate(Destinations.series(series.sourceId, series.url))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Destinations.SETTINGS)
                    },
                )
            }
            composable(Destinations.BROWSE) {
                BrowseScreen(
                    onNavigateToSeries = { series ->
                        navController.navigate(Destinations.series(series.sourceId, series.url))
                    },
                )
            }
            composable(Destinations.DOWNLOADS) {
                DownloadsScreen()
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Destinations.SERIES,
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.LongType },
                    navArgument("seriesUrl") { type = NavType.StringType },
                ),
            ) {
                SeriesScreen(
                    onNavigateToReader = { sourceId, seriesUrl, chapterUrl, contentType ->
                        navController.navigate(Destinations.reader(sourceId, seriesUrl, chapterUrl, contentType))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Destinations.READER,
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.LongType },
                    navArgument("seriesUrl") { type = NavType.StringType },
                    navArgument("chapterUrl") { type = NavType.StringType },
                    navArgument("contentType") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                ReaderScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChapter = { chapter ->
                        val contentType = backStackEntry.arguments?.getString("contentType") ?: "NOVEL"
                        navController.navigate(
                            Destinations.reader(chapter.sourceId, chapter.seriesUrl, chapter.url, contentType)
                        ) {
                            popUpTo(Destinations.READER) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

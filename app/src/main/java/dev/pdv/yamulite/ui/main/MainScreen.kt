package dev.pdv.yamulite.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pdv.yamulite.ui.main.favorites.FavoritesScreen
import dev.pdv.yamulite.ui.main.nowplaying.NowPlayingScreen
import dev.pdv.yamulite.ui.main.search.SearchScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Search("search", "Поиск", Icons.Filled.Search),
    Favorites("favorites", "Избранное", Icons.Filled.Favorite),
    NowPlaying("now", "Сейчас", Icons.Filled.PlayArrow),
}

@Composable
fun MainScreen() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != tab.route) {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Search.route,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Tab.Search.route) { SearchScreen() }
            composable(Tab.Favorites.route) { FavoritesScreen() }
            composable(Tab.NowPlaying.route) { NowPlayingScreen() }
        }
    }
}

package com.geomemoir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.geomemoir.presentation.navigation.AppNavGraph
import com.geomemoir.presentation.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import org.maplibre.android.MapLibre

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre GL Native
        MapLibre.getInstance(this)
        
        setContent {
            GeoMemoirAppTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define items to show in bottom nav
    val bottomNavItems = listOf(
        Triple(Screen.Map.route, "Map", Icons.Default.Map),
        Triple(Screen.PlacesList.route, "Places", Icons.Default.List),
        Triple(Screen.Categories.route, "Categories", Icons.Default.Label)
    )

    // Only show bottom navigation on root level tabs
    val showBottomNav = bottomNavItems.any { it.first == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = Color(0xFF1E1E24), // Sleek obsidian grey
                    contentColor = Color.White
                ) {
                    bottomNavItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        ScaffoldContent(
            modifier = Modifier.padding(innerPadding),
            navController = navController
        )
    }
}

@Composable
fun ScaffoldContent(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        AppNavGraph(navController = navController)
    }
}

@Composable
fun GeoMemoirAppTheme(content: @Composable () -> Unit) {
    val customDarkColorScheme = androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF8B5CF6),      // Vivid Violet
        secondary = Color(0xFF06B6D4),    // Cyan Accent
        background = Color(0xFF0F0F13),   // Premium Deep Dark
        surface = Color(0xFF1E1E24)       // Dark Surface Cards
    )

    androidx.compose.material3.MaterialTheme(
        colorScheme = customDarkColorScheme,
        content = content
    )
}

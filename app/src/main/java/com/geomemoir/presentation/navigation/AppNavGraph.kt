package com.geomemoir.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.geomemoir.presentation.category.CategoryScreen
import com.geomemoir.presentation.map.MapScreen
import com.geomemoir.presentation.place.AddEditPlaceScreen
import com.geomemoir.presentation.place.PlaceDetailScreen
import com.geomemoir.presentation.places_list.PlacesListScreen

sealed class Screen(val route: String) {
    object Map : Screen("map")
    object PlacesList : Screen("places_list")
    object PlaceDetail : Screen("place_detail/{placeId}") {
        fun createRoute(placeId: Long) = "place_detail/$placeId"
    }
    object AddEditPlace : Screen("add_edit_place?lat={lat}&lng={lng}&placeId={placeId}") {
        fun createRoute(lat: Double, lng: Double, placeId: Long? = null) =
            "add_edit_place?lat=${lat}&lng=${lng}&placeId=${placeId ?: -1L}"
    }
    object Categories : Screen("categories")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Map.route) {
        composable(Screen.Map.route) {
            MapScreen(navController)
        }
        composable(Screen.PlacesList.route) {
            PlacesListScreen(navController)
        }
        composable(
            route = Screen.PlaceDetail.route,
            arguments = listOf(
                navArgument("placeId") { type = NavType.LongType }
            )
        ) {
            PlaceDetailScreen(navController)
        }
        composable(
            route = Screen.AddEditPlace.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; defaultValue = "" },
                navArgument("lng") { type = NavType.StringType; defaultValue = "" },
                navArgument("placeId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) {
            AddEditPlaceScreen(navController)
        }
        composable(Screen.Categories.route) {
            CategoryScreen(navController)
        }
    }
}

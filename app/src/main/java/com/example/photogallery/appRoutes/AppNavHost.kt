package com.example.photogallery.appRoutes

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.photogallery.features.galleryScreen.GalleryScreen
import com.example.photogallery.features.galleryScreen.PhotoScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.GALLERY_SCREEN_ROUTE) {
        composable(route = AppRoutes.GALLERY_SCREEN_ROUTE) {
            GalleryScreen(
                onOpenPhoto = { index ->
                    navController.navigate("photo/$index")
                }
            )
        }

        composable(
            route = AppRoutes.PHOTO_ROUTE,
            arguments = listOf(navArgument("index"){ type = NavType.IntType })
        ){ navBackStackEntry ->
            val index = navBackStackEntry.arguments?.getInt("index") ?: 0
            PhotoScreen(startIndex = index)
        }
    }
}
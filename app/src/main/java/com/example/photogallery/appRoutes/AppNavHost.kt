package com.example.photogallery.appRoutes

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.photogallery.features.home.HomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.HOME_ROUTE) {
        composable(route = AppRoutes.HOME_ROUTE) {
            HomeScreen(

            )
        }
    }
}
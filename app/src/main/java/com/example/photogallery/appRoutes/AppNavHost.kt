package com.example.photogallery.appRoutes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.photogallery.features.galleryScreen.GalleryScreen
import com.example.photogallery.features.galleryScreen.GalleryViewModel
import com.example.photogallery.features.galleryScreen.PhotoScreen
import com.example.photogallery.features.galleryScreen.SubjectGalleryScreen
import com.example.photogallery.features.galleryScreen.SubjectsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val galleryViewModel: GalleryViewModel = viewModel(activity, factory = GalleryViewModel.Factory)
    // lista globala pt a calcula indexul global
    val allImages = galleryViewModel.images.collectAsStateWithLifecycle(emptyList())

    NavHost(navController = navController, startDestination = AppRoutes.GALLERY_SCREEN_ROUTE) {
        composable(route = AppRoutes.GALLERY_SCREEN_ROUTE) {
            GalleryScreen(
                viewModel = galleryViewModel,
                onOpenPhoto = { index ->
                    navController.navigate("photo/$index")
                },
                onOpenSubjects = {
                    navController.navigate(AppRoutes.SUBJECTS_ROUTE)
                }
            )
        }

        composable(
            route = AppRoutes.PHOTO_ROUTE,
            arguments = listOf(navArgument("index"){ type = NavType.IntType })
        ){ navBackStackEntry ->
            val index = navBackStackEntry.arguments?.getInt("index") ?: 0
            PhotoScreen(
                viewModel = galleryViewModel,
                startIndex = index,
                onClose = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.SUBJECTS_ROUTE) {
            SubjectsScreen(
                viewModel = galleryViewModel,
                onOpenSubject = { id, _ ->
                    navController.navigate("subject/$id")
                }
            )
        }

        composable(
            route = AppRoutes.SUBJECT_GALLERY_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("id") ?: 0L

            SubjectGalleryScreen(
                viewModel = galleryViewModel,
                subjectId = subjectId,
                onOpenPhoto = { uri ->
                    // transform indexul din lista filtrata in indexul din lista globala
                    val idx = allImages.value.indexOf(uri)
                    if (idx >= 0) {
                        navController.navigate("photo/$idx")
                    }
                }
            )
        }
    }
}
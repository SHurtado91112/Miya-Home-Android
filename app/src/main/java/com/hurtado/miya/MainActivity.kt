package com.hurtado.miya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hurtado.miya.features.home.HomeScreen
import com.hurtado.miya.ui.theme.MiyaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity, Compose host — the Android analogue of `MiyaApp.swift`'s SwiftUI `App`
 * (there's no AppDelegate there either). Also the redirect target for the Stage 6 Google OAuth
 * custom-scheme intent filter declared in the manifest.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiyaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiyaNavHost()
                }
            }
        }
    }
}

/**
 * Navigation graph mirroring `HomeFeature.Path` (`sectionDetail`, `albumDetail`,
 * `authorDetail`) plus the sign-in/home gate `AppFeature.State` owns on iOS. Stage 3 replaces
 * the detail placeholders with real `SectionDetailScreen`/`AlbumDetailScreen`/
 * `AuthorDetailScreen`; Stage 4/5 add the media preview overlay; Stage 6 adds the sign-in route.
 */
@Composable
fun MiyaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                onOpenSectionDetail = { sectionId -> navController.navigate("section/$sectionId") },
                onOpenSong = { /* TODO(Stage 5): present SongPreview */ },
                onOpenPhoto = { /* TODO(Stage 4): present PhotoPreview */ },
                onSignOut = { /* TODO(Stage 6): route back to sign-in */ },
            )
        }
        composable("section/{sectionId}") { backStackEntry ->
            PlaceholderDetailScreen(
                title = "Section: ${backStackEntry.arguments?.getString("sectionId")}",
            )
        }
        composable("album/{albumId}") { backStackEntry ->
            PlaceholderDetailScreen(
                title = "Album: ${backStackEntry.arguments?.getString("albumId")}",
            )
        }
        composable("author/{authorId}") { backStackEntry ->
            PlaceholderDetailScreen(
                title = "Author: ${backStackEntry.arguments?.getString("authorId")}",
            )
        }
    }
}

/** Stand-in for the Stage 3 detail screens so navigation is exercisable end-to-end today. */
@Composable
private fun PlaceholderDetailScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$title\n(Stage 3 will replace this)")
    }
}

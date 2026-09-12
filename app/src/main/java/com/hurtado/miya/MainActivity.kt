package com.hurtado.miya

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hurtado.miya.features.albumdetail.AlbumDetailScreen
import com.hurtado.miya.features.authordetail.AuthorDetailScreen
import com.hurtado.miya.features.home.HomeScreen
import com.hurtado.miya.features.sectiondetail.SectionDetailScreen
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

/** Builds the `author/{id}/{name}` route, URL-encoding the name (mirrors passing an `AuthorRef` value directly on iOS). */
private fun authorRoute(id: String, name: String) = "author/${Uri.encode(id)}/${Uri.encode(name)}"

/**
 * Navigation graph mirroring `HomeFeature.Path` (`sectionDetail`, `albumDetail`,
 * `authorDetail`) plus the sign-in/home gate `AppFeature.State` owns on iOS. Stage 4/5 add the
 * media preview overlay; Stage 6 adds the sign-in route.
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

        composable(
            route = "section/{sectionId}",
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType }),
        ) {
            SectionDetailScreen(
                onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                onOpenSong = { /* TODO(Stage 5) */ },
                onOpenPhoto = { /* TODO(Stage 4) */ },
                onOpenAuthor = { author -> navController.navigate(authorRoute(author.id, author.name)) },
            )
        }

        composable(
            route = "album/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
        ) {
            AlbumDetailScreen(
                onOpenSong = { /* TODO(Stage 5) */ },
                onOpenPhoto = { /* TODO(Stage 4) */ },
                onOpenAuthor = { author -> navController.navigate(authorRoute(author.id, author.name)) },
            )
        }

        composable(
            route = "author/{authorId}/{authorName}",
            arguments = listOf(
                navArgument("authorId") { type = NavType.StringType },
                navArgument("authorName") { type = NavType.StringType },
            ),
        ) {
            AuthorDetailScreen(
                onOpenAlbum = { album -> navController.navigate("album/${album.id}") },
                onOpenSong = { /* TODO(Stage 5) */ },
                onOpenPhoto = { /* TODO(Stage 4) */ },
            )
        }
    }
}

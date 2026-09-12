package com.hurtado.miya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hurtado.miya.features.albumdetail.AlbumDetailScreen
import com.hurtado.miya.features.app.AppAction
import com.hurtado.miya.features.app.AppState
import com.hurtado.miya.features.app.AppViewModel
import com.hurtado.miya.features.authordetail.AuthorDetailScreen
import com.hurtado.miya.features.home.HomeScreen
import com.hurtado.miya.features.mediapreview.MEDIA_PREVIEW_BAR_HEIGHT
import com.hurtado.miya.features.mediapreview.MEDIA_PREVIEW_BAR_SPACING
import com.hurtado.miya.features.mediapreview.MediaPreviewAction
import com.hurtado.miya.features.mediapreview.MediaPreviewHost
import com.hurtado.miya.features.mediapreview.MediaPreviewViewModel
import com.hurtado.miya.features.sectiondetail.SectionDetailScreen
import com.hurtado.miya.features.signin.SignInScreen
import com.hurtado.miya.services.auth.OAuthRedirectBus
import com.hurtado.miya.ui.theme.MiyaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity, Compose host — the Android analogue of `MiyaApp.swift`'s SwiftUI `App`
 * (there's no AppDelegate there either). Also the redirect target for the Google OAuth
 * custom-scheme intent filter declared in the manifest — [onCreate]/[onNewIntent] forward the
 * redirect URI into [OAuthRedirectBus] for [com.hurtado.miya.services.auth.GoogleOAuthLauncher]
 * to pick up.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var oauthRedirectBus: OAuthRedirectBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        forwardOAuthRedirect(intent)
        setContent {
            MiyaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        forwardOAuthRedirect(intent)
    }

    private fun forwardOAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "com.hurtado.miya.oauth") oauthRedirectBus.emit(data)
    }
}

/**
 * Root gate mirroring `AppView`: sign-in wall until there's a session, then Home. Restoring shows
 * a spinner rather than flashing the sign-in button before the session read finishes.
 */
@Composable
fun AppRoot(appViewModel: AppViewModel = hiltViewModel()) {
    val state by appViewModel.store.state.collectAsState()
    val store = appViewModel.store

    LaunchedEffect(Unit) { store.send(AppAction.Appeared) }

    when (val current = state) {
        is AppState.Restoring -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is AppState.SignedOut -> SignInScreen(
            onSignedIn = { profile -> store.send(AppAction.SignedIn(profile)) },
        )

        is AppState.SignedIn -> MiyaNavHost(
            onSignOut = { store.send(AppAction.SignOutRequested) },
        )
    }
}

/** Builds the `author/{id}/{name}` route, URL-encoding the name (mirrors passing an `AuthorRef` value directly on iOS). */
private fun authorRoute(id: String, name: String) = "author/${Uri.encode(id)}/${Uri.encode(name)}"

/**
 * Navigation graph mirroring `HomeFeature.Path` (`sectionDetail`, `albumDetail`,
 * `authorDetail`). The [MediaPreviewHost] overlay is composed alongside the [NavHost] (not
 * inside it), so its [MediaPreviewViewModel] is scoped to this composable's caller and survives
 * pushing/popping the back stack underneath it, mirroring `@Presents var preview` living on
 * `HomeFeature.State` above the pushed detail screens. Recomposed fresh (a new
 * `ViewModelStoreOwner`) each time [AppRoot] switches to [AppState.SignedIn], so signing out and
 * back in never leaks the previous session's `HomeViewModel`/`MediaPreviewViewModel` state —
 * the Android equivalent of iOS destroying `HomeFeature.State` on sign-out.
 */
@Composable
fun MiyaNavHost(onSignOut: () -> Unit, navController: NavHostController = rememberNavController()) {
    val mediaPreviewViewModel: MediaPreviewViewModel = hiltViewModel()
    val mediaPreviewStore = mediaPreviewViewModel.store
    val previewState by mediaPreviewStore.state.collectAsState()

    // Space every scrolling screen reserves at the bottom so its last row is never hidden behind
    // the docked mini bar(s) — mirrors the `safeAreaInset` each iOS detail/home view adds for
    // `collapsedPreviewHeight`.
    val dockedCount = previewState.dockedKinds.size
    val bottomInset = if (dockedCount == 0) {
        0.dp
    } else {
        MEDIA_PREVIEW_BAR_HEIGHT * dockedCount + MEDIA_PREVIEW_BAR_SPACING * (dockedCount - 1).coerceAtLeast(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                    onOpenSectionDetail = { sectionId -> navController.navigate("section/$sectionId") },
                    onOpenSong = { item, siblings ->
                        mediaPreviewStore.send(MediaPreviewAction.OpenSong(item, siblings))
                    },
                    onOpenPhoto = { item -> mediaPreviewStore.send(MediaPreviewAction.OpenPhoto(item)) },
                    onSignOut = onSignOut,
                    bottomInset = bottomInset,
                )
            }

            composable(
                route = "section/{sectionId}",
                arguments = listOf(navArgument("sectionId") { type = NavType.StringType }),
            ) {
                SectionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                    onOpenSong = { item, siblings ->
                        mediaPreviewStore.send(MediaPreviewAction.OpenSong(item, siblings))
                    },
                    onOpenPhoto = { item -> mediaPreviewStore.send(MediaPreviewAction.OpenPhoto(item)) },
                    onOpenAuthor = { author -> navController.navigate(authorRoute(author.id, author.name)) },
                    bottomInset = bottomInset,
                )
            }

            composable(
                route = "album/{albumId}",
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
            ) {
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSong = { item, siblings ->
                        mediaPreviewStore.send(MediaPreviewAction.OpenSong(item, siblings))
                    },
                    onOpenPhoto = { item -> mediaPreviewStore.send(MediaPreviewAction.OpenPhoto(item)) },
                    onOpenAuthor = { author -> navController.navigate(authorRoute(author.id, author.name)) },
                    bottomInset = bottomInset,
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
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { album -> navController.navigate("album/${album.id}") },
                    onOpenSong = { item, siblings ->
                        mediaPreviewStore.send(MediaPreviewAction.OpenSong(item, siblings))
                    },
                    onOpenPhoto = { item -> mediaPreviewStore.send(MediaPreviewAction.OpenPhoto(item)) },
                    bottomInset = bottomInset,
                )
            }
        }

        MediaPreviewHost(
            onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
            onOpenAuthor = { author -> navController.navigate(authorRoute(author.id, author.name)) },
            viewModel = mediaPreviewViewModel,
        )
    }
}

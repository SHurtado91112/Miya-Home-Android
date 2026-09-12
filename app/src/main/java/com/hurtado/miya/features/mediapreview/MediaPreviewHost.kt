package com.hurtado.miya.features.mediapreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.features.home.AuthorRef

/** Room one docked mini bar takes, mirrors `MediaPreview.barHeight`/`barSpacing`. */
val MEDIA_PREVIEW_BAR_HEIGHT = 76.dp
val MEDIA_PREVIEW_BAR_SPACING = 8.dp

/**
 * Renders on top of [com.hurtado.miya.MiyaNavHost]'s content, mirrors `MediaPreviewView` (the
 * expanded sheet, shown for whichever of photo/song is at [MediaPreviewState.expandedKind]) +
 * `MediaPreviewBarsView` (the docked mini-bar stack, photo above song) together — Compose has no
 * separate sheet-vs-overlay presentation layer to split them across the way iOS does.
 */
@Composable
fun MediaPreviewHost(
    onOpenAlbum: (String) -> Unit,
    onOpenAuthor: (AuthorRef) -> Unit,
    viewModel: MediaPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store

    val photo = state.photo
    val song = state.song

    when (state.expandedKind) {
        MediaPreviewKind.PHOTO -> if (photo != null) {
            PhotoPreviewScreen(
                state = photo,
                onAction = { action ->
                    store.send(MediaPreviewAction.Photo(action))
                    when (action) {
                        is PhotoPreviewAction.View.ViewAlbumTapped -> photo.item.albumID?.let(onOpenAlbum)
                        is PhotoPreviewAction.View.AuthorTapped -> onOpenAuthor(action.author)
                        else -> Unit
                    }
                },
            )
        }

        MediaPreviewKind.SONG -> if (song != null) {
            SongPreviewScreen(
                state = song,
                onAction = { action ->
                    store.send(MediaPreviewAction.Song(action))
                    when (action) {
                        is SongPreviewAction.View.ViewAlbumTapped -> song.item.albumID?.let(onOpenAlbum)
                        is SongPreviewAction.View.AuthorTapped -> onOpenAuthor(action.author)
                        else -> Unit
                    }
                },
            )
        }

        null -> if (state.dockedKinds.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MEDIA_PREVIEW_BAR_SPACING, Alignment.Bottom),
            ) {
                if (photo != null) {
                    PhotoMiniBar(state = photo, onAction = { store.send(MediaPreviewAction.Photo(it)) })
                }
                if (song != null) {
                    SongMiniBar(state = song, onAction = { store.send(MediaPreviewAction.Song(it)) })
                }
            }
        }
    }
}

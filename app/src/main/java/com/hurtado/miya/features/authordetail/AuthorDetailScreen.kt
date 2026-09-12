package com.hurtado.miya.features.authordetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind
import com.hurtado.miya.views.AlbumCardGrid
import com.hurtado.miya.views.DetailItemGrid

/** Port of `AuthorDetailView.swift`. */
@Composable
fun AuthorDetailScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenSong: (HomeSectionItem) -> Unit,
    onOpenPhoto: (HomeSectionItem) -> Unit,
    viewModel: AuthorDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store

    LaunchedEffect(Unit) { store.send(AuthorDetailAction.View.Appeared) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = state.author.name, style = MaterialTheme.typography.displayMedium)

            if (state.albums.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Albums", style = MaterialTheme.typography.headlineMedium)
                    AlbumCardGrid(
                        albums = state.albums,
                        onAlbumClick = { album ->
                            store.send(AuthorDetailAction.View.AlbumTapped(album.id))
                            onOpenAlbum(album)
                        },
                    )
                }
            }

            DetailItemGrid(
                items = state.items,
                hasMore = state.itemsHasMore,
                onItemClick = { item ->
                    store.send(AuthorDetailAction.View.ItemTapped(item.id))
                    when (item.kind) {
                        MediaKind.PHOTO -> onOpenPhoto(item)
                        MediaKind.SONG -> onOpenSong(item)
                        MediaKind.ALBUM -> Unit
                    }
                },
                onReachedEnd = { store.send(AuthorDetailAction.View.ReachedEnd) },
            )
        }
    }
}

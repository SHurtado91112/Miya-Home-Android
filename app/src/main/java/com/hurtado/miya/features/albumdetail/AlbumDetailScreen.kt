package com.hurtado.miya.features.albumdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind
import com.hurtado.miya.views.DetailItemGrid

/** Port of `AlbumDetailView.swift`. */
@Composable
fun AlbumDetailScreen(
    onOpenSong: (HomeSectionItem) -> Unit,
    onOpenPhoto: (HomeSectionItem) -> Unit,
    onOpenAuthor: (AuthorRef) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isLoadingAlbum) {
                CircularProgressIndicator()
                return@Column
            }

            Column {
                Text(text = state.album.title, style = MaterialTheme.typography.displayMedium)
                val author = state.album.author
                if (author != null) {
                    Row(
                        modifier = Modifier.clickable {
                            store.send(AlbumDetailAction.View.AuthorTapped(author))
                            onOpenAuthor(author)
                        },
                    ) {
                        Text(
                            text = author.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = state.album.subtitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DetailItemGrid(
                items = state.album.items,
                hasMore = state.album.itemsHasMore,
                onItemClick = { item ->
                    store.send(AlbumDetailAction.View.ItemTapped(item.id))
                    when (item.kind) {
                        MediaKind.PHOTO -> onOpenPhoto(item)
                        MediaKind.SONG -> onOpenSong(item)
                        MediaKind.ALBUM -> Unit // albums don't nest albums in this data model
                    }
                },
                onReachedEnd = { store.send(AlbumDetailAction.View.ReachedEnd) },
            )
        }
    }
}

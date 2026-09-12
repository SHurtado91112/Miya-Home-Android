package com.hurtado.miya.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.views.SectionCardGrid

/**
 * Root Home screen, port of `HomeView.swift`. `NavigationStack` + pull-to-refresh + account menu
 * → sign out. The bottom `safeAreaInset` reserved for the mini playback bar becomes a Stage 5
 * addition once `SongPreviewFeature`/`MediaPreviewFeature` exist; omitted for now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAlbum: (String) -> Unit,
    onOpenSectionDetail: (String) -> Unit,
    onOpenSong: (HomeSectionItem) -> Unit,
    onOpenPhoto: (HomeSectionItem) -> Unit,
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store

    LaunchedEffect(Unit) { store.send(HomeAction.View.Appeared) }

    // Delegate-equivalent routing: AppFeature/NavHost would observe HomeFeature's `.delegate`
    // actions in TCA; a hand-rolled Store has no case-path `.forEach` for that, so routing is
    // triggered directly from the `onClick`s below (each also sends the matching View action so
    // the reducer's own bookkeeping stays in sync).
    var showAccountMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = state.title, style = MaterialTheme.typography.displayMedium) },
                actions = {
                    IconButton(onClick = { showAccountMenu = true }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                    DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            onClick = {
                                showAccountMenu = false
                                store.send(HomeAction.View.SignOutTapped)
                                onSignOut()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { store.send(HomeAction.View.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.sections.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                    ) {
                        items(state.sections, key = { it.id }) { section ->
                            Column {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                                SectionCardGrid(
                                    items = section.items,
                                    onItemClick = { item ->
                                        store.send(HomeAction.View.ItemTapped(item.id, section.id))
                                        when (item.kind) {
                                            MediaKind.ALBUM -> onOpenAlbum(item.id)
                                            MediaKind.PHOTO -> onOpenPhoto(item)
                                            MediaKind.SONG -> onOpenSong(item)
                                        }
                                    },
                                    onSeeAllClick = {
                                        store.send(HomeAction.View.SeeAllTapped(section.id))
                                        onOpenSectionDetail(section.id)
                                    },
                                )
                            }
                        }

                        if (state.albums.isNotEmpty()) {
                            item(key = "albums-header") {
                                Text(
                                    text = "Albums",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                            item(key = "albums-grid") {
                                com.hurtado.miya.views.AlbumCardGrid(
                                    albums = state.albums,
                                    onAlbumClick = { album ->
                                        store.send(HomeAction.View.AlbumTapped(album.id))
                                        onOpenAlbum(album.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

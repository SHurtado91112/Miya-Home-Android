package com.hurtado.miya.features.sectiondetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind
import com.hurtado.miya.views.AuthorRow
import com.hurtado.miya.views.DetailItemGrid
import com.hurtado.miya.views.DetailTopBar
import kotlinx.coroutines.delay

/** Port of `SectionDetailView.swift`: full item list + debounced server search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenSong: (HomeSectionItem, List<HomeSectionItem>) -> Unit,
    onOpenPhoto: (HomeSectionItem) -> Unit,
    onOpenAuthor: (AuthorRef) -> Unit,
    bottomInset: Dp = 0.dp,
    viewModel: SectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.store.state.collectAsState()
    val store = viewModel.store
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.autoFocusSearch) {
        if (state.autoFocusSearch) {
            delay(300)
            focusRequester.requestFocus()
            store.send(SectionDetailAction.View.FocusConsumed)
        }
    }

    Scaffold(topBar = { DetailTopBar(onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = state.section.title, style = MaterialTheme.typography.displayMedium)

            OutlinedTextField(
                value = state.query,
                onValueChange = { store.send(SectionDetailAction.View.QueryChanged(it)) },
                placeholder = { Text("Search ${state.section.title}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
            )

            if (state.searchAuthors.isNotEmpty()) {
                Column {
                    state.searchAuthors.forEach { author ->
                        AuthorRow(name = author.name, onClick = {
                            store.send(SectionDetailAction.View.AuthorTapped(author))
                            onOpenAuthor(author)
                        })
                    }
                }
            }

            if (state.showsEmptyState) {
                Text(
                    text = "No results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }

            if (state.isLoadingSection) {
                CircularProgressIndicator()
            } else {
                DetailItemGrid(
                    items = state.displayedItems,
                    hasMore = state.isSearching && state.searchHasMore,
                    onItemClick = { item ->
                        store.send(SectionDetailAction.View.ItemTapped(item.id))
                        when (item.kind) {
                            MediaKind.ALBUM -> onOpenAlbum(item.id)
                            MediaKind.PHOTO -> onOpenPhoto(item)
                            MediaKind.SONG -> onOpenSong(item, state.displayedItems)
                        }
                    },
                    onReachedEnd = { store.send(SectionDetailAction.View.ReachedEnd) },
                )
            }
        }
    }
}

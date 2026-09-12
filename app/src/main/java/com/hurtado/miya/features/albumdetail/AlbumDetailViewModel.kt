package com.hurtado.miya.features.albumdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.services.HomeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the [Album] via [HomeClient.loadAlbumNode] — the Android analogue of iOS's
 * cache-miss album resolution (`loadAlbumNode(nodeID:)`), used here unconditionally since a
 * plain string nav arg can't carry the already-loaded object the way iOS's push can.
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reducer: AlbumDetailReducer,
    private val homeClient: HomeClient,
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])

    val store = Store(
        initialState = AlbumDetailState(
            album = Album(id = albumId, title = "", subtitle = "", systemImage = "square.stack"),
            isLoadingAlbum = true,
        ),
        reducer = reducer,
        scope = viewModelScope,
    )

    init {
        viewModelScope.launch {
            val album = homeClient.loadAlbumNode(albumId)
            if (album != null) {
                store.send(AlbumDetailAction.AlbumLoaded(album))
            }
        }
    }
}

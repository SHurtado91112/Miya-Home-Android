package com.hurtado.miya.features.albumdetail

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.HomeClient
import javax.inject.Inject

/** Port of `Miya/Features/AlbumDetailFeature.swift`: one album's item grid, cursor-paginated. */
data class AlbumDetailState(
    val album: Album,
    val isLoadingMore: Boolean = false,
    val isLoadingAlbum: Boolean = false,
)

sealed interface AlbumDetailAction {
    sealed interface View : AlbumDetailAction {
        data class ItemTapped(val itemId: String) : View
        data class AuthorTapped(val author: AuthorRef) : View
        data object ReachedEnd : View
    }

    sealed interface Delegate : AlbumDetailAction {
        data class ItemTapped(val item: HomeSectionItem) : Delegate
        data class AuthorTapped(val author: AuthorRef) : Delegate
        data object DidPaginate : Delegate
    }

    /** Resolves the album by id once loaded (see [AlbumDetailViewModel]'s cache-miss lookup). */
    data class AlbumLoaded(val album: Album) : AlbumDetailAction
    data class PageLoaded(val elements: List<HomeSectionItem>, val cursor: String?, val hasMore: Boolean) : AlbumDetailAction
    data object PageLoadFailed : AlbumDetailAction
}

class AlbumDetailReducer @Inject constructor(
    private val homeClient: HomeClient,
) : Reducer<AlbumDetailState, AlbumDetailAction> {

    private enum class CancelId { Paginate }

    override fun reduce(
        state: AlbumDetailState,
        action: AlbumDetailAction,
    ): Pair<AlbumDetailState, Effect<AlbumDetailAction>> = when (action) {
        is AlbumDetailAction.AlbumLoaded ->
            state.copy(album = action.album, isLoadingAlbum = false) to Effect.none()

        is AlbumDetailAction.View.ItemTapped -> {
            val item = state.album.items.firstOrNull { it.id == action.itemId }
            if (item == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<AlbumDetailAction> { send -> send(AlbumDetailAction.Delegate.ItemTapped(item)) }
            }
        }

        is AlbumDetailAction.View.AuthorTapped ->
            state to Effect.Run<AlbumDetailAction> { send -> send(AlbumDetailAction.Delegate.AuthorTapped(action.author)) }

        is AlbumDetailAction.View.ReachedEnd -> {
            if (state.isLoadingMore || !state.album.itemsHasMore || state.album.itemsCursor == null) {
                state to Effect.none()
            } else {
                state.copy(isLoadingMore = true) to Effect.Run<AlbumDetailAction>(
                    id = CancelId.Paginate,
                    cancelInFlight = true,
                ) { send ->
                    try {
                        val page = homeClient.loadAlbumItems(state.album.id, state.album.itemsCursor)
                        send(AlbumDetailAction.PageLoaded(page.elements, page.cursor, page.hasMore))
                    } catch (t: Throwable) {
                        send(AlbumDetailAction.PageLoadFailed)
                    }
                }
            }
        }

        is AlbumDetailAction.PageLoaded -> {
            val next = state.copy(
                isLoadingMore = false,
                album = state.album.copy(
                    items = state.album.items + action.elements,
                    itemsCursor = action.cursor,
                    itemsHasMore = action.hasMore,
                ),
            )
            next to Effect.Run<AlbumDetailAction> { send -> send(AlbumDetailAction.Delegate.DidPaginate) }
        }

        is AlbumDetailAction.PageLoadFailed ->
            state.copy(isLoadingMore = false) to Effect.none()

        is AlbumDetailAction.Delegate -> state to Effect.none()
    }
}

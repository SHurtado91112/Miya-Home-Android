package com.hurtado.miya.features.authordetail

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.HomeClient
import javax.inject.Inject

/**
 * Port of `Miya/Features/AuthorDetailFeature.swift`: an author's full library — an unpaginated
 * "Albums" shelf loaded once, above a cursor-paginated item grid (items recur across albums /
 * overlapping pages, so appended pages are deduped by id).
 */
data class AuthorDetailState(
    val author: AuthorRef,
    val albums: List<Album> = emptyList(),
    val items: List<HomeSectionItem> = emptyList(),
    val itemsCursor: String? = null,
    /** True until the first page proves otherwise, mirrors iOS's optimistic initial value. */
    val itemsHasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasLoadedFirstPage: Boolean = false,
)

sealed interface AuthorDetailAction {
    sealed interface View : AuthorDetailAction {
        data object Appeared : View
        data class ItemTapped(val itemId: String) : View
        data class AlbumTapped(val albumId: String) : View
        data object ReachedEnd : View
    }

    sealed interface Delegate : AuthorDetailAction {
        data class ItemTapped(val item: HomeSectionItem) : Delegate
        data class AlbumTapped(val album: Album) : Delegate
        data object DidPaginate : Delegate
    }

    data class PageLoaded(val elements: List<HomeSectionItem>, val cursor: String?, val hasMore: Boolean) : AuthorDetailAction
    data object PageLoadFailed : AuthorDetailAction
    data class AlbumsLoaded(val albums: List<Album>) : AuthorDetailAction
}

class AuthorDetailReducer @Inject constructor(
    private val homeClient: HomeClient,
) : Reducer<AuthorDetailState, AuthorDetailAction> {

    private enum class CancelId { Paginate }

    override fun reduce(
        state: AuthorDetailState,
        action: AuthorDetailAction,
    ): Pair<AuthorDetailState, Effect<AuthorDetailAction>> = when (action) {
        is AuthorDetailAction.View.Appeared -> {
            if (state.hasLoadedFirstPage || state.isLoadingMore) {
                state to Effect.none()
            } else {
                state.copy(isLoadingMore = true) to Effect.merge(
                    loadAlbumsEffect(state.author.id),
                    loadItemsEffect(state.author.id, cursor = null),
                )
            }
        }

        is AuthorDetailAction.View.ItemTapped -> {
            val item = state.items.firstOrNull { it.id == action.itemId }
            if (item == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<AuthorDetailAction> { send -> send(AuthorDetailAction.Delegate.ItemTapped(item)) }
            }
        }

        is AuthorDetailAction.View.AlbumTapped -> {
            val album = state.albums.firstOrNull { it.id == action.albumId }
            if (album == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<AuthorDetailAction> { send -> send(AuthorDetailAction.Delegate.AlbumTapped(album)) }
            }
        }

        is AuthorDetailAction.AlbumsLoaded -> state.copy(albums = action.albums) to Effect.none()

        is AuthorDetailAction.View.ReachedEnd -> {
            if (!state.hasLoadedFirstPage || state.isLoadingMore || !state.itemsHasMore || state.itemsCursor == null) {
                state to Effect.none()
            } else {
                state.copy(isLoadingMore = true) to loadItemsEffect(state.author.id, state.itemsCursor)
            }
        }

        is AuthorDetailAction.PageLoaded -> {
            val fresh = action.elements.filter { new -> state.items.none { it.id == new.id } }
            val next = state.copy(
                isLoadingMore = false,
                hasLoadedFirstPage = true,
                items = state.items + fresh,
                itemsCursor = action.cursor,
                itemsHasMore = action.hasMore,
            )
            next to Effect.Run<AuthorDetailAction> { send -> send(AuthorDetailAction.Delegate.DidPaginate) }
        }

        is AuthorDetailAction.PageLoadFailed ->
            state.copy(isLoadingMore = false, hasLoadedFirstPage = true, itemsHasMore = false) to Effect.none()

        is AuthorDetailAction.Delegate -> state to Effect.none()
    }

    private fun loadItemsEffect(authorId: String, cursor: String?): Effect<AuthorDetailAction> =
        Effect.Run(id = CancelId.Paginate, cancelInFlight = true) { send ->
            try {
                val page = homeClient.loadAuthorItems(authorId, cursor)
                send(AuthorDetailAction.PageLoaded(page.elements, page.cursor, page.hasMore))
            } catch (t: Throwable) {
                send(AuthorDetailAction.PageLoadFailed)
            }
        }

    private fun loadAlbumsEffect(authorId: String): Effect<AuthorDetailAction> = Effect.Run { send ->
        try {
            send(AuthorDetailAction.AlbumsLoaded(homeClient.loadAuthorAlbums(authorId)))
        } catch (t: Throwable) {
            // Best-effort shelf; failure just leaves the Albums section empty, mirrors iOS
            // (`loadAlbums` only reports the issue, no failure action).
        }
    }
}

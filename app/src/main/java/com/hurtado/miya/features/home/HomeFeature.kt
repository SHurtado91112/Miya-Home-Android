package com.hurtado.miya.features.home

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.services.HomeClient
import javax.inject.Inject

/**
 * Port of `Miya/Features/HomeFeature.swift`. The iOS reducer also owns the nav `StackState` and
 * the `@Presents` media preview; here those live one level up, in `MainActivity`'s NavHost +
 * a sibling `MediaPreviewFeature` store (Stage 3/4), since a hand-rolled store has no
 * `scope(state:action:)`/`StackState` equivalent — Navigation-Compose owns the back stack
 * directly and this reducer only reports *intent* to navigate via [Action.Delegate].
 */
data class HomeState(
    val title: String = "Miya",
    val sections: List<HomeSection> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadError: String? = null,
)

sealed interface HomeAction {
    sealed interface View : HomeAction {
        data object Appeared : View
        data object Refresh : View
        data class ItemTapped(val itemId: String, val sectionId: String) : View
        data class AlbumTapped(val albumId: String) : View
        data class SeeAllTapped(val sectionId: String) : View
        data object SignOutTapped : View
    }

    sealed interface Delegate : HomeAction {
        data class OpenSong(val item: HomeSectionItem) : Delegate
        data class OpenPhoto(val item: HomeSectionItem) : Delegate
        data class OpenAlbum(val albumId: String) : Delegate
        data class OpenSectionDetail(val sectionId: String) : Delegate
        data object SignOutRequested : Delegate
    }

    data class SectionsLoaded(val sections: List<HomeSection>) : HomeAction
    data class AlbumsLoaded(val albums: List<Album>) : HomeAction
    data class LoadFailed(val message: String) : HomeAction
}

class HomeReducer @Inject constructor(
    private val homeClient: HomeClient,
) : Reducer<HomeState, HomeAction> {

    override fun reduce(state: HomeState, action: HomeAction): Pair<HomeState, Effect<HomeAction>> {
        return when (action) {
            is HomeAction.View.Appeared -> {
                if (state.sections.isNotEmpty() || state.isLoading) return state to Effect.none()
                state.copy(isLoading = true, loadError = null) to loadEffect()
            }

            is HomeAction.View.Refresh -> {
                state.copy(isRefreshing = true, loadError = null) to loadEffect()
            }

            is HomeAction.SectionsLoaded -> {
                state.copy(sections = action.sections, isLoading = false, isRefreshing = false) to Effect.none()
            }

            is HomeAction.AlbumsLoaded -> {
                state.copy(albums = action.albums, isLoading = false, isRefreshing = false) to Effect.none()
            }

            is HomeAction.LoadFailed -> {
                state.copy(isLoading = false, isRefreshing = false, loadError = action.message) to Effect.none()
            }

            is HomeAction.View.ItemTapped -> {
                val item = state.sections.firstOrNull { it.id == action.sectionId }
                    ?.items?.firstOrNull { it.id == action.itemId }
                    ?: return state to Effect.none()
                state to openItemEffect(item)
            }

            is HomeAction.View.AlbumTapped -> state to Effect.Run<HomeAction> { send ->
                send(HomeAction.Delegate.OpenAlbum(action.albumId))
            }

            is HomeAction.View.SeeAllTapped -> state to Effect.Run<HomeAction> { send ->
                send(HomeAction.Delegate.OpenSectionDetail(action.sectionId))
            }

            is HomeAction.View.SignOutTapped -> state to Effect.Run<HomeAction> { send ->
                send(HomeAction.Delegate.SignOutRequested)
            }

            // Delegate actions are only ever *sent out*, never handled here — the parent
            // (AppFeature) observes them by inspecting emitted actions, mirroring TCA's
            // `case .delegate:` no-op arm.
            is HomeAction.Delegate -> state to Effect.none()
        }
    }

    /** Mirrors `HomeFeature`'s item-open routing: album -> push, photo -> preview, song -> preview+start. */
    private fun openItemEffect(item: HomeSectionItem): Effect<HomeAction> = Effect.Run { send ->
        when (item.kind) {
            MediaKind.ALBUM -> send(HomeAction.Delegate.OpenAlbum(item.id))
            MediaKind.PHOTO -> send(HomeAction.Delegate.OpenPhoto(item))
            MediaKind.SONG -> send(HomeAction.Delegate.OpenSong(item))
        }
    }

    private fun loadEffect(): Effect<HomeAction> = Effect.merge(
        Effect.Run(id = CancelId.LoadSections, cancelInFlight = true) { send ->
            try {
                send(HomeAction.SectionsLoaded(homeClient.loadSections()))
            } catch (t: Throwable) {
                send(HomeAction.LoadFailed(t.message ?: "Failed to load sections"))
            }
        },
        Effect.Run(id = CancelId.LoadAlbums, cancelInFlight = true) { send ->
            try {
                send(HomeAction.AlbumsLoaded(homeClient.loadAlbums(after = null).elements))
            } catch (t: Throwable) {
                send(HomeAction.LoadFailed(t.message ?: "Failed to load albums"))
            }
        },
    )

    private enum class CancelId { LoadSections, LoadAlbums }
}

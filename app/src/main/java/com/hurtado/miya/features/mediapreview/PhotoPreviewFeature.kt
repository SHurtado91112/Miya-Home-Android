package com.hurtado.miya.features.mediapreview

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import javax.inject.Inject

/** Mirrors `PresentationDetent` as used by `PhotoPreviewFeature`/`SongPreviewFeature`: mini bar vs full screen. */
enum class PreviewDetent { MINI, LARGE }

/** Port of `Miya/Features/PhotoPreviewFeature.swift`. */
data class PhotoPreviewState(
    val item: HomeSectionItem,
    val showsMetadata: Boolean = false,
    val detent: PreviewDetent = PreviewDetent.LARGE,
)

sealed interface PhotoPreviewAction {
    sealed interface View : PhotoPreviewAction {
        data object CloseTapped : View
        data object ToggleMetadataTapped : View
        data object ViewAlbumTapped : View
        data class AuthorTapped(val author: AuthorRef) : View
        data object ExpandTapped : View

        /** Android-only addition: there's no drag-to-dismiss sheet gesture here like iOS's
         * presentation detents, so the system back gesture/button minimizes to the mini bar
         * instead of doing nothing. */
        data object MinimizeTapped : View
    }

    sealed interface Delegate : PhotoPreviewAction {
        data class ViewAlbumTapped(val albumId: String) : Delegate
        data class AuthorTapped(val author: AuthorRef) : Delegate
        /** The user dismissed this preview from its mini bar or the close button. */
        data object Closed : Delegate
    }
}

class PhotoPreviewReducer @Inject constructor() : Reducer<PhotoPreviewState, PhotoPreviewAction> {

    override fun reduce(
        state: PhotoPreviewState,
        action: PhotoPreviewAction,
    ): Pair<PhotoPreviewState, Effect<PhotoPreviewAction>> = when (action) {
        is PhotoPreviewAction.View.CloseTapped ->
            state to Effect.Run<PhotoPreviewAction> { send -> send(PhotoPreviewAction.Delegate.Closed) }

        is PhotoPreviewAction.View.ToggleMetadataTapped ->
            state.copy(showsMetadata = !state.showsMetadata) to Effect.none()

        is PhotoPreviewAction.View.ViewAlbumTapped -> {
            val albumId = state.item.albumID
            if (albumId == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<PhotoPreviewAction> { send -> send(PhotoPreviewAction.Delegate.ViewAlbumTapped(albumId)) }
            }
        }

        is PhotoPreviewAction.View.AuthorTapped ->
            state to Effect.Run<PhotoPreviewAction> { send -> send(PhotoPreviewAction.Delegate.AuthorTapped(action.author)) }

        is PhotoPreviewAction.View.ExpandTapped ->
            state.copy(detent = PreviewDetent.LARGE) to Effect.none()

        is PhotoPreviewAction.View.MinimizeTapped ->
            state.copy(detent = PreviewDetent.MINI) to Effect.none()

        is PhotoPreviewAction.Delegate -> state to Effect.none()
    }
}

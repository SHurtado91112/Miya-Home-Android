package com.hurtado.miya.features.mediapreview

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.architecture.map
import com.hurtado.miya.features.home.HomeSectionItem
import javax.inject.Inject

/**
 * Port of `Miya/Features/MediaPreview.swift`: the floating preview presented when a section item
 * is tapped. iOS holds up to one song *and* one photo at once; this stage only wires the photo
 * slot (Stage 5 adds a `song: SongPreviewState?` slot alongside it, plus the cross-detent rule —
 * expanding one collapses the other to its mini bar — mirrored from `MediaPreview.body`'s
 * `.song(.view(.expandTapped))` / `.photo(.view(.expandTapped))` cases).
 */
enum class MediaPreviewKind { PHOTO }

data class MediaPreviewState(
    val photo: PhotoPreviewState? = null,
) {
    val isEmpty: Boolean get() = photo == null

    /** The preview currently expanded full-screen, if any. */
    val expandedKind: MediaPreviewKind?
        get() = if (photo?.detent == PreviewDetent.LARGE) MediaPreviewKind.PHOTO else null

    /** The mini bars docked at the bottom right now, top-to-bottom. */
    val dockedKinds: List<MediaPreviewKind>
        get() = if (expandedKind != null) emptyList() else listOfNotNull(photo?.let { MediaPreviewKind.PHOTO })
}

sealed interface MediaPreviewAction {
    data class Photo(val action: PhotoPreviewAction) : MediaPreviewAction

    /**
     * Folds a freshly tapped item into the preview, mirrors `MediaPreview.opening(_:siblings:into:)`.
     * [siblings] is the list the item was tapped in — reserved for Stage 5's play queue.
     */
    data class OpenPhoto(val item: HomeSectionItem, val siblings: List<HomeSectionItem> = emptyList()) : MediaPreviewAction
}

class MediaPreviewReducer @Inject constructor(
    private val photoReducer: PhotoPreviewReducer,
) : Reducer<MediaPreviewState, MediaPreviewAction> {

    override fun reduce(
        state: MediaPreviewState,
        action: MediaPreviewAction,
    ): Pair<MediaPreviewState, Effect<MediaPreviewAction>> = when (action) {
        is MediaPreviewAction.OpenPhoto ->
            state.copy(photo = PhotoPreviewState(item = action.item)) to Effect.none()

        is MediaPreviewAction.Photo -> {
            val current = state.photo
            if (current == null) {
                state to Effect.none()
            } else {
                val (nextPhoto, effect) = photoReducer.reduce(current, action.action)
                val closed = action.action is PhotoPreviewAction.Delegate.Closed
                state.copy(photo = if (closed) null else nextPhoto) to effect.map { MediaPreviewAction.Photo(it) }
            }
        }
    }
}

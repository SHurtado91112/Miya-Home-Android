package com.hurtado.miya.features.mediapreview

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.architecture.map
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind
import javax.inject.Inject

/**
 * Port of `Miya/Features/MediaPreview.swift`: the floating preview presented when a section item
 * is tapped. Holds up to one song *and* one photo at once — opening a photo never dismisses a
 * pending song preview (and vice versa). At most one may be expanded; the rest render as stacked
 * mini bars, and tapping a bar expands that one.
 */
enum class MediaPreviewKind { PHOTO, SONG }

data class MediaPreviewState(
    val photo: PhotoPreviewState? = null,
    val song: SongPreviewState? = null,
) {
    val isEmpty: Boolean get() = photo == null && song == null

    /** The preview currently expanded full-screen, if any. Driven purely by the children's
     * `detent`, so minimizing one drops straight back to the docked bar stack. */
    val expandedKind: MediaPreviewKind?
        get() = when {
            song?.detent == PreviewDetent.LARGE -> MediaPreviewKind.SONG
            photo?.detent == PreviewDetent.LARGE -> MediaPreviewKind.PHOTO
            else -> null
        }

    /** The mini bars docked at the bottom right now, top-to-bottom (photo above song, matching
     * `MediaPreviewBarsView`'s fixed order). */
    val dockedKinds: List<MediaPreviewKind>
        get() = if (expandedKind != null) {
            emptyList()
        } else {
            listOfNotNull(
                photo?.let { MediaPreviewKind.PHOTO },
                song?.let { MediaPreviewKind.SONG },
            )
        }
}

sealed interface MediaPreviewAction {
    data class Photo(val action: PhotoPreviewAction) : MediaPreviewAction
    data class Song(val action: SongPreviewAction) : MediaPreviewAction

    /** Folds a freshly tapped photo into the preview, mirrors `MediaPreview.opening(_:siblings:into:)`. */
    data class OpenPhoto(val item: HomeSectionItem) : MediaPreviewAction

    /** Folds a freshly tapped song in and starts playback. [siblings] is the list the item was
     * tapped in — its songs become the play queue. */
    data class OpenSong(val item: HomeSectionItem, val siblings: List<HomeSectionItem> = emptyList()) : MediaPreviewAction
}

class MediaPreviewReducer @Inject constructor(
    private val photoReducer: PhotoPreviewReducer,
    private val songReducer: SongPreviewReducer,
) : Reducer<MediaPreviewState, MediaPreviewAction> {

    override fun reduce(
        state: MediaPreviewState,
        action: MediaPreviewAction,
    ): Pair<MediaPreviewState, Effect<MediaPreviewAction>> = when (action) {
        is MediaPreviewAction.OpenPhoto ->
            state.copy(
                photo = PhotoPreviewState(item = action.item),
                song = state.song?.copy(detent = PreviewDetent.MINI),
            ) to Effect.none()

        is MediaPreviewAction.OpenSong -> {
            val songs = action.siblings.filter { it.kind == MediaKind.SONG }
            val queue = if (songs.any { it.id == action.item.id }) songs else listOf(action.item)
            state.copy(
                song = SongPreviewState(item = action.item, queue = queue),
                photo = state.photo?.copy(detent = PreviewDetent.MINI),
            ) to Effect.Run<MediaPreviewAction> { send -> send(MediaPreviewAction.Song(SongPreviewAction.Start)) }
        }

        is MediaPreviewAction.Photo -> {
            val current = state.photo
            if (current == null) {
                state to Effect.none()
            } else {
                val (nextPhoto, effect) = photoReducer.reduce(current, action.action)
                var next = state.copy(photo = if (action.action is PhotoPreviewAction.Delegate.Closed) null else nextPhoto)
                if (action.action is PhotoPreviewAction.View.ExpandTapped) {
                    // Expanding the photo drops any open song to its mini bar.
                    next = next.copy(song = next.song?.copy(detent = PreviewDetent.MINI))
                }
                next to effect.map { MediaPreviewAction.Photo(it) }
            }
        }

        is MediaPreviewAction.Song -> {
            val current = state.song
            if (current == null) {
                state to Effect.none()
            } else {
                val (nextSong, effect) = songReducer.reduce(current, action.action)
                var next = state.copy(song = if (action.action is SongPreviewAction.Delegate.Closed) null else nextSong)
                if (action.action is SongPreviewAction.View.ExpandTapped) {
                    // Expanding the song drops any open photo to its mini bar.
                    next = next.copy(photo = next.photo?.copy(detent = PreviewDetent.MINI))
                }
                next to effect.map { MediaPreviewAction.Song(it) }
            }
        }
    }
}

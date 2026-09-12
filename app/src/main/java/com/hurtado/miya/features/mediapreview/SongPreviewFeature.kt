package com.hurtado.miya.features.mediapreview

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.audio.AudioPlayerClient
import com.hurtado.miya.services.audio.AudioPlayerEvent
import com.hurtado.miya.services.audio.RemoteCommand
import com.hurtado.miya.services.audio.toAudioTrack
import javax.inject.Inject

/** How far into a track ⏮ restarts it instead of stepping back a track. */
const val RESTART_THRESHOLD_SECONDS = 3.0

/** Port of `Miya/Features/SongPreviewFeature.swift`. */
data class SongPreviewState(
    val item: HomeSectionItem,
    /** The songs `item` was opened alongside, in display order — the play queue. Always
     * contains `item`; `[item]` when opened without siblings. */
    val queue: List<HomeSectionItem> = listOf(item),
    val isPlaying: Boolean = false,
    val currentTime: Double = 0.0,
    /** The server's `duration` until Media3 reports the asset's own. */
    val duration: Double? = item.duration,
    /** True while the user drags the scrubber, so incoming progress ticks don't fight the thumb
     * back to where playback actually is. */
    val isScrubbing: Boolean = false,
    val detent: PreviewDetent = PreviewDetent.LARGE,
) {
    val nextItem: HomeSectionItem?
        get() {
            val index = queue.indexOfFirst { it.id == item.id }
            return if (index in queue.indices && index + 1 < queue.size) queue[index + 1] else null
        }

    val previousItem: HomeSectionItem?
        get() {
            val index = queue.indexOfFirst { it.id == item.id }
            return if (index > 0) queue[index - 1] else null
        }

    /** ⏮ stays enabled at the head of the queue once you're a few seconds in, because there it
     * restarts the track rather than stepping back. */
    val canGoBack: Boolean get() = previousItem != null || currentTime > RESTART_THRESHOLD_SECONDS
}

sealed interface SongPreviewAction {
    sealed interface View : SongPreviewAction {
        data object PlayPauseTapped : View
        data object PreviousTapped : View
        data object NextTapped : View
        data class ScrubChanged(val time: Double) : View
        data object ScrubEnded : View
        data object ExpandTapped : View
        data object MinimizeTapped : View // Android-only, see PhotoPreviewAction.View.MinimizeTapped
        data object CloseTapped : View
        data object ViewAlbumTapped : View
        data class AuthorTapped(val author: AuthorRef) : View
    }

    sealed interface Delegate : SongPreviewAction {
        data class ViewAlbumTapped(val albumId: String) : Delegate
        data class AuthorTapped(val author: AuthorRef) : Delegate
        /** The user dismissed this preview from its mini bar. */
        data object Closed : Delegate
    }

    /** Hand `item` to the player and start it. Sent when a song is opened, and by this reducer
     * on every queue advance. */
    data object Start : SongPreviewAction
    data class Player(val event: AudioPlayerEvent) : SongPreviewAction
}

class SongPreviewReducer @Inject constructor(
    private val audioPlayer: AudioPlayerClient,
) : Reducer<SongPreviewState, SongPreviewAction> {

    private enum class CancelId { PlayerEvents, PlayerLoad, Seek }

    override fun reduce(
        state: SongPreviewState,
        action: SongPreviewAction,
    ): Pair<SongPreviewState, Effect<SongPreviewAction>> = when (action) {
        is SongPreviewAction.Start -> {
            val track = state.item.toAudioTrack(
                canGoPrevious = state.previousItem != null,
                canGoNext = state.nextItem != null,
            )
            // Optimistic — `.player(.statusChanged)` is the correction.
            val next = state.copy(isPlaying = true, currentTime = 0.0, duration = state.item.duration)
            next to Effect.merge(
                Effect.Run(id = CancelId.PlayerEvents, cancelInFlight = true) { send ->
                    audioPlayer.events().collect { event -> send(SongPreviewAction.Player(event)) }
                },
                Effect.Run<SongPreviewAction>(id = CancelId.PlayerLoad, cancelInFlight = true) { audioPlayer.load(track) },
            )
        }

        is SongPreviewAction.View.PlayPauseTapped -> {
            val wasPlaying = state.isPlaying
            state.copy(isPlaying = !wasPlaying) to Effect.Run<SongPreviewAction> {
                if (wasPlaying) audioPlayer.pause() else audioPlayer.play()
            }
        }

        is SongPreviewAction.View.NextTapped -> {
            val next = state.nextItem
            if (next == null) {
                state to Effect.none()
            } else {
                state.copy(item = next) to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.Start) }
            }
        }

        is SongPreviewAction.View.PreviousTapped -> {
            val previous = state.previousItem
            if (state.currentTime <= RESTART_THRESHOLD_SECONDS && previous != null) {
                state.copy(item = previous) to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.Start) }
            } else {
                state.copy(currentTime = 0.0) to Effect.Run<SongPreviewAction> { audioPlayer.seek(0.0) }
            }
        }

        is SongPreviewAction.View.ScrubChanged ->
            state.copy(isScrubbing = true, currentTime = action.time) to Effect.none()

        is SongPreviewAction.View.ScrubEnded ->
            state.copy(isScrubbing = false) to Effect.Run<SongPreviewAction>(id = CancelId.Seek, cancelInFlight = true) {
                audioPlayer.seek(state.currentTime)
            }

        is SongPreviewAction.View.ExpandTapped ->
            state.copy(detent = PreviewDetent.LARGE) to Effect.none()

        is SongPreviewAction.View.MinimizeTapped ->
            state.copy(detent = PreviewDetent.MINI) to Effect.none()

        is SongPreviewAction.View.CloseTapped ->
            // Stop before closing: `.closed` makes the parent nil this state, which would cancel
            // this same effect if the stop were sequenced after — same ordering note as iOS.
            state to Effect.Run<SongPreviewAction> { send ->
                audioPlayer.stop()
                send(SongPreviewAction.Delegate.Closed)
            }

        is SongPreviewAction.View.ViewAlbumTapped -> {
            val albumId = state.item.albumID
            if (albumId == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.Delegate.ViewAlbumTapped(albumId)) }
            }
        }

        is SongPreviewAction.View.AuthorTapped ->
            state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.Delegate.AuthorTapped(action.author)) }

        is SongPreviewAction.Player -> reducePlayerEvent(state, action.event)

        is SongPreviewAction.Delegate -> state to Effect.none()
    }

    private fun reducePlayerEvent(
        state: SongPreviewState,
        event: AudioPlayerEvent,
    ): Pair<SongPreviewState, Effect<SongPreviewAction>> = when (event) {
        is AudioPlayerEvent.StatusChanged ->
            state.copy(isPlaying = event.isPlaying) to Effect.none()

        is AudioPlayerEvent.Progress -> {
            var next = state
            if (event.duration != null) next = next.copy(duration = event.duration)
            if (!next.isScrubbing) next = next.copy(currentTime = event.time)
            next to Effect.none()
        }

        is AudioPlayerEvent.Finished -> {
            val next = state.nextItem
            if (next == null) {
                state.copy(isPlaying = false, currentTime = 0.0) to Effect.Run<SongPreviewAction> { audioPlayer.seek(0.0) }
            } else {
                state.copy(item = next) to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.Start) }
            }
        }

        is AudioPlayerEvent.Failed -> state.copy(isPlaying = false) to Effect.none()

        is AudioPlayerEvent.Remote -> when (val command = event.command) {
            RemoteCommand.Toggle ->
                state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.View.PlayPauseTapped) }
            RemoteCommand.Play ->
                if (state.isPlaying) state to Effect.none()
                else state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.View.PlayPauseTapped) }
            RemoteCommand.Pause ->
                if (!state.isPlaying) state to Effect.none()
                else state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.View.PlayPauseTapped) }
            RemoteCommand.Next ->
                state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.View.NextTapped) }
            RemoteCommand.Previous ->
                state to Effect.Run<SongPreviewAction> { send -> send(SongPreviewAction.View.PreviousTapped) }
            is RemoteCommand.Seek ->
                state.copy(currentTime = command.time) to Effect.Run<SongPreviewAction> { audioPlayer.seek(command.time) }
        }
    }
}

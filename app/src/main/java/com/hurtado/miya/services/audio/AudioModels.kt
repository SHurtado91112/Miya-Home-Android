package com.hurtado.miya.services.audio

import com.hurtado.miya.features.home.HomeSectionItem

/**
 * One item of playable audio, flattened out of [HomeSectionItem] so the audio layer doesn't
 * depend on the app's media model. Mirrors `AudioTrack` in `Services/AudioPlayerClient.swift`.
 * [canGoPrevious]/[canGoNext] would drive a lock-screen transport's enabled state; Android's
 * default `MediaSession` notification derives that from the player's own single-item state
 * instead (see [Media3AudioPlayerClient] doc), so they're kept here for parity/future use but
 * not currently threaded into the session.
 */
data class AudioTrack(
    val id: String,
    val url: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val duration: Double?,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
)

/** Mirrors `AudioPlayerEvent`: everything the player reports back for a reducer to act on. */
sealed interface AudioPlayerEvent {
    data class StatusChanged(val isPlaying: Boolean) : AudioPlayerEvent
    data class Progress(val time: Double, val duration: Double?) : AudioPlayerEvent
    data object Finished : AudioPlayerEvent
    data class Failed(val message: String) : AudioPlayerEvent
    data class Remote(val command: RemoteCommand) : AudioPlayerEvent
}

sealed interface RemoteCommand {
    data object Play : RemoteCommand
    data object Pause : RemoteCommand
    data object Toggle : RemoteCommand
    data object Next : RemoteCommand
    data object Previous : RemoteCommand
    data class Seek(val time: Double) : RemoteCommand
}

/** Mirrors `AudioTrack.init?(item:canGoPrevious:canGoNext:)`. Always succeeds on Android since
 * [TestAudio] guarantees a fallback — the fixture JSON has no `audioURL` at all. */
fun HomeSectionItem.toAudioTrack(canGoPrevious: Boolean, canGoNext: Boolean): AudioTrack = AudioTrack(
    id = id,
    url = audioURL ?: TestAudio.assetUri(id),
    title = title,
    artist = author?.name ?: subtitle,
    artworkUrl = smallImageURL,
    duration = duration,
    canGoPrevious = canGoPrevious,
    canGoNext = canGoNext,
)

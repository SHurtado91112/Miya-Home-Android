package com.hurtado.miya.services.audio

import kotlinx.coroutines.flow.Flow

/**
 * The playback dependency, mirrors `AudioPlayerClient.swift`. The reducer, not this client, is
 * the source of truth for transport state — everything the player learns on its own (a periodic
 * time update, the end of a track, a route change) comes back through [events] as a fact for
 * `SongPreviewFeature` to act on, rather than being applied here.
 */
interface AudioPlayerClient {
    /** Hand the player a track, publish now-playing metadata, and start playing. */
    suspend fun load(track: AudioTrack)
    suspend fun play()
    suspend fun pause()
    suspend fun seek(time: Double)
    /** Pause, clear now-playing, and let the notification/session go away. */
    suspend fun stop()

    /** Everything the player reports back. Replays the current playing status immediately, so a
     * fresh subscriber isn't blind until the next tick — mirrors the `AsyncStream` behavior on
     * iOS. */
    fun events(): Flow<AudioPlayerEvent>
}

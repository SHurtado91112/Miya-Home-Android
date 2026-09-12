package com.hurtado.miya.services.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Media3 machinery behind [AudioPlayerClient], mirrors `AudioPlayerEngine.swift`. Owns the
 * app-wide singleton [ExoPlayer] (provided by `di/AudioModule.kt`) that [MiyaPlaybackService]
 * wraps in a `MediaSession` for lock-screen/notification controls — both talk to the same player
 * instance, so a system play/pause surfaces here via [Player.Listener] exactly like a UI tap
 * would.
 *
 * Unlike iOS, next/previous aren't wired into the system notification's transport: this player
 * always holds a single `MediaItem` (queue navigation is the reducer's job, not ExoPlayer's own
 * playlist), so Media3's default session UI simply shows those buttons disabled. Acceptable for
 * now — the in-app transport controls are unaffected — revisit if lock-screen skip becomes a
 * requirement (would need a custom `MediaSession.Callback` forwarding to `RemoteCommand`).
 */
@Singleton
class Media3AudioPlayerClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
) : AudioPlayerClient {

    private val isPlayingState = MutableStateFlow(false)
    private val oneShotEvents = MutableSharedFlow<AudioPlayerEvent>(extraBufferCapacity = 16)
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var currentTrack: AudioTrack? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    oneShotEvents.tryEmit(AudioPlayerEvent.Finished)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                oneShotEvents.tryEmit(AudioPlayerEvent.Failed(error.message ?: "The track could not be played."))
            }
        })
    }

    override suspend fun load(track: AudioTrack) {
        currentTrack = track
        // Starts the foreground service so playback (and its notification) survive the app
        // backgrounding — the Android analogue of activating the AVAudioSession + relying on
        // UIBackgroundModes=audio.
        ContextCompat.startForegroundService(context, Intent(context, MiyaPlaybackService::class.java))

        val mediaItem = MediaItem.Builder()
            .setUri(track.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .apply { track.artworkUrl?.let { setArtworkUri(Uri.parse(it)) } }
                    .build(),
            )
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        startProgressTicker()
    }

    override suspend fun play() {
        player.play()
    }

    override suspend fun pause() {
        player.pause()
    }

    override suspend fun seek(time: Double) {
        player.seekTo((time * 1000).toLong())
    }

    override suspend fun stop() {
        progressJob?.cancel()
        player.stop()
        player.clearMediaItems()
        currentTrack = null
        oneShotEvents.tryEmit(AudioPlayerEvent.StatusChanged(isPlaying = false))
    }

    override fun events(): Flow<AudioPlayerEvent> =
        merge(isPlayingState.map { AudioPlayerEvent.StatusChanged(it) }, oneShotEvents)

    /** No `addPeriodicTimeObserver` equivalent on Media3's `Player`, so poll at the same 0.5s
     * cadence iOS uses. */
    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = engineScope.launch {
            while (isActive) {
                val durationMs = player.duration
                val duration = if (durationMs > 0 && durationMs != C.TIME_UNSET) {
                    durationMs / 1000.0
                } else {
                    currentTrack?.duration
                }
                oneShotEvents.tryEmit(AudioPlayerEvent.Progress(time = player.currentPosition / 1000.0, duration = duration))
                kotlinx.coroutines.delay(500)
            }
        }
    }
}

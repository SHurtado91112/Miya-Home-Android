package com.hurtado.miya.services.audio

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground media session service — the Android analogue of `AVPlayer` +
 * `MPNowPlayingInfoCenter` + `MPRemoteCommandCenter` + the `audio` `UIBackgroundMode`. Stage 5
 * wires this to `AudioPlayerEngine`/`SongPreviewFeature` (queue management, the 3s-restart rule,
 * auto-advance, lock-screen/notification transport controls). Present now only so the
 * `<service>` declared in AndroidManifest.xml resolves to a real class.
 */
@UnstableApi
class MiyaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

package com.hurtado.miya.services.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val NOTIFICATION_CHANNEL_ID = "miya_playback"
private const val NOTIFICATION_ID = 1

/**
 * Foreground media session service — the Android analogue of `AVPlayer` +
 * `MPNowPlayingInfoCenter` + `MPRemoteCommandCenter` + the `audio` `UIBackgroundMode`. Wraps the
 * same app-wide [ExoPlayer] instance [Media3AudioPlayerClient] drives (see `di/AudioModule.kt`),
 * so system controls (notification, lock screen, Bluetooth) and in-app taps operate on one
 * shared player and stay in sync automatically.
 *
 * [onCreate] calls [startForeground] immediately with a minimal placeholder notification. This
 * is required: `Media3AudioPlayerClient.load` starts this service via
 * `ContextCompat.startForegroundService`, which gives the service ~5s to call
 * [startForeground] itself or the system kills the whole process with a
 * `ForegroundServiceDidNotStartInTimeException` — a crash caught during on-device testing (it
 * surfaced as every track change eventually killing the app a few seconds later). Media3's own
 * `MediaSession` notification machinery takes over updating this notification's actual content
 * (title/artwork/transport) once the session exists; this placeholder only satisfies the
 * immediate contract.
 */
@UnstableApi
@AndroidEntryPoint
class MiyaPlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
        startForeground(NOTIFICATION_ID, buildPlaceholderNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        // Deliberately not releasing `player` here — it's an app-wide singleton the reducer side
        // may still hold a reference to; only the session/service wrapper goes away.
        super.onDestroy()
    }

    private fun buildPlaceholderNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Playback",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Miya")
            .setContentText("Loading playback…")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
}

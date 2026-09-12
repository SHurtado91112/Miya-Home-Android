package com.hurtado.miya.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.hurtado.miya.services.audio.AudioPlayerClient
import com.hurtado.miya.services.audio.Media3AudioPlayerClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the app-wide singleton [ExoPlayer] that both [Media3AudioPlayerClient] (the reducer
 * side, via [AudioPlayerClient]) and `MiyaPlaybackService` (the system session/notification side)
 * share — one player, two consumers, same as iOS's single `AudioPlayerEngine.shared` instance.
 */
@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()

    @Provides
    @Singleton
    fun provideAudioPlayerClient(client: Media3AudioPlayerClient): AudioPlayerClient = client
}

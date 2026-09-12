package com.hurtado.miya.di

import com.hurtado.miya.BuildConfig
import com.hurtado.miya.services.auth.AuthClient
import com.hurtado.miya.services.auth.FixtureAuthClient
import com.hurtado.miya.services.auth.LiveAuthClient
import com.hurtado.miya.services.auth.MiyaAuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Binds [AuthClient] to the fixture implementation whenever no server is configured — mirrors
 * iOS `RunMode.isFixtureMode` exactly (keyed on `MIYA_SERVER_URL` alone; a missing Google client
 * id is instead checked inside [LiveAuthClient.signInWithGoogle] itself, throwing
 * [com.hurtado.miya.services.auth.AuthError.NotConfigured] the same way iOS's `signInWithGoogle`
 * does) — the same rule `di/ClientsModule.kt` follows for [com.hurtado.miya.services.HomeClient].
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideMiyaAuthApi(client: OkHttpClient): MiyaAuthApi =
        MiyaAuthApi(baseUrl = BuildConfig.MIYA_SERVER_URL, client = client)

    @Provides
    @Singleton
    fun provideAuthClient(fixture: FixtureAuthClient, live: LiveAuthClient): AuthClient =
        if (BuildConfig.MIYA_SERVER_URL.isNotBlank()) live else fixture
}

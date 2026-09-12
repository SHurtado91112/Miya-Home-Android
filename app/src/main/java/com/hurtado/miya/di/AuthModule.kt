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
 * Binds [AuthClient] to the fixture implementation whenever no server or Google OAuth client id
 * is configured — mirrors iOS `RunMode.isFixtureMode` and the same rule `di/ClientsModule.kt`
 * follows for [com.hurtado.miya.services.HomeClient]. With real values configured (a reachable
 * MIYA_SERVER_URL and a Google Cloud-registered Android OAuth client id), flips to
 * [LiveAuthClient] automatically.
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
    fun provideAuthClient(fixture: FixtureAuthClient, live: LiveAuthClient): AuthClient {
        val isConfigured = BuildConfig.MIYA_SERVER_URL.isNotBlank() &&
            BuildConfig.MIYA_GOOGLE_CLIENT_ID.isNotBlank()
        return if (isConfigured) live else fixture
    }
}

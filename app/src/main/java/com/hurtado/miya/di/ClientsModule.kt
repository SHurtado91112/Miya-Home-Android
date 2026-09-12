package com.hurtado.miya.di

import com.hurtado.miya.BuildConfig
import com.hurtado.miya.services.HomeClient
import com.hurtado.miya.services.auth.AuthClient
import com.hurtado.miya.services.fixture.FixtureHomeClient
import com.hurtado.miya.services.graphql.LiveHomeClient
import com.hurtado.miya.services.graphql.MiyaGraphQlClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Binds [HomeClient] to the fixture implementation, mirroring iOS `RunMode.isFixtureMode`: no
 * `MIYA_SERVER_URL` configured -> bundled JSON. With one configured, binds [LiveHomeClient]
 * instead — see its doc and `services/graphql/MiyaGraphQlClient.kt`'s for the "unverified
 * against a live server" caveat (this build environment has no route to test against one).
 */
@Module
@InstallIn(SingletonComponent::class)
object ClientsModule {

    @Provides
    @Singleton
    fun provideMiyaGraphQlClient(client: OkHttpClient, authClient: AuthClient): MiyaGraphQlClient =
        MiyaGraphQlClient(baseUrl = BuildConfig.MIYA_SERVER_URL, client = client, authClient = authClient)

    @Provides
    @Singleton
    fun provideHomeClient(fixture: FixtureHomeClient, live: LiveHomeClient): HomeClient =
        if (BuildConfig.MIYA_SERVER_URL.isNotBlank()) live else fixture
}

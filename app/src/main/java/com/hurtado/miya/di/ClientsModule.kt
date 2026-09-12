package com.hurtado.miya.di

import com.hurtado.miya.BuildConfig
import com.hurtado.miya.services.HomeClient
import com.hurtado.miya.services.fixture.FixtureHomeClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [HomeClient] to the fixture implementation, mirroring iOS `RunMode.isFixtureMode`: no
 * `MIYA_SERVER_URL` configured -> bundled JSON. Stage 7 adds a `LiveHomeClient` over Apollo
 * Kotlin and switches this binding based on [BuildConfig.MIYA_SERVER_URL].
 */
@Module
@InstallIn(SingletonComponent::class)
object ClientsModule {

    @Provides
    @Singleton
    fun provideHomeClient(fixture: FixtureHomeClient): HomeClient {
        // TODO(Stage 7): return LiveHomeClient(...) when BuildConfig.MIYA_SERVER_URL.isNotEmpty()
        return fixture
    }
}

package com.hurtado.miya.services.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Always signed in as a stand-in user, with no tokens — port of iOS's `AuthClient.fixture`.
 * Bound whenever no server URL or Google client id is configured (see `di/AuthModule.kt`), the
 * same rule `HomeClient`'s fixture/live split follows.
 */
@Singleton
class FixtureAuthClient @Inject constructor() : AuthClient {
    override suspend fun signInWithGoogle(): UserProfile = UserProfile.fixture
    override suspend fun restore(): UserProfile? = UserProfile.fixture
    override suspend fun accessToken(): String? = null
    override suspend fun invalidateAccessToken() = Unit
    override suspend fun signOut() = Unit
    override fun sessionInvalidated(): Flow<Unit> = emptyFlow()
}

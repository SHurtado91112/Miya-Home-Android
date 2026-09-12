package com.hurtado.miya.services.auth

import com.hurtado.miya.BuildConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Port of the non-fixture branch of iOS `AuthClient.liveValue`: wires [SessionStore] up to real
 * Google sign-in. Requires [BuildConfig.MIYA_GOOGLE_CLIENT_ID] to be set (a real Android OAuth
 * client id registered in Google Cloud Console for this applicationId/signing certificate) —
 * without one, [signInWithGoogle] throws [AuthError.NotConfigured], matching iOS's check for a
 * missing `MiyaGoogleClientID`/`MIYA_SERVER_URL`.
 */
@Singleton
class LiveAuthClient @Inject constructor(
    private val store: SessionStore,
) : AuthClient {

    override suspend fun signInWithGoogle(): UserProfile {
        val clientId = BuildConfig.MIYA_GOOGLE_CLIENT_ID.takeIf { it.isNotBlank() }
            ?: throw AuthError.NotConfigured
        return store.signIn(clientId, BuildConfig.MIYA_OAUTH_REDIRECT_URI)
    }

    override suspend fun restore(): UserProfile? = store.restore()

    override suspend fun accessToken(): String? = store.validAccessToken()

    override suspend fun invalidateAccessToken() = store.invalidateAccessToken()

    override suspend fun signOut() = store.signOut()

    override fun sessionInvalidated(): Flow<Unit> = store.invalidationEvents
}

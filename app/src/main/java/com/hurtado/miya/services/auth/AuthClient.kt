package com.hurtado.miya.services.auth

import kotlinx.coroutines.flow.Flow

/**
 * The only auth seam features touch, port of `AuthClient.swift`. Every method deals in
 * [UserProfile], never [Session] — tokens must never cross into feature state or actions.
 */
interface AuthClient {
    /** Runs the Google consent flow and establishes a session. */
    suspend fun signInWithGoogle(): UserProfile

    /** The persisted session at launch, revalidated against the server. `null` when signed out. */
    suspend fun restore(): UserProfile?

    /** Bearer token for the next request, refreshed if needed. `null` in fixture mode, where
     * there is no server to authenticate to. */
    suspend fun accessToken(): String?

    /** Called after a 401 so the next request refreshes rather than resending a token the
     * server just rejected. */
    suspend fun invalidateAccessToken()

    /** Revokes server-side where possible, then clears local state. */
    suspend fun signOut()

    /** Fires when the session dies outside a user action, so the root gate can drop back to the
     * sign-in wall. */
    fun sessionInvalidated(): Flow<Unit>
}

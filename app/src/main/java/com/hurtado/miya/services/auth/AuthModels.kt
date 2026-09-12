package com.hurtado.miya.services.auth

import kotlinx.serialization.Serializable

/** The non-secret half of a signed-in identity. This is the only part that may travel through
 * feature state — port of `Session.swift`'s `UserProfile`. */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
) {
    val initials: String
        get() {
            val source = displayName?.takeIf { it.isNotBlank() } ?: email
            return source.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
        }

    companion object {
        /** Always signed in as a stand-in user, with no tokens — mirrors iOS `UserProfile.fixture`. */
        val fixture = UserProfile(
            id = "fixture-user",
            email = "listener@miya.app",
            displayName = "Miya Listener",
        )
    }
}

/** Credentials. Lives only in [SessionStore] (backed by EncryptedSharedPreferences) — never in
 * feature state or an action, mirrors `Session.swift`. */
@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String,
    /** Epoch millis, mirrors `accessTokenExpiresAt: Date`. */
    val accessTokenExpiresAtEpochMs: Long,
    val user: UserProfile,
) {
    /** Treat a token as spent slightly before it truly expires, so one that dies in flight
     * doesn't produce an avoidable 401. */
    fun isFresh(nowEpochMs: Long = System.currentTimeMillis(), skewMs: Long = 60_000L): Boolean =
        accessTokenExpiresAtEpochMs - nowEpochMs > skewMs
}

sealed class AuthError(message: String? = null) : Exception(message) {
    /** The user dismissed the Google tab. Not a failure — never alert. */
    data object Canceled : AuthError()

    /** The session is genuinely dead; sign out. */
    data object SessionExpired : AuthError("Your session has expired. Please sign in again.")

    /** No client id configured, or no server configured. */
    data object NotConfigured : AuthError("Miya isn't configured for sign-in yet.")

    /** The server refused, with a message safe to show. */
    data class Server(val serverMessage: String) : AuthError(serverMessage)

    /** The network is unreachable. Keep the session and let the user retry. */
    data class Transport(val detail: String) : AuthError("Couldn't reach Miya. $detail")
}

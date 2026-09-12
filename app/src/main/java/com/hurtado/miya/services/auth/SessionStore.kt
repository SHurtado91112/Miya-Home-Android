package com.hurtado.miya.services.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the signed-in session: the in-memory copy, its encrypted-storage backing, and the refresh
 * lifecycle. Port of the `SessionStore` actor (`SessionStore.swift`) — a `Mutex`-guarded class
 * standing in for Swift's actor isolation, since Kotlin has no native actor model.
 */
@Singleton
class SessionStore @Inject constructor(
    private val keyStore: SessionKeyStore,
    private val api: MiyaAuthApi,
    private val oauth: GoogleOAuthLauncher,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var session: Session? = null

    /** The in-flight refresh, if any, together with the token it presented — mirrors
     * `refreshTask`/`refreshingToken`. */
    private var refreshDeferred: Deferred<Session>? = null
    private var refreshingToken: String? = null

    private val _invalidationEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val invalidationEvents: SharedFlow<Unit> = _invalidationEvents.asSharedFlow()

    // MARK: - Sign in

    suspend fun signIn(clientId: String, redirectUri: String): UserProfile = mutex.withLock {
        val authorization = oauth.authorize(clientId, redirectUri)
        val newSession = api.signInWithGoogle(
            code = authorization.code,
            codeVerifier = authorization.codeVerifier,
            redirectUri = authorization.redirectUri,
            nonce = authorization.nonce,
        )
        persist(newSession)
        newSession.user
    }

    // MARK: - Restore

    /** Reads local storage and confirms the token still works. Returns `null` only when there is
     * genuinely no usable session — a network failure leaves the stored session in place and
     * reports the cached profile, so launching offline doesn't sign the user out. */
    suspend fun restore(): UserProfile? {
        val stored = loadFromStore() ?: return null
        session = stored

        return try {
            val token = validAccessToken() ?: return null
            val profile = api.viewer(token)
            if (profile != null) {
                session = session?.copy(user = profile)
                session?.let { persist(it) }
                profile
            } else {
                clear()
                null
            }
        } catch (e: AuthError.SessionExpired) {
            clear()
            null
        } catch (e: AuthError) {
            // Offline or server error: keep what we have and let the user in on cached identity.
            stored.user
        }
    }

    // MARK: - Tokens

    /** A usable bearer token, refreshing first if the current one is spent. */
    suspend fun validAccessToken(): String? {
        val current = session ?: return null
        if (current.isFresh()) return current.accessToken
        return refresh(staleToken = current.refreshToken).accessToken
    }

    /** Marks the access token spent after the server rejected it, so the next caller refreshes
     * instead of re-sending a token known to be bad. */
    fun invalidateAccessToken() {
        session = session?.copy(accessTokenExpiresAtEpochMs = 0L)
    }

    /**
     * Single-flight refresh. Home issues several loads concurrently on appear, so two callers
     * routinely find the same expired token at the same instant; refresh tokens are single-use
     * and rotated server-side, so letting both redeem would spend the token twice. Callers join
     * the existing task rather than starting a second.
     */
    private suspend fun refresh(staleToken: String): Session {
        val existing = mutex.withLock {
            if (refreshDeferred != null && refreshingToken == staleToken) {
                refreshDeferred
            } else if (refreshingToken != null && refreshingToken != staleToken && session?.isFresh() == true) {
                // Someone already rotated past this token while we were waiting.
                return session!!
            } else {
                null
            }
        }
        if (existing != null) return existing.await()

        val deferred = scope.async {
            try {
                api.refreshSession(staleToken)
            } catch (e: AuthError.Transport) {
                throw e
            } catch (e: AuthError) {
                throw AuthError.SessionExpired
            }
        }
        mutex.withLock {
            refreshDeferred = deferred
            refreshingToken = staleToken
        }

        try {
            val refreshed = deferred.await()
            persist(refreshed)
            return refreshed
        } catch (e: AuthError.SessionExpired) {
            clear()
            _invalidationEvents.tryEmit(Unit)
            throw e
        } finally {
            mutex.withLock {
                refreshDeferred = null
                refreshingToken = null
            }
        }
    }

    // MARK: - Sign out

    suspend fun signOut() {
        val refreshToken = session?.refreshToken
        if (refreshToken != null) {
            // Best effort: local sign-out must succeed even if the server is unreachable.
            try {
                api.signOut(refreshToken)
            } catch (e: Exception) {
                // ignored — clear locally regardless
            }
        }
        clear()
    }

    // MARK: - Persistence

    private fun persist(newSession: Session) {
        session = newSession
        keyStore.save(json.encodeToString(newSession))
    }

    private fun loadFromStore(): Session? {
        val raw = keyStore.load() ?: return null
        return try {
            json.decodeFromString(Session.serializer(), raw)
        } catch (e: Exception) {
            null
        }
    }

    private fun clear() {
        session = null
        refreshDeferred = null
        refreshingToken = null
        keyStore.delete()
    }
}

package com.hurtado.miya.services.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/** What the redirect handed back, ready to redeem server-side. Mirrors the tuple
 * `GoogleOAuthPresenter.authorize` returns on iOS. */
data class GoogleAuthorization(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
    val nonce: String,
)

private const val AUTHORIZE_TIMEOUT_MS = 5 * 60 * 1000L

/**
 * Presents the Google consent screen via a Custom Tab and waits for the redirect. Port of
 * `GoogleOAuthPresenter` (`GoogleOAuth.swift`) — Android has no `ASWebAuthenticationSession`
 * with a completion callback, so the redirect instead arrives as a deep link into
 * [com.hurtado.miya.MainActivity] (declared in the manifest), forwarded here via
 * [OAuthRedirectBus].
 */
@Singleton
class GoogleOAuthLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val redirectBus: OAuthRedirectBus,
) {
    suspend fun authorize(clientId: String, redirectUri: String): GoogleAuthorization {
        val challenge = Pkce.generateChallenge()

        val authorizeUri = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "openid email profile")
            .appendQueryParameter("code_challenge", challenge.challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", challenge.state)
            .appendQueryParameter("nonce", challenge.nonce)
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.intent.data = authorizeUri
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(customTabsIntent.intent)

        val redirect = withTimeout(AUTHORIZE_TIMEOUT_MS) {
            redirectBus.redirects.first { it.getQueryParameter("state") == challenge.state }
        }

        // The user denied consent, or Google reported some other error.
        if (redirect.getQueryParameter("error") != null) throw AuthError.Canceled
        val code = redirect.getQueryParameter("code") ?: throw AuthError.Canceled

        return GoogleAuthorization(
            code = code,
            codeVerifier = challenge.verifier,
            redirectUri = redirectUri,
            nonce = challenge.nonce,
        )
    }
}

package com.hurtado.miya.services.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Proof Key for Code Exchange (RFC 7636) plus the two OpenID nonces the authorization request
 * needs. Port of `PKCE.swift`.
 *
 * PKCE is what makes a browser-based OAuth flow safe for an app that cannot keep a secret: the
 * app sends a hash of a random verifier up front, then proves ownership by revealing the
 * verifier when redeeming the code.
 */
object Pkce {
    /** One authorization attempt's worth of random values. */
    data class Challenge(
        /** Held on device, sent only when redeeming the code. */
        val verifier: String,
        /** Sent in the authorization URL: base64url(SHA256(verifier)). */
        val challenge: String,
        /** Echoed back on the redirect; guards against a response being swapped in from a
         * different request. */
        val state: String,
        /** Embedded in the issued id_token by Google, verified server-side. */
        val nonce: String,
    )

    fun generateChallenge(): Challenge {
        val verifier = randomUrlSafeString()
        return Challenge(
            verifier = verifier,
            challenge = s256(verifier),
            state = randomUrlSafeString(),
            nonce = randomUrlSafeString(),
        )
    }

    /** 32 bytes of CSPRNG output, base64url-encoded — the low end of RFC 7636's 43...128 range
     * for a code verifier. */
    private fun randomUrlSafeString(byteCount: Int = 32): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return base64UrlEncode(bytes)
    }

    private fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
        return base64UrlEncode(digest)
    }

    private fun base64UrlEncode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
}

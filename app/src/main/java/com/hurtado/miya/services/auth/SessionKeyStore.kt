package com.hurtado.miya.services.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE_NAME = "miya_session_store"
private const val SESSION_KEY = "session.v1"

/**
 * Encrypted local session storage, the Android analogue of `KeychainClient.swift`. Backed by
 * Jetpack Security's `EncryptedSharedPreferences` (AES256-GCM values, an AES256-SIV-wrapped
 * keyset) rather than the Keychain's `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` — the
 * closest Android equivalent for "not backed up, unusable before first unlock".
 */
@Singleton
class SessionKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(json: String) {
        prefs.edit().putString(SESSION_KEY, json).apply()
    }

    fun load(): String? = prefs.getString(SESSION_KEY, null)

    fun delete() {
        prefs.edit().remove(SESSION_KEY).apply()
    }
}

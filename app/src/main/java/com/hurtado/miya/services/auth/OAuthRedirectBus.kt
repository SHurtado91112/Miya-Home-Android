package com.hurtado.miya.services.auth

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the OAuth redirect deep link (delivered to [com.hurtado.miya.MainActivity] via the
 * `com.hurtado.miya.oauth://oauth2redirect` intent-filter, since a Custom Tab has no
 * `ActivityResult` callback of its own) back into the suspending [GoogleOAuthLauncher.authorize]
 * call waiting on it. The Android analogue of `ASWebAuthenticationSession`'s completion handler.
 */
@Singleton
class OAuthRedirectBus @Inject constructor() {
    private val _redirects = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val redirects: SharedFlow<Uri> = _redirects.asSharedFlow()

    fun emit(uri: Uri) {
        _redirects.tryEmit(uri)
    }
}

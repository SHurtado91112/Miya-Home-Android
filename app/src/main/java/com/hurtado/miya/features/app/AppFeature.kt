package com.hurtado.miya.features.app

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.services.audio.AudioPlayerClient
import com.hurtado.miya.services.auth.AuthClient
import com.hurtado.miya.services.auth.UserProfile
import javax.inject.Inject

/**
 * The root gate: shows the sign-in wall until there is a session, then Home. Port of
 * `AppFeature.swift`. iOS models this as an enum `State` so signing out *destroys*
 * `HomeFeature.State` — Android gets the same effect for free since `MiyaNavHost` (holding all
 * of Home's nav graph + the `MediaPreviewViewModel`) is only composed while [AppState.SignedIn],
 * so its whole ViewModelStore is torn down on sign-out/invalidation rather than merely hidden.
 */
sealed interface AppState {
    /** Starts here rather than flashing the sign-in button before the session read finishes. */
    data object Restoring : AppState
    data object SignedOut : AppState
    data class SignedIn(val profile: UserProfile) : AppState
}

sealed interface AppAction {
    data object Appeared : AppAction
    data class Restored(val profile: UserProfile?) : AppAction
    data object SessionInvalidated : AppAction
    data object SignOutRequested : AppAction
    /** From `SignInScreen`'s `onSignedIn` callback (the Android stand-in for observing
     * `SignInFeature.Action.Delegate.signedIn`). */
    data class SignedIn(val profile: UserProfile) : AppAction
}

class AppReducer @Inject constructor(
    private val authClient: AuthClient,
    private val audioPlayer: AudioPlayerClient,
) : Reducer<AppState, AppAction> {

    private enum class CancelId { Invalidation }

    override fun reduce(state: AppState, action: AppAction): Pair<AppState, Effect<AppAction>> = when (action) {
        is AppAction.Appeared -> state to Effect.merge(
            Effect.Run<AppAction> { send -> send(AppAction.Restored(authClient.restore())) },
            Effect.Run(id = CancelId.Invalidation, cancelInFlight = true) { send ->
                authClient.sessionInvalidated().collect { send(AppAction.SessionInvalidated) }
            },
        )

        is AppAction.Restored -> {
            val next = if (action.profile == null) AppState.SignedOut else AppState.SignedIn(action.profile)
            next to Effect.none()
        }

        is AppAction.SignedIn -> AppState.SignedIn(action.profile) to Effect.none()

        is AppAction.SessionInvalidated -> {
            if (state !is AppState.SignedIn) {
                state to Effect.none()
            } else {
                AppState.SignedOut to stopAudio()
            }
        }

        is AppAction.SignOutRequested -> AppState.SignedOut to Effect.merge(
            stopAudio(),
            Effect.Run<AppAction> { authClient.signOut() },
        )
    }

    /** iOS notes: `ifCaseLet` cancels the departing child's effects, but the audio engine is a
     * separate singleton — cancelling `SongPreviewFeature`'s subscription doesn't stop playback.
     * Without this, music keeps playing over the sign-in screen. Same story here: tearing down
     * `MiyaNavHost`'s `MediaPreviewViewModel` doesn't stop the shared `ExoPlayer`. */
    private fun stopAudio(): Effect<AppAction> = Effect.Run { audioPlayer.stop() }
}

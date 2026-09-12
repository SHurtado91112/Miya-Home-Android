package com.hurtado.miya.features.signin

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.services.auth.AuthClient
import com.hurtado.miya.services.auth.AuthError
import com.hurtado.miya.services.auth.UserProfile
import javax.inject.Inject

/**
 * The sign-in wall, port of `SignInFeature.swift`. iOS models providers as a list
 * (`Provider.allCases`) to leave room for Sign in with Apple later (App Store Guideline 4.8);
 * Android has no such requirement, so this only wires the one Google provider for now — extend
 * to a list the same way if a second provider is ever needed.
 */
data class SignInState(
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
    /** Set on a successful sign-in; the screen observes this (rather than the `Delegate` action
     * directly — the hand-rolled `Store` has no action-stream subscription) to notify the nav
     * host to switch to Home. */
    val signedInProfile: UserProfile? = null,
) {
    val isBusy: Boolean get() = isAuthenticating
}

sealed interface SignInAction {
    sealed interface View : SignInAction {
        data object GoogleTapped : View
        data object ErrorDismissed : View
    }

    sealed interface Delegate : SignInAction {
        /** Carries only the profile — tokens never enter an action. */
        data class SignedIn(val profile: UserProfile) : Delegate
    }

    data class SignInResult(val result: Result<UserProfile>) : SignInAction
}

class SignInReducer @Inject constructor(
    private val authClient: AuthClient,
) : Reducer<SignInState, SignInAction> {

    override fun reduce(
        state: SignInState,
        action: SignInAction,
    ): Pair<SignInState, Effect<SignInAction>> = when (action) {
        is SignInAction.View.GoogleTapped -> {
            if (state.isBusy) {
                state to Effect.none()
            } else {
                state.copy(isAuthenticating = true, errorMessage = null) to Effect.Run<SignInAction> { send ->
                    val result = try {
                        Result.success(authClient.signInWithGoogle())
                    } catch (t: Throwable) {
                        Result.failure(t)
                    }
                    send(SignInAction.SignInResult(result))
                }
            }
        }

        is SignInAction.SignInResult -> {
            val next = state.copy(isAuthenticating = false)
            val profile = action.result.getOrNull()
            if (profile != null) {
                next.copy(signedInProfile = profile) to
                    Effect.Run<SignInAction> { send -> send(SignInAction.Delegate.SignedIn(profile)) }
            } else {
                val error = action.result.exceptionOrNull()
                // Dismissing the Google tab is a decision, not a failure — never alert on it.
                if (error is AuthError.Canceled) {
                    next to Effect.none()
                } else {
                    next.copy(errorMessage = error?.message ?: "Something went wrong. Please try again.") to Effect.none()
                }
            }
        }

        is SignInAction.View.ErrorDismissed -> state.copy(errorMessage = null) to Effect.none()

        is SignInAction.Delegate -> state to Effect.none()
    }
}

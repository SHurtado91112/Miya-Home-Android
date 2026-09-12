package com.hurtado.miya.architecture

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "TCA-lite": a small hand-rolled MVI runtime that mirrors The Composable Architecture's
 * vocabulary (State / Action / Reducer / Effect / Store) 1:1, since the iOS app this is a port
 * of is built strictly on TCA. No third-party MVI library is used so the mapping in CLAUDE.md
 * stays literal and every port note in the Swift source has a direct Kotlin counterpart.
 *
 * Usage mirrors `@Reducer` + `Reduce { state, action in ... }`:
 *
 *   class AlbumDetailReducer @Inject constructor(...) : Reducer<AlbumDetailState, AlbumDetailAction> {
 *       override fun reduce(state: AlbumDetailState, action: AlbumDetailAction)
 *           : Pair<AlbumDetailState, Effect<AlbumDetailAction>> = when (action) {
 *           is AlbumDetailAction.View.ReachedEnd -> { ... state.copy(isLoadingMore = true) to Effect.Run(...) }
 *           ...
 *       }
 *   }
 */
interface Reducer<State, Action> {
    /**
     * Computes the next state and the [Effect] to run, mirroring
     * `reduce(into state: inout State, action: Action) -> Effect<Action>`. [State] here is
     * immutable, so this returns the *new* state rather than mutating in place.
     */
    fun reduce(state: State, action: Action): Pair<State, Effect<Action>>
}

/** Side effects a reducer can request, mirroring TCA's `Effect<Action>`. */
sealed interface Effect<out Action> {
    /** No side effect — mirrors `return .none`. */
    data object None : Effect<Nothing>

    /**
     * Runs [block] in the store's scope, mirroring `.run { send in ... }`. [id] +
     * [cancelInFlight] mirror `.cancellable(id:cancelInFlight:)`.
     */
    data class Run<Action>(
        val id: Any? = null,
        val cancelInFlight: Boolean = false,
        val block: suspend (send: suspend (Action) -> Unit) -> Unit,
    ) : Effect<Action>

    /** Cancels any in-flight [Run] effect registered under [id], mirrors `.cancel(id:)`. */
    data class Cancel(val id: Any) : Effect<Nothing>

    /** Runs several effects together, mirrors `.merge(...)`. */
    data class Merge<Action>(val effects: List<Effect<Action>>) : Effect<Action>

    companion object {
        fun <Action> none(): Effect<Action> = None
        fun <Action> merge(vararg effects: Effect<Action>): Effect<Action> = Merge(effects.toList())
    }
}

/**
 * Holds [State] as a [StateFlow] and dispatches [Action]s through a [Reducer], running any
 * returned [Effect] on [scope]. Mirrors TCA's `Store<State, Action>` minus the child-scoping
 * machinery (`scope(state:action:)`) — features here compose by exposing plain child
 * `Store`s/callbacks instead, since Kotlin has no case-path equivalent.
 *
 * [send] is expected to be called from [scope]'s dispatcher (Main, in practice, via
 * `viewModelScope`) — this store does not itself serialize concurrent mutation.
 */
class Store<State, Action>(
    initialState: State,
    private val reducer: Reducer<State, Action>,
    private val scope: CoroutineScope,
) {
    private val stateFlow = MutableStateFlow(initialState)
    val state: StateFlow<State> = stateFlow.asStateFlow()
    val currentState: State get() = stateFlow.value

    private val inFlightEffects = mutableMapOf<Any, Job>()

    fun send(action: Action) {
        val (next, effect) = reducer.reduce(stateFlow.value, action)
        stateFlow.value = next
        runEffect(effect)
    }

    private fun runEffect(effect: Effect<Action>) {
        when (effect) {
            is Effect.None -> Unit
            is Effect.Run -> {
                val job = scope.launch {
                    effect.block { a -> send(a) }
                }
                val id = effect.id
                if (id != null) {
                    if (effect.cancelInFlight) inFlightEffects[id]?.cancel()
                    inFlightEffects[id] = job
                }
            }
            is Effect.Cancel -> inFlightEffects[effect.id]?.cancel()
            is Effect.Merge -> effect.effects.forEach { runEffect(it) }
        }
    }
}

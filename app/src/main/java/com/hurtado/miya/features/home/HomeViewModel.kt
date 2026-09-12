package com.hurtado.miya.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin Hilt `ViewModel` that owns the `Store`, since a hand-rolled `Store` needs a
 * `CoroutineScope` tied to something lifecycle-aware — the closest Android has to TCA's
 * `@State var store = Store(...)` held by a SwiftUI `View`. The `ViewModel` here is *only* a
 * scope + DI holder; all real logic still lives in [HomeReducer], not in this class, per
 * CLAUDE.md's "no god-object ViewModels" rule.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    reducer: HomeReducer,
) : ViewModel() {
    val store = Store(
        initialState = HomeState(),
        reducer = reducer,
        scope = viewModelScope,
    )
}

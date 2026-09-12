package com.hurtado.miya.features.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    reducer: AppReducer,
) : ViewModel() {
    val store = Store(
        initialState = AppState.Restoring,
        reducer = reducer,
        scope = viewModelScope,
    )
}

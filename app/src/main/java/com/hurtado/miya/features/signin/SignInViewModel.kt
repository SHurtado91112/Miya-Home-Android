package com.hurtado.miya.features.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    reducer: SignInReducer,
) : ViewModel() {
    val store = Store(
        initialState = SignInState(),
        reducer = reducer,
        scope = viewModelScope,
    )
}

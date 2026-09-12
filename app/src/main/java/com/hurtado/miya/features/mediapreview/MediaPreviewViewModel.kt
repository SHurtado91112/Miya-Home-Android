package com.hurtado.miya.features.mediapreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Owns the [MediaPreviewFeature] store at Activity scope — obtained via `hiltViewModel()` called
 * directly in [com.hurtado.miya.MiyaNavHost] (outside any `composable {}` block), so it survives
 * pushing/popping the nav back stack the same way iOS's `@Presents var preview` on
 * `HomeFeature.State` outlives the pushed detail screens underneath its sheet.
 */
@HiltViewModel
class MediaPreviewViewModel @Inject constructor(
    reducer: MediaPreviewReducer,
) : ViewModel() {
    val store = Store(
        initialState = MediaPreviewState(),
        reducer = reducer,
        scope = viewModelScope,
    )
}

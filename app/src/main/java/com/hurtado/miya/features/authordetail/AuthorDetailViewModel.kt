package com.hurtado.miya.features.authordetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import com.hurtado.miya.features.home.AuthorRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Unlike Album/SectionDetail, no lookup round-trip is needed: the [AuthorRef] is already fully
 * known at the point a user taps an author row/link, so its id + name travel as nav args
 * directly (mirrors iOS passing the full `AuthorRef` value at push time).
 */
@HiltViewModel
class AuthorDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    reducer: AuthorDetailReducer,
) : ViewModel() {

    val store = Store(
        initialState = AuthorDetailState(
            author = AuthorRef(
                id = checkNotNull(savedStateHandle["authorId"]),
                name = checkNotNull(savedStateHandle["authorName"]),
            ),
        ),
        reducer = reducer,
        scope = viewModelScope,
    )
}

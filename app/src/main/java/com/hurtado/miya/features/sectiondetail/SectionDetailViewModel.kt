package com.hurtado.miya.features.sectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtado.miya.architecture.Store
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.services.HomeClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the [HomeSection] by id from [HomeClient.loadSections] (mirrors the object already
 * being resident in `HomeFeature.State` on iOS — Android's nav args are plain strings, so a
 * lookup replaces that direct object pass; cheap in fixture mode, a candidate to optimize with a
 * targeted query once Stage 7's live GraphQL client exists).
 */
@HiltViewModel
class SectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reducer: SectionDetailReducer,
    private val homeClient: HomeClient,
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])
    private val autoFocusSearch: Boolean = savedStateHandle["autoFocusSearch"] ?: false

    val store = Store(
        initialState = SectionDetailState(
            section = HomeSection(id = sectionId, title = "", items = emptyList()),
            autoFocusSearch = autoFocusSearch,
            isLoadingSection = true,
        ),
        reducer = reducer,
        scope = viewModelScope,
    )

    init {
        viewModelScope.launch {
            val section = homeClient.loadSections().firstOrNull { it.id == sectionId }
            if (section != null) {
                store.send(SectionDetailAction.SectionLoaded(section))
            }
        }
    }
}

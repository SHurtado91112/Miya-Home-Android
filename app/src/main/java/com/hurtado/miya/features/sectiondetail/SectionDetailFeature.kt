package com.hurtado.miya.features.sectiondetail

import com.hurtado.miya.architecture.Effect
import com.hurtado.miya.architecture.Reducer
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.HomeClient
import com.hurtado.miya.services.SearchResults
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Port of `Miya/Features/SectionDetailFeature.swift`: a section's full item list plus
 * server-backed search with a 300ms debounce and cursor pagination.
 */
private val SEARCH_DEBOUNCE_MS = 300L

data class SectionDetailState(
    val section: HomeSection,
    val query: String = "",
    /** Set when reached via the section's search bar, so the screen focuses the field once. */
    val autoFocusSearch: Boolean = false,
    val searchEntries: List<HomeSectionItem> = emptyList(),
    val searchAuthors: List<AuthorRef> = emptyList(),
    val searchCursor: String? = null,
    val searchHasMore: Boolean = false,
    val isLoadingSearch: Boolean = false,
    val isLoadingSection: Boolean = false,
) {
    val isSearching: Boolean get() = query.trim().isNotEmpty()

    /** What the grid renders: server search results while searching, else the section's items. */
    val displayedItems: List<HomeSectionItem> get() = if (isSearching) searchEntries else section.items

    val showsEmptyState: Boolean
        get() = isSearching && !isLoadingSearch && searchEntries.isEmpty() && searchAuthors.isEmpty()
}

sealed interface SectionDetailAction {
    sealed interface View : SectionDetailAction {
        data class QueryChanged(val query: String) : View
        data class ItemTapped(val itemId: String) : View
        data class AuthorTapped(val author: AuthorRef) : View
        data object ReachedEnd : View
        data object FocusConsumed : View
    }

    sealed interface Delegate : SectionDetailAction {
        data class ItemTapped(val item: HomeSectionItem) : Delegate
        data class AuthorTapped(val author: AuthorRef) : Delegate
    }

    /** Resolves the section by id once loaded (see [com.hurtado.miya.features.sectiondetail.SectionDetailViewModel]). */
    data class SectionLoaded(val section: HomeSection) : SectionDetailAction
    data class SearchResponse(val results: SearchResults, val reset: Boolean) : SectionDetailAction
    data object SearchFailed : SectionDetailAction
}

class SectionDetailReducer @Inject constructor(
    private val homeClient: HomeClient,
) : Reducer<SectionDetailState, SectionDetailAction> {

    private enum class CancelId { Search }

    override fun reduce(
        state: SectionDetailState,
        action: SectionDetailAction,
    ): Pair<SectionDetailState, Effect<SectionDetailAction>> = when (action) {
        is SectionDetailAction.SectionLoaded ->
            state.copy(section = action.section, isLoadingSection = false) to Effect.none()

        is SectionDetailAction.View.QueryChanged -> {
            val next = state.copy(query = action.query)
            if (!next.isSearching) {
                next.copy(
                    searchEntries = emptyList(),
                    searchAuthors = emptyList(),
                    searchCursor = null,
                    searchHasMore = false,
                    isLoadingSearch = false,
                ) to Effect.Cancel(CancelId.Search)
            } else {
                next.copy(isLoadingSearch = true) to
                    searchEffect(next.section.id, next.query, reset = true, cursor = null)
            }
        }

        is SectionDetailAction.View.ReachedEnd -> {
            if (!state.isSearching || state.isLoadingSearch || !state.searchHasMore || state.searchCursor == null) {
                state to Effect.none()
            } else {
                state.copy(isLoadingSearch = true) to
                    searchEffect(state.section.id, state.query, reset = false, cursor = state.searchCursor)
            }
        }

        is SectionDetailAction.SearchResponse -> {
            val merged = if (action.reset) {
                action.results.entries.elements
            } else {
                state.searchEntries + action.results.entries.elements.filter { fresh ->
                    state.searchEntries.none { it.id == fresh.id }
                }
            }
            state.copy(
                isLoadingSearch = false,
                searchEntries = merged,
                searchAuthors = action.results.authors,
                searchCursor = action.results.entries.cursor,
                searchHasMore = action.results.entries.hasMore,
            ) to Effect.none()
        }

        is SectionDetailAction.SearchFailed ->
            state.copy(isLoadingSearch = false, searchHasMore = false) to Effect.none()

        is SectionDetailAction.View.ItemTapped -> {
            val source = if (state.isSearching) state.searchEntries else state.section.items
            val item = source.firstOrNull { it.id == action.itemId }
            if (item == null) {
                state to Effect.none()
            } else {
                state to Effect.Run<SectionDetailAction> { send -> send(SectionDetailAction.Delegate.ItemTapped(item)) }
            }
        }

        is SectionDetailAction.View.AuthorTapped ->
            state to Effect.Run<SectionDetailAction> { send -> send(SectionDetailAction.Delegate.AuthorTapped(action.author)) }

        is SectionDetailAction.View.FocusConsumed ->
            state.copy(autoFocusSearch = false) to Effect.none()

        is SectionDetailAction.Delegate -> state to Effect.none()
    }

    private fun searchEffect(
        sectionId: String,
        query: String,
        reset: Boolean,
        cursor: String?,
    ): Effect<SectionDetailAction> = Effect.Run(id = CancelId.Search, cancelInFlight = true) { send ->
        try {
            if (reset) delay(SEARCH_DEBOUNCE_MS)
            val results = homeClient.search(query, sectionId, cursor)
            send(SectionDetailAction.SearchResponse(results, reset))
        } catch (t: Throwable) {
            send(SectionDetailAction.SearchFailed)
        }
    }
}

package com.hurtado.miya.services

/**
 * Cursor page, mirrors `Miya/Services/Pagination.swift`'s `Page<Element>`. `elements` is a plain
 * `List` rather than an `IdentifiedArrayOf` (Kotlin has no identified-collection type in the
 * standard lib) — callers that need id-keyed access build a `Map` from it locally.
 */
data class Page<T>(
    val elements: List<T> = emptyList(),
    val cursor: String? = null,
    val hasMore: Boolean = false,
) {
    companion object {
        fun <T> empty(): Page<T> = Page()
    }
}

/** Mirrors `SearchResults`: a page of matched entries plus any matched authors. */
data class SearchResults(
    val entries: Page<com.hurtado.miya.features.home.HomeSectionItem>,
    val authors: List<com.hurtado.miya.features.home.AuthorRef>,
)

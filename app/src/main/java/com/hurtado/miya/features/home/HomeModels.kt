package com.hurtado.miya.features.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain models — direct port of the model section at the bottom of
 * `Miya/Features/HomeFeature.swift`. `id` is always the server *slug* (used as the Kotlin data
 * class's natural key); `nodeID` is the Relay global id, server-only and excluded from the
 * fixture JSON's shape (defaults to "").
 */
@Serializable
enum class MediaKind {
    @SerialName("song") SONG,
    @SerialName("photo") PHOTO,
    @SerialName("album") ALBUM,
}

@Serializable
data class AuthorRef(
    val id: String,
    val name: String,
    val nodeID: String = "",
)

@Serializable
data class HomeSectionItem(
    val id: String,
    val kind: MediaKind,
    val title: String,
    val subtitle: String,
    val systemImage: String,
    val detail: String,
    @SerialName("imageURL") val imageURL: String? = null,
    /** `/media/{id}/thumb`, longest edge ~512; server-only, populated by live GraphQL mode. */
    @SerialName("thumbnailURL") val thumbnailURL: String? = null,
    val albumID: String? = null,
    val author: AuthorRef? = null,
    /** Populated when `kind == ALBUM`; server-only. */
    val albumNodeID: String? = null,
    /** `items(first: 3)` covers for the fanned album-stack card; server-only. */
    val coverPreviewURLs: List<String> = emptyList(),
    /** `/media/{id}`, Range-capable stream; server-only. */
    @SerialName("audioURL") val audioURL: String? = null,
    val duration: Double? = null,
) {
    /** Mirrors `smallImageURL`: prefer the thumbnail, fall back to the full image. */
    val smallImageURL: String? get() = thumbnailURL ?: imageURL
}

@Serializable
data class HomeSection(
    val id: String,
    val title: String,
    val items: List<HomeSectionItem> = emptyList(),
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val subtitle: String,
    val author: AuthorRef? = null,
    val systemImage: String,
    @SerialName("imageURL") val imageURL: String? = null,
    @SerialName("thumbnailURL") val thumbnailURL: String? = null,
    val items: List<HomeSectionItem> = emptyList(),
    val nodeID: String = "",
    val itemsCursor: String? = null,
    val itemsHasMore: Boolean = false,
)

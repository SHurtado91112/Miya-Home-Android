package com.hurtado.miya.services

import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.features.home.HomeSectionItem

/**
 * Data-plane client, mirrors the `@DependencyClient HomeClient` in the iOS app (backed there by
 * either `MiyaGraphQLClient` or the bundled-JSON fixtures per `RunMode.isFixtureMode`). Two
 * implementations exist here for the same reason: [com.hurtado.miya.services.fixture.FixtureHomeClient]
 * (Stage 2, this file's sibling) and a Stage 7 `LiveHomeClient` over Apollo Kotlin. Which one is
 * bound is decided in a Hilt module based on whether a server URL is configured.
 */
interface HomeClient {
    suspend fun loadSections(): List<HomeSection>

    suspend fun loadAlbums(after: String?): Page<Album>

    suspend fun loadAlbumItems(albumId: String, after: String?): Page<HomeSectionItem>

    /** Cache-miss album resolution, mirrors `loadAlbumNode(nodeID:)`. */
    suspend fun loadAlbumNode(albumId: String): Album?

    suspend fun search(query: String, sectionId: String?, after: String?): SearchResults

    suspend fun loadAuthorItems(authorId: String, after: String?): Page<HomeSectionItem>

    /** Unpaginated, mirrors `loadAuthorAlbums(authorNodeID:)`. */
    suspend fun loadAuthorAlbums(authorId: String): List<Album>

    suspend fun loadAuthor(authorId: String): AuthorRef?
}

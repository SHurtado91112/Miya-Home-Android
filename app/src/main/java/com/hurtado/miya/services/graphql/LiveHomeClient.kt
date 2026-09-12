package com.hurtado.miya.services.graphql

import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.HomeClient
import com.hurtado.miya.services.Page
import com.hurtado.miya.services.SearchResults
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [HomeClient] over [MiyaGraphQlClient] — bound instead of `FixtureHomeClient` whenever
 * `MIYA_SERVER_URL` is configured (see `di/ClientsModule.kt`). **Unverified against a live
 * server**; see [MiyaGraphQlClient]'s doc for why and what to check first.
 *
 * [loadAlbumNode] and [loadAuthorItems]/[loadAuthorAlbums] need a Relay *node id*, but this
 * client's callers only have the *slug* (Android nav routes carry the slug, not the richer
 * object iOS keeps resident in `HomeFeature.State` across a push) — for [loadAlbumNode] and
 * author lookups reached from a slug alone, the slug is passed through as if it were the node
 * id. This works only if the server accepts a slug wherever it accepts a node id (some Relay
 * servers do, treating `node(id:)` loosely); if not, these calls will fail against a real
 * server and need either a slug-keyed lookup query added server-side or the nav routes
 * re-plumbed to carry the node id (available whenever the album/author was reached from a
 * screen that already loaded the full object, e.g. Home's Albums shelf or AuthorDetail's Albums
 * shelf) instead of the slug.
 */
@Singleton
class LiveHomeClient @Inject constructor(
    private val graphql: MiyaGraphQlClient,
) : HomeClient {

    override suspend fun loadSections(): List<HomeSection> = graphql.loadSections()

    override suspend fun loadAlbums(after: String?): Page<Album> = graphql.loadAlbums(after)

    override suspend fun loadAlbumItems(albumId: String, after: String?): Page<HomeSectionItem> =
        graphql.loadAlbumItems(albumId, after)

    override suspend fun loadAlbumNode(albumId: String): Album? = graphql.loadAlbumNode(albumId)

    override suspend fun search(query: String, sectionId: String?, after: String?): SearchResults =
        graphql.search(query, sectionId, after)

    override suspend fun loadAuthorItems(authorId: String, after: String?): Page<HomeSectionItem> =
        graphql.loadAuthorItems(authorId, after)

    override suspend fun loadAuthorAlbums(authorId: String): List<Album> =
        graphql.loadAuthorAlbums(authorId)

    override suspend fun loadAuthor(authorId: String): AuthorRef? =
        // No standalone "author by slug" query on iOS either — AuthorDetail there receives a
        // full AuthorRef at push time, same as this Android port's nav-args approach. Not
        // needed by any caller today; present only for interface parity.
        null
}

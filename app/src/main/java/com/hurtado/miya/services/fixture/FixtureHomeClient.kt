package com.hurtado.miya.services.fixture

import android.content.Context
import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.services.HomeClient
import com.hurtado.miya.services.Page
import com.hurtado.miya.services.SearchResults
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-mode [HomeClient], mirrors the fixture branch of the iOS `HomeClient.liveValue` (see
 * `Services/HomeClient.swift`): reads the bundled `home_sections.json` / `albums.json` assets
 * (copied verbatim from `Miya/Resources/`) and implements search/author queries locally with the
 * same diacritic- and case-insensitive folding rule. Bound whenever no server URL is configured —
 * see `di/ClientsModule.kt`.
 */
@Singleton
class FixtureHomeClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : HomeClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val sections: List<HomeSection> by lazy {
        val text = context.assets.open("home_sections.json").bufferedReader().use { it.readText() }
        json.decodeFromString(text)
    }

    private val albums: List<Album> by lazy {
        val text = context.assets.open("albums.json").bufferedReader().use { it.readText() }
        json.decodeFromString(text)
    }

    override suspend fun loadSections(): List<HomeSection> = withContext(Dispatchers.IO) { sections }

    override suspend fun loadAlbums(after: String?): Page<Album> = withContext(Dispatchers.IO) {
        Page(elements = albums, cursor = null, hasMore = false)
    }

    override suspend fun loadAlbumItems(albumId: String, after: String?): Page<HomeSectionItem> =
        withContext(Dispatchers.IO) {
            val album = albums.firstOrNull { it.id == albumId }
            Page(elements = album?.items ?: emptyList(), cursor = null, hasMore = false)
        }

    override suspend fun loadAlbumNode(albumId: String): Album? = withContext(Dispatchers.IO) {
        albums.firstOrNull { it.id == albumId }
    }

    override suspend fun search(query: String, sectionId: String?, after: String?): SearchResults =
        withContext(Dispatchers.IO) {
            val folded = query.fold()
            if (folded.isBlank()) return@withContext SearchResults(Page.empty(), emptyList())

            val candidateSections = sections.filter { sectionId == null || it.id == sectionId }
            val matchedItems = candidateSections
                .flatMap { it.items }
                .filter { item ->
                    item.title.fold().contains(folded) ||
                        item.subtitle.fold().contains(folded) ||
                        item.detail.fold().contains(folded)
                }
                .distinctBy { it.id }

            val matchedAuthors = allAuthors()
                .filter { it.name.fold().contains(folded) }
                .distinctBy { it.id }

            SearchResults(
                entries = Page(elements = matchedItems, cursor = null, hasMore = false),
                authors = matchedAuthors,
            )
        }

    override suspend fun loadAuthorItems(authorId: String, after: String?): Page<HomeSectionItem> =
        withContext(Dispatchers.IO) {
            val fromSections = sections.flatMap { it.items }
            val fromAlbumItems = albums.flatMap { it.items }
            val items = (fromSections + fromAlbumItems)
                .filter { it.author?.id == authorId }
                .distinctBy { it.id }
            Page(elements = items, cursor = null, hasMore = false)
        }

    override suspend fun loadAuthorAlbums(authorId: String): List<Album> = withContext(Dispatchers.IO) {
        albums.filter { it.author?.id == authorId }
    }

    override suspend fun loadAuthor(authorId: String): AuthorRef? = withContext(Dispatchers.IO) {
        allAuthors().firstOrNull { it.id == authorId }
    }

    private fun allAuthors(): List<AuthorRef> {
        val fromSectionItems = sections.flatMap { it.items }.mapNotNull { it.author }
        val fromAlbumAuthors = albums.mapNotNull { it.author }
        val fromAlbumItems = albums.flatMap { it.items }.mapNotNull { it.author }
        return (fromSectionItems + fromAlbumAuthors + fromAlbumItems).distinctBy { it.id }
    }

    /** Diacritic- and case-insensitive fold, mirrors the iOS fixture client's matching rule. */
    private fun String.fold(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
}

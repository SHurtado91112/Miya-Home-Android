package com.hurtado.miya.services.graphql

import com.hurtado.miya.features.home.Album
import com.hurtado.miya.features.home.AuthorRef
import com.hurtado.miya.features.home.HomeSection
import com.hurtado.miya.features.home.HomeSectionItem
import com.hurtado.miya.features.home.MediaKind
import com.hurtado.miya.services.Page
import com.hurtado.miya.services.SearchResults
import com.hurtado.miya.services.auth.AuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val PAGE_SIZE = 20

/**
 * Data-plane client, port of `MiyaGraphQLClient.swift`: a single `POST {baseUrl}/graphql`
 * endpoint, bearer-authenticated via [authClient], with a single retry after a 401 (calling
 * [AuthClient.invalidateAccessToken] first so the retry forces a refresh rather than resending
 * the token the server just rejected) — mirrors the iOS client's `allowRetry` flag exactly, and
 * deliberately stays a *separate* transport from [com.hurtado.miya.services.auth.MiyaAuthApi]
 * per the two-client separation rule in CLAUDE.md.
 *
 * **Unverified against a live server.** Built with plain OkHttp + kotlinx.serialization rather
 * than Apollo Kotlin codegen (generating typed operations needs the server's GraphQL schema, and
 * this build environment has no route to a reachable MiyaServer to introspect one from), and the
 * field selections/query shapes below are transcribed from the iOS client's documented
 * operations rather than checked against a live response. Treat this as a structural starting
 * point to validate — and adjust field names/shapes against — the first time it's run against a
 * real MiyaServer.
 */
class MiyaGraphQlClient(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val authClient: AuthClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val mediaFields = """
        id slug title subtitle systemImage detail imageUrl thumbnailUrl
        author { id slug name }
        album { slug }
    """.trimIndent()
    private val songFields = "$mediaFields audioUrl durationSeconds"
    private val albumCoverFields = "items(first: 3) { edges { node { imageUrl thumbnailUrl } } }"

    suspend fun loadSections(): List<HomeSection> {
        val query = """
            query Sections {
              sections {
                id slug title
                items {
                  __typename
                  ... on Song { $songFields }
                  ... on Photo { $mediaFields }
                  ... on Album { id slug title subtitle systemImage imageUrl thumbnailUrl $albumCoverFields }
                }
              }
            }
        """.trimIndent()
        val data = execute(query, buildJsonObject {})
        return data.getValue("sections").jsonArray.map { it.jsonObject.toHomeSection() }
    }

    suspend fun loadAlbums(after: String?): Page<Album> {
        val query = """
            query Albums(${'$'}first: Int!, ${'$'}after: String) {
              albums(first: ${'$'}first, after: ${'$'}after) {
                edges {
                  cursor
                  node { id slug title subtitle systemImage imageUrl thumbnailUrl author { id slug name } $albumCoverFields }
                }
                pageInfo { hasNextPage endCursor }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("first", PAGE_SIZE)
            put("after", after)
        }
        val data = execute(query, variables)
        return data.getValue("albums").jsonObject.toAlbumPage()
    }

    suspend fun loadAlbumItems(albumNodeId: String, after: String?): Page<HomeSectionItem> {
        val query = """
            query AlbumItems(${'$'}id: ID!, ${'$'}first: Int!, ${'$'}after: String) {
              node(id: ${'$'}id) {
                ... on Album {
                  items(first: ${'$'}first, after: ${'$'}after) {
                    edges { cursor node { $mediaFields audioUrl durationSeconds } }
                    pageInfo { hasNextPage endCursor }
                  }
                }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("id", albumNodeId)
            put("first", PAGE_SIZE)
            put("after", after)
        }
        val data = execute(query, variables)
        val itemsConnection = data.getValue("node").jsonObject.getValue("items").jsonObject
        return itemsConnection.toItemPage()
    }

    /** Cache-miss album resolution by Relay node id, mirrors `loadAlbumNode(nodeID:)`. */
    suspend fun loadAlbumNode(albumNodeId: String): Album? {
        val query = """
            query AlbumNode(${'$'}id: ID!, ${'$'}first: Int!) {
              node(id: ${'$'}id) {
                ... on Album {
                  id slug title subtitle systemImage imageUrl thumbnailUrl
                  author { id slug name }
                  items(first: ${'$'}first) {
                    edges { cursor node { $mediaFields audioUrl durationSeconds } }
                    pageInfo { hasNextPage endCursor }
                  }
                }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("id", albumNodeId)
            put("first", PAGE_SIZE)
        }
        val data = execute(query, variables)
        val node = data["node"] as? JsonObject ?: return null
        if (node.isEmpty()) return null
        return node.toAlbumWithItems()
    }

    suspend fun search(query: String, sectionSlug: String?, after: String?): SearchResults {
        val gql = """
            query Search(${'$'}query: String!, ${'$'}sectionSlug: String, ${'$'}first: Int!, ${'$'}after: String) {
              search(query: ${'$'}query, sectionSlug: ${'$'}sectionSlug, first: ${'$'}first, after: ${'$'}after) {
                entries {
                  edges { cursor node { __typename $songFields } }
                  pageInfo { hasNextPage endCursor }
                }
                authors { id slug name }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("query", query)
            put("sectionSlug", sectionSlug)
            put("first", PAGE_SIZE)
            put("after", after)
        }
        val data = execute(gql, variables)
        val searchObj = data.getValue("search").jsonObject
        val entries = searchObj.getValue("entries").jsonObject.toItemPage()
        val authors = searchObj.getValue("authors").jsonArray.map { it.jsonObject.toAuthorRef() }
        return SearchResults(entries, authors)
    }

    suspend fun loadAuthorItems(authorNodeId: String, after: String?): Page<HomeSectionItem> {
        val query = """
            query AuthorItems(${'$'}id: ID!, ${'$'}first: Int!, ${'$'}after: String) {
              node(id: ${'$'}id) {
                ... on Author {
                  items(first: ${'$'}first, after: ${'$'}after) {
                    edges { cursor node { __typename $songFields } }
                    pageInfo { hasNextPage endCursor }
                  }
                }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("id", authorNodeId)
            put("first", PAGE_SIZE)
            put("after", after)
        }
        val data = execute(query, variables)
        val itemsConnection = data.getValue("node").jsonObject.getValue("items").jsonObject
        return itemsConnection.toItemPage()
    }

    suspend fun loadAuthorAlbums(authorNodeId: String): List<Album> {
        val query = """
            query AuthorAlbums(${'$'}id: ID!, ${'$'}first: Int!) {
              node(id: ${'$'}id) {
                ... on Author {
                  albums(first: ${'$'}first) { edges { node { id slug title subtitle systemImage imageUrl thumbnailUrl $albumCoverFields } } }
                }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("id", authorNodeId)
            put("first", PAGE_SIZE)
        }
        val data = execute(query, variables)
        val node = data["node"] as? JsonObject ?: return emptyList()
        val albumsConnection = node["albums"] as? JsonObject ?: return emptyList()
        val edges = albumsConnection["edges"] as? JsonArray ?: return emptyList()
        return edges.map { it.jsonObject.getValue("node").jsonObject.toAlbum() }
    }

    // MARK: - Transport

    private suspend fun execute(query: String, variables: JsonObject, allowRetry: Boolean = true): JsonObject =
        withContext(Dispatchers.IO) {
            val token = authClient.accessToken()
            val bodyJson = buildJsonObject {
                put("query", query)
                put("variables", variables)
            }
            val requestBuilder = Request.Builder()
                .url("$baseUrl/graphql")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            if (token != null) requestBuilder.addHeader("Authorization", "Bearer $token")

            val response = try {
                client.newCall(requestBuilder.build()).execute()
            } catch (e: IOException) {
                throw IllegalStateException("Couldn't reach Miya: ${e.message}", e)
            }

            response.use { resp ->
                if (resp.code == 401) {
                    if (!allowRetry) throw IllegalStateException("Session expired")
                    authClient.invalidateAccessToken()
                    resp.close()
                    return@withContext execute(query, variables, allowRetry = false)
                }
                val text = resp.body?.string() ?: throw IllegalStateException("Empty response")
                val root = json.parseToJsonElement(text).jsonObject
                val errors = root["errors"] as? JsonArray
                if (!errors.isNullOrEmpty()) {
                    val message = errors[0].jsonObject["message"]?.jsonPrimitive?.content ?: "Server error"
                    throw IllegalStateException(message)
                }
                root["data"]?.jsonObject ?: throw IllegalStateException("Malformed response")
            }
        }
}

// MARK: - Mapping (GraphQL id = Relay global id -> nodeID; GraphQL slug -> domain id)

private fun JsonObject.toHomeSection(): HomeSection = HomeSection(
    id = getValue("slug").jsonPrimitive.content,
    title = getValue("title").jsonPrimitive.content,
    items = (get("items") as? JsonArray)?.map { it.jsonObject.toHomeSectionItem() } ?: emptyList(),
)

private fun JsonObject.toHomeSectionItem(): HomeSectionItem {
    val kind = when (get("__typename")?.jsonPrimitive?.content) {
        "Song" -> MediaKind.SONG
        "Album" -> MediaKind.ALBUM
        else -> MediaKind.PHOTO
    }
    val albumSlug = (get("album") as? JsonObject)?.get("slug")?.jsonPrimitive?.contentOrNull
    return HomeSectionItem(
        id = getValue("slug").jsonPrimitive.content,
        kind = kind,
        title = getValue("title").jsonPrimitive.content,
        subtitle = get("subtitle")?.jsonPrimitive?.contentOrNull.orEmpty(),
        systemImage = get("systemImage")?.jsonPrimitive?.contentOrNull.orEmpty(),
        detail = get("detail")?.jsonPrimitive?.contentOrNull.orEmpty(),
        imageURL = get("imageUrl")?.jsonPrimitive?.contentOrNull,
        thumbnailURL = get("thumbnailUrl")?.jsonPrimitive?.contentOrNull,
        albumID = albumSlug,
        author = (get("author") as? JsonObject)?.toAuthorRef(),
        albumNodeID = if (kind == MediaKind.ALBUM) get("id")?.jsonPrimitive?.contentOrNull else null,
        coverPreviewURLs = (get("items") as? JsonObject)?.toCoverUrls().orEmpty(),
        audioURL = get("audioUrl")?.jsonPrimitive?.contentOrNull,
        duration = get("durationSeconds")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
    )
}

private fun JsonObject.toAuthorRef(): AuthorRef = AuthorRef(
    id = getValue("slug").jsonPrimitive.content,
    name = getValue("name").jsonPrimitive.content,
    nodeID = get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
)

private fun JsonObject.toAlbum(): Album {
    // Album has no coverPreviewURLs field of its own (only HomeSectionItem does) — the
    // AlbumCardGrid/StackedCoverCard fan in views/SectionCardGrid.kt derives its fanned covers
    // from `album.items.take(3)`, so the `items(first: 3)` preview fetched alongside the album
    // (see `albumCoverFields`) is stashed here as lightweight placeholder items carrying only an
    // image URL, rather than added as a new field on Album.
    val coverItems = (get("items") as? JsonObject)?.toCoverUrls().orEmpty().map { url ->
        HomeSectionItem(
            id = url,
            kind = MediaKind.PHOTO,
            title = "",
            subtitle = "",
            systemImage = "",
            detail = "",
            imageURL = url,
        )
    }
    return Album(
        id = getValue("slug").jsonPrimitive.content,
        title = getValue("title").jsonPrimitive.content,
        subtitle = get("subtitle")?.jsonPrimitive?.contentOrNull.orEmpty(),
        author = (get("author") as? JsonObject)?.toAuthorRef(),
        systemImage = get("systemImage")?.jsonPrimitive?.contentOrNull.orEmpty(),
        imageURL = get("imageUrl")?.jsonPrimitive?.contentOrNull,
        thumbnailURL = get("thumbnailUrl")?.jsonPrimitive?.contentOrNull,
        nodeID = get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
        items = coverItems,
    )
}

private fun JsonObject.toAlbumWithItems(): Album {
    val base = toAlbum()
    val itemsConnection = get("items") as? JsonObject
    val page = itemsConnection?.toItemPage()
    return base.copy(
        items = page?.elements ?: emptyList(),
        itemsCursor = page?.cursor,
        itemsHasMore = page?.hasMore ?: false,
    )
}

/** `items(first: 3) { edges { node { imageUrl thumbnailUrl } } }` -> the fanned cover list. */
private fun JsonObject.toCoverUrls(): List<String> {
    val edges = get("edges") as? JsonArray ?: return emptyList()
    return edges.mapNotNull { edge ->
        val node = edge.jsonObject["node"] as? JsonObject ?: return@mapNotNull null
        (node["thumbnailUrl"] ?: node["imageUrl"])?.jsonPrimitive?.contentOrNull
    }
}

private fun JsonObject.toItemPage(): Page<HomeSectionItem> {
    val edges = get("edges") as? JsonArray ?: JsonArray(emptyList())
    val elements = edges.map { it.jsonObject.getValue("node").jsonObject.toHomeSectionItem() }
    val pageInfo = get("pageInfo") as? JsonObject
    return Page(
        elements = elements,
        cursor = pageInfo?.get("endCursor")?.jsonPrimitive?.contentOrNull,
        hasMore = pageInfo?.get("hasNextPage")?.jsonPrimitive?.content?.toBoolean() ?: false,
    )
}

private fun JsonObject.toAlbumPage(): Page<Album> {
    val edges = get("edges") as? JsonArray ?: JsonArray(emptyList())
    val elements = edges.map { it.jsonObject.getValue("node").jsonObject.toAlbum() }
    val pageInfo = get("pageInfo") as? JsonObject
    return Page(
        elements = elements,
        cursor = pageInfo?.get("endCursor")?.jsonPrimitive?.contentOrNull,
        hasMore = pageInfo?.get("hasNextPage")?.jsonPrimitive?.content?.toBoolean() ?: false,
    )
}

private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

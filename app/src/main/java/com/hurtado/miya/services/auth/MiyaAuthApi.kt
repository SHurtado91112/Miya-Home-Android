package com.hurtado.miya.services.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Auth-plane client, port of `MiyaAuthAPI.swift`: a plain GraphQL-over-HTTP POST to
 * `{baseUrl}/graphql`, deliberately a *separate* transport from the data-plane client
 * (`HomeClient`'s live implementation, Stage 7) — never bearer-authenticated except [viewer],
 * and never routed through a 401-retry, so a transport failure here can't be misread as a
 * session rejection (mirrors the two-client separation rule in CLAUDE.md).
 *
 * Uses plain OkHttp + kotlinx.serialization rather than Apollo Kotlin codegen: generating typed
 * operations needs the server's GraphQL schema, which requires introspecting a reachable
 * MiyaServer — not available from this build environment. Swap for Apollo-generated operations
 * once the schema can be fetched from a live server.
 */
class MiyaAuthApi(
    private val baseUrl: String,
    private val client: OkHttpClient,
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    suspend fun signInWithGoogle(code: String, codeVerifier: String, redirectUri: String, nonce: String): Session {
        val query = """
            mutation SignInWithGoogle(${'$'}code: String!, ${'$'}codeVerifier: String!, ${'$'}redirectUri: String!, ${'$'}nonce: String!) {
              signInWithGoogle(code: ${'$'}code, codeVerifier: ${'$'}codeVerifier, redirectUri: ${'$'}redirectUri, nonce: ${'$'}nonce) {
                accessToken refreshToken expiresAt user { id email name avatarUrl }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject {
            put("code", code)
            put("codeVerifier", codeVerifier)
            put("redirectUri", redirectUri)
            put("nonce", nonce)
        }
        val data = execute(query, variables, bearer = null)
        return parseSession(data.getValue("signInWithGoogle").jsonObject)
    }

    suspend fun refreshSession(refreshToken: String): Session {
        val query = """
            mutation RefreshSession(${'$'}refreshToken: String!) {
              refreshSession(refreshToken: ${'$'}refreshToken) {
                accessToken refreshToken expiresAt user { id email name avatarUrl }
              }
            }
        """.trimIndent()
        val variables = buildJsonObject { put("refreshToken", refreshToken) }
        val data = execute(query, variables, bearer = null)
        return parseSession(data.getValue("refreshSession").jsonObject)
    }

    suspend fun signOut(refreshToken: String) {
        val query = "mutation SignOut(\$refreshToken: String!) { signOut(refreshToken: \$refreshToken) }"
        val variables = buildJsonObject { put("refreshToken", refreshToken) }
        execute(query, variables, bearer = null)
    }

    /** Sent *with* a bearer, to validate a restored token — the one exception to this client
     * staying token-free. */
    suspend fun viewer(accessToken: String): UserProfile? {
        val query = "query Viewer { viewer { id email name avatarUrl } }"
        val data = execute(query, buildJsonObject {}, bearer = accessToken)
        val viewerObj = data["viewer"] as? JsonObject ?: return null
        return parseUser(viewerObj)
    }

    private suspend fun execute(query: String, variables: JsonObject, bearer: String?): JsonObject =
        withContext(Dispatchers.IO) {
            val bodyJson = buildJsonObject {
                put("query", query)
                put("variables", variables)
            }
            val requestBuilder = Request.Builder()
                .url("$baseUrl/graphql")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            if (bearer != null) requestBuilder.addHeader("Authorization", "Bearer $bearer")

            val response = try {
                client.newCall(requestBuilder.build()).execute()
            } catch (e: IOException) {
                throw AuthError.Transport(e.message ?: "network error")
            }

            response.use { resp ->
                if (resp.code == 401) throw AuthError.SessionExpired
                val text = resp.body?.string() ?: throw AuthError.Transport("empty response")
                val root = try {
                    json.parseToJsonElement(text).jsonObject
                } catch (e: Exception) {
                    throw AuthError.Server("Malformed response")
                }
                val errors = root["errors"] as? JsonArray
                if (!errors.isNullOrEmpty()) {
                    val message = errors[0].jsonObject["message"]?.jsonPrimitive?.content ?: "Server error"
                    throw AuthError.Server(message)
                }
                root["data"]?.jsonObject ?: throw AuthError.Server("Malformed response")
            }
        }

    private fun parseSession(obj: JsonObject): Session {
        val expiresAtRaw = obj.getValue("expiresAt").jsonPrimitive.content
        val epochMs = try {
            Instant.parse(expiresAtRaw).toEpochMilli()
        } catch (e: DateTimeParseException) {
            System.currentTimeMillis()
        }
        return Session(
            accessToken = obj.getValue("accessToken").jsonPrimitive.content,
            refreshToken = obj.getValue("refreshToken").jsonPrimitive.content,
            accessTokenExpiresAtEpochMs = epochMs,
            user = parseUser(obj.getValue("user").jsonObject),
        )
    }

    private fun parseUser(obj: JsonObject): UserProfile = UserProfile(
        id = obj.getValue("id").jsonPrimitive.content,
        email = obj.getValue("email").jsonPrimitive.content,
        displayName = (obj["name"] as? JsonPrimitive)?.contentOrNull,
        avatarUrl = (obj["avatarUrl"] as? JsonPrimitive)?.contentOrNull,
    )
}

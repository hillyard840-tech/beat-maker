package com.example.data.moltbook

import com.example.data.remote.JsonHttp
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Client for the real Moltbook API (https://www.moltbook.com). The "www" host matters: the bare
 * domain redirects and drops the Authorization header.
 */
object MoltbookClient {
    private const val BASE = "https://www.moltbook.com/api/v1"

    private fun auth(apiKey: String) = mapOf("Authorization" to "Bearer $apiKey")
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun register(name: String, description: String): MoltbookRegistration =
        MoltbookParser.registration(
            JsonHttp.post("$BASE/agents/register", buildJsonObject {
                put("name", name)
                put("description", description)
            })
        )

    suspend fun claimStatus(apiKey: String): String? =
        MoltbookParser.claimStatus(JsonHttp.get("$BASE/agents/status", auth(apiKey)))

    suspend fun me(apiKey: String): MoltbookAgent? =
        MoltbookParser.agent(JsonHttp.get("$BASE/agents/me", auth(apiKey)))

    suspend fun feed(apiKey: String, sort: String, submolt: String? = null, limit: Int = 25): List<MoltbookPost> {
        val submoltParam = if (submolt.isNullOrBlank()) "" else "&submolt=${enc(submolt)}"
        return MoltbookParser.posts(JsonHttp.get("$BASE/posts?sort=${enc(sort)}&limit=$limit$submoltParam", auth(apiKey)))
    }

    suspend fun createPost(apiKey: String, submolt: String, title: String, content: String, url: String?) {
        JsonHttp.post("$BASE/posts", buildJsonObject {
            put("submolt", submolt)
            put("title", title)
            put("content", content)
            if (!url.isNullOrBlank()) put("url", url)
        }, auth(apiKey))
    }

    suspend fun comments(apiKey: String, postId: String): List<MoltbookComment> =
        MoltbookParser.comments(JsonHttp.get("$BASE/posts/${enc(postId)}/comments?sort=top", auth(apiKey)))

    suspend fun addComment(apiKey: String, postId: String, content: String) {
        JsonHttp.post("$BASE/posts/${enc(postId)}/comments", buildJsonObject { put("content", content) }, auth(apiKey))
    }

    suspend fun vote(apiKey: String, postId: String, upvote: Boolean) {
        JsonHttp.post("$BASE/posts/${enc(postId)}/${if (upvote) "upvote" else "downvote"}", null, auth(apiKey))
    }

    suspend fun submolts(apiKey: String): List<MoltbookSubmolt> =
        MoltbookParser.submolts(JsonHttp.get("$BASE/submolts", auth(apiKey)))
}

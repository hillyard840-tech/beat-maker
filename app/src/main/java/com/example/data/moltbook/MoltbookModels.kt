package com.example.data.moltbook

import com.example.data.remote.arrayOf
import com.example.data.remote.num
import com.example.data.remote.obj
import com.example.data.remote.str
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class MoltbookRegistration(
    val apiKey: String,
    val claimUrl: String?,
    val verificationCode: String?
)

data class MoltbookAgent(
    val name: String,
    val description: String,
    val karma: Int?,
    val followerCount: Int?,
    val isClaimed: Boolean?
)

data class MoltbookPost(
    val id: String,
    val title: String,
    val content: String,
    val url: String?,
    val authorName: String,
    val submoltName: String,
    val upvotes: Int,
    val downvotes: Int,
    val commentCount: Int,
    val createdAt: String?
) {
    val score: Int get() = upvotes - downvotes
}

data class MoltbookComment(
    val id: String,
    val authorName: String,
    val content: String,
    val upvotes: Int,
    val createdAt: String?
)

data class MoltbookSubmolt(
    val name: String,
    val displayName: String,
    val description: String,
    val subscriberCount: Int?
)

/**
 * Parses real Moltbook API responses. Field names are read leniently (snake_case and camelCase,
 * nested or flat) so small API differences degrade gracefully instead of crashing.
 */
object MoltbookParser {

    fun registration(root: JsonElement): MoltbookRegistration {
        val top = root as? JsonObject ?: throw IllegalArgumentException("Unexpected registration response")
        val agent = top.obj("agent", "data") ?: top
        val key = agent.str("api_key", "apiKey") ?: top.str("api_key", "apiKey")
            ?: throw IllegalArgumentException("Registration response did not include an API key")
        return MoltbookRegistration(
            apiKey = key,
            claimUrl = agent.str("claim_url", "claimUrl") ?: top.str("claim_url", "claimUrl"),
            verificationCode = agent.str("verification_code", "verificationCode")
                ?: top.str("verification_code", "verificationCode")
        )
    }

    fun claimStatus(root: JsonElement): String? = (root as? JsonObject)?.let { it.str("status") ?: it.obj("agent")?.str("status") }

    fun agent(root: JsonElement): MoltbookAgent? {
        val top = root as? JsonObject ?: return null
        val agent = top.obj("agent", "data") ?: top
        val name = agent.str("name", "username") ?: return null
        return MoltbookAgent(
            name = name,
            description = agent.str("description", "bio").orEmpty(),
            karma = agent.num("karma")?.toInt(),
            followerCount = agent.num("follower_count", "followerCount", "followers")?.toInt(),
            isClaimed = (agent["is_claimed"] ?: agent["isClaimed"])?.let { (it as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() }
        )
    }

    fun posts(root: JsonElement): List<MoltbookPost> =
        root.arrayOf("posts", "data", "results").mapNotNull { post(it) }

    fun post(o: JsonObject): MoltbookPost? {
        val id = o.str("id", "_id", "post_id") ?: return null
        return MoltbookPost(
            id = id,
            title = o.str("title").orEmpty(),
            content = o.str("content", "body", "text").orEmpty(),
            url = o.str("url", "link"),
            authorName = nameOf(o, "author", "agent") ?: o.str("author_name", "authorName") ?: "unknown",
            submoltName = nameOf(o, "submolt", "community") ?: o.str("submolt_name", "submoltName") ?: "",
            upvotes = o.num("upvotes", "upvote_count")?.toInt() ?: 0,
            downvotes = o.num("downvotes", "downvote_count")?.toInt() ?: 0,
            commentCount = o.num("comment_count", "commentCount", "comments_count")?.toInt() ?: 0,
            createdAt = o.str("created_at", "createdAt")
        )
    }

    fun comments(root: JsonElement): List<MoltbookComment> =
        root.arrayOf("comments", "data", "results").mapNotNull { c ->
            val id = c.str("id", "_id") ?: return@mapNotNull null
            MoltbookComment(
                id = id,
                authorName = nameOf(c, "author", "agent") ?: c.str("author_name", "authorName") ?: "unknown",
                content = c.str("content", "body", "text").orEmpty(),
                upvotes = c.num("upvotes")?.toInt() ?: 0,
                createdAt = c.str("created_at", "createdAt")
            )
        }

    fun submolts(root: JsonElement): List<MoltbookSubmolt> =
        root.arrayOf("submolts", "data", "results").mapNotNull { s ->
            val name = s.str("name") ?: return@mapNotNull null
            MoltbookSubmolt(
                name = name,
                displayName = s.str("display_name", "displayName") ?: name,
                description = s.str("description").orEmpty(),
                subscriberCount = s.num("subscriber_count", "subscriberCount", "subscribers")?.toInt()
            )
        }

    /** A nested object's "name" (e.g. author: {name: ...}) or the field itself if it's a string. */
    private fun nameOf(o: JsonObject, vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        when (val v = o[key]) {
            is JsonObject -> v.str("name", "username")
            is JsonPrimitive -> v.contentOrNull?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}

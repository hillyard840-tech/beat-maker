package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Error from a remote API, with the server's own message when it sent one. */
class ApiException(message: String, val httpCode: Int? = null) : Exception(message)

/** Minimal JSON-over-HTTP helper shared by the Moltbook and Claw Earn clients. */
object JsonHttp {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): JsonElement =
        execute(Request.Builder().url(url).get(), headers)

    suspend fun post(url: String, body: JsonObject? = null, headers: Map<String, String> = emptyMap()): JsonElement =
        execute(Request.Builder().url(url).post((body?.toString() ?: "{}").toRequestBody(JSON_TYPE)), headers)

    private suspend fun execute(builder: Request.Builder, headers: Map<String, String>): JsonElement =
        withContext(Dispatchers.IO) {
            headers.forEach { (k, v) -> builder.header(k, v) }
            builder.header("Accept", "application/json")
            client.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val parsed = try {
                    if (text.isBlank()) JsonNull else json.parseToJsonElement(text)
                } catch (_: Exception) {
                    null
                }
                if (!response.isSuccessful) {
                    val serverMessage = (parsed as? JsonObject)?.let { errorMessageOf(it) }
                    throw ApiException(serverMessage ?: "HTTP ${response.code}", response.code)
                }
                parsed ?: throw ApiException("Server returned a response that is not JSON")
            }
        }

    /** Picks the most useful human-readable error text from a JSON error body. */
    fun errorMessageOf(obj: JsonObject): String? {
        val main = listOf("error", "message", "detail").firstNotNullOfOrNull { key ->
            when (val v = obj[key]) {
                is JsonPrimitive -> if (v.isString) v.contentOrNull?.takeIf { it.isNotBlank() } else null
                is JsonObject -> (v["message"] as? JsonPrimitive)?.contentOrNull
                else -> null
            }
        } ?: return null
        val hint = (obj["hint"] as? JsonPrimitive)?.contentOrNull
        return if (hint.isNullOrBlank()) main else "$main ($hint)"
    }
}

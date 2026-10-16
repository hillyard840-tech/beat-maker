package com.example.api

import com.example.BuildConfig
import com.example.data.remote.JsonHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/** Settings for an OpenAI-compatible server such as a self-hosted FreeLLMAPI. */
data class CompatConfig(val baseUrl: String, val apiKey: String, val model: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

/** [errorCode] is the server's machine-readable code when it sends one (FreeLLMAPI: "no_tools_model", ...). */
class CompatApiException(message: String, val httpCode: Int?, val errorCode: String? = null) : Exception(message)

/** Talks to POST {base}/chat/completions and GET {base}/models. */
object OpenAiCompatClient {
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        // Free providers behind a router can be slow to start; streaming keeps the socket busy.
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    private fun request(cfg: CompatConfig, path: String): Request.Builder {
        val base = OpenAiCompat.normalizeBaseUrl(cfg.baseUrl)
        OpenAiCompat.baseUrlProblem(base)?.let { throw CompatApiException(it, null) }
        return Request.Builder().url("$base/$path").apply {
            if (cfg.apiKey.isNotBlank()) header("Authorization", "Bearer ${cfg.apiKey.trim()}")
        }
    }

    private fun Response.failure(): CompatApiException {
        val body = body?.string().orEmpty()
        val obj = try { JsonHttp.json.parseToJsonElement(body) as? JsonObject } catch (_: Exception) { null }
        val msg = obj?.let { JsonHttp.errorMessageOf(it) } ?: body.take(300).ifBlank { "HTTP $code" }
        val errorCode = ((obj?.get("error") as? JsonObject)?.get("code") as? kotlinx.serialization.json.JsonPrimitive)?.content
        val friendly = when {
            code == 401 -> "The server rejected the API key. Copy the unified key from the FreeLLMAPI Keys page again."
            errorCode == "no_providers_configured" -> "Your server has no provider keys yet. Add at least one provider key in the FreeLLMAPI dashboard."
            errorCode == "no_vision_model" || (code == 429 && msg.contains("vision", ignoreCase = true)) ->
                "None of the models on your server can read images. Enable a vision-capable model in FreeLLMAPI, or send the message without the image."
            else -> "Server returned $code: $msg"
        }
        return CompatApiException(friendly, code, errorCode)
    }

    suspend fun chat(cfg: CompatConfig, request: GenerateContentRequest, includeTools: Boolean = true): GenerateContentResponse =
        withContext(Dispatchers.IO) {
            val body = OpenAiCompat.requestBody(cfg.model, request, stream = false, includeTools = includeTools)
            client.newCall(request(cfg, "chat/completions").post(body.toString().toRequestBody(JSON_TYPE)).build()).execute().use { res ->
                if (!res.isSuccessful) throw res.failure()
                OpenAiCompat.parseResponse(JsonHttp.json.parseToJsonElement(res.body?.string().orEmpty()))
            }
        }

    /** Streams a reply; [onText] receives each new piece of visible text. */
    suspend fun stream(
        cfg: CompatConfig,
        request: GenerateContentRequest,
        includeTools: Boolean = true,
        onText: suspend (String) -> Unit
    ): GenerateContentResponse = withContext(Dispatchers.IO) {
        val body = OpenAiCompat.requestBody(cfg.model, request, stream = true, includeTools = includeTools)
        val httpRequest = request(cfg, "chat/completions")
            .header("Accept", "text/event-stream")
            .post(body.toString().toRequestBody(JSON_TYPE))
            .build()
        client.newCall(httpRequest).execute().use { res ->
            if (!res.isSuccessful) throw res.failure()
            val contentType = res.header("Content-Type").orEmpty()
            if (!contentType.contains("event-stream")) {
                // Some servers ignore stream=true and answer with one JSON body.
                val parsed = OpenAiCompat.parseResponse(JsonHttp.json.parseToJsonElement(res.body?.string().orEmpty()))
                parsed.candidates.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("")
                    ?.takeIf { it.isNotEmpty() }?.let { onText(it) }
                return@withContext parsed
            }
            val acc = OpenAiCompat.StreamAccumulator()
            val source = res.body?.source() ?: throw CompatApiException("Empty response", res.code)
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                val piece = acc.accept(line)
                if (piece.isNotEmpty()) onText(piece)
            }
            acc.result()
        }
    }

    suspend fun models(cfg: CompatConfig): List<String> = withContext(Dispatchers.IO) {
        client.newCall(request(cfg, "models").get().build()).execute().use { res ->
            if (!res.isSuccessful) throw res.failure()
            OpenAiCompat.parseModels(JsonHttp.json.parseToJsonElement(res.body?.string().orEmpty()))
        }
    }
}

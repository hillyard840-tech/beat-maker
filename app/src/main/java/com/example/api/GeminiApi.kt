package com.example.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.example.BuildConfig
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

@Serializable
data class Tool(
    val googleSearch: GoogleSearch? = null,
    val functionDeclarations: List<FunctionDeclaration>? = null
)

@Serializable
class GoogleSearch

@Serializable
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Schema? = null
)

@Serializable
data class Schema(
    val type: String,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val description: String? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null,
    @SerialName("thought_signature")
    @JsonNames("thought_signature", "thoughtSignature")
    val thoughtSignature: String? = null,
    val thought: Boolean? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FunctionCall(
    val name: String,
    val args: Map<String, JsonElement>? = null,
    val id: String? = null,
    @SerialName("thought_signature")
    @JsonNames("thought_signature", "thoughtSignature")
    val thoughtSignature: String? = null
)

@Serializable
data class FunctionResponse(
    val name: String,
    val response: JsonObject,
    val id: String? = null
)

@Serializable
data class GenerateContentResponse(
    // Absent when the prompt is blocked by safety filters, so it must not be required.
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    // Absent when the candidate was stopped early (e.g. finishReason = SAFETY).
    val content: Content? = null,
    val finishReason: String? = null
)

/** Gemini accepts the key as a header, which keeps it out of URLs (and therefore out of logs). */
const val API_KEY_HEADER = "x-goog-api-key"

interface GeminiApiService {
    @POST("v1beta/models/{model}:streamGenerateContent")
    @Streaming
    suspend fun generateContentStream(
        @Path("model") model: String,
        @Header(API_KEY_HEADER) apiKey: String,
        @Body request: GenerateContentRequest,
        @Query("alt") alt: String = "sse"
    ): ResponseBody

    @GET("v1beta/models")
    suspend fun listModels(
        @Header(API_KEY_HEADER) apiKey: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageToken") pageToken: String?
    ): ListModelsResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header(API_KEY_HEADER) apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .apply {
            // Never log request/response bodies in release builds: they contain the user's
            // chats, attachments and (base64) images.
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
                        redactHeader(API_KEY_HEADER)
                    }
                )
            }
        }
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    val service: GeminiApiService by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

/** Resolves the key to use: the user's custom key first, then the build-time key (ignoring the placeholder). */
object ApiKeys {
    private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"

    fun resolve(customKey: String?): String {
        val custom = customKey?.trim().orEmpty()
        if (custom.isNotEmpty()) return custom
        val envKey = BuildConfig.GEMINI_API_KEY.trim()
        return if (envKey == PLACEHOLDER_KEY) "" else envKey
    }
}

package com.example.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

data class GeminiModel(
    val id: String,
    val name: String,
    val category: String, // "Flash", "Lite", "Pro", "Other"
    val description: String
)

@Serializable
data class ApiModel(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String> = emptyList()
)

@Serializable
data class ListModelsResponse(
    val models: List<ApiModel> = emptyList(),
    val nextPageToken: String? = null
)

/**
 * The model list comes from the Gemini API itself (models.list), so only models that actually
 * exist for the user's key are offered. Nothing is hardcoded except Google's documented
 * "-latest" aliases, used before the list has loaded.
 */
object ModelRegistry {
    /** Google-maintained aliases that always point at the current Flash / Flash-Lite release. */
    const val DEFAULT_MODEL = "gemini-flash-lite-latest"
    val ALIAS_FALLBACKS = listOf("gemini-flash-lite-latest", "gemini-flash-latest")

    private val _models = MutableStateFlow<List<GeminiModel>>(emptyList())
    val models: StateFlow<List<GeminiModel>> = _models.asStateFlow()

    /** Imagen models (method "predict") available to this key, newest first. */
    private val _imageModels = MutableStateFlow<List<String>>(emptyList())
    val imageModels: StateFlow<List<String>> = _imageModels.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    suspend fun refresh(apiKey: String) {
        if (apiKey.isBlank()) {
            _loadError.value = "Add an API key to load the models available to you."
            return
        }
        try {
            val all = mutableListOf<ApiModel>()
            var pageToken: String? = null
            do {
                val page = RetrofitClient.service.listModels(apiKey, 1000, pageToken)
                all += page.models
                pageToken = page.nextPageToken?.takeIf { it.isNotBlank() }
            } while (pageToken != null)
            _models.value = toChatModels(all)
            _imageModels.value = toImageModels(all)
            _loadError.value = null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _loadError.value = "Couldn't load models: ${e.message}"
        }
    }

    /** Keeps Gemini models that support generateContent, newest-looking names first. */
    fun toChatModels(apiModels: List<ApiModel>): List<GeminiModel> =
        apiModels
            .filter { "generateContent" in it.supportedGenerationMethods }
            .map { m ->
                val id = m.name.removePrefix("models/")
                GeminiModel(
                    id = id,
                    name = m.displayName?.takeIf { it.isNotBlank() } ?: id,
                    category = categoryOf(id),
                    description = m.description.orEmpty()
                )
            }
            .filter { it.id.startsWith("gemini") && !it.id.contains("embedding") }
            .distinctBy { it.id }
            .sortedByDescending { it.id }

    fun toImageModels(apiModels: List<ApiModel>): List<String> =
        apiModels
            .filter { "predict" in it.supportedGenerationMethods }
            .map { it.name.removePrefix("models/") }
            .filter { it.startsWith("imagen") }
            .distinct()
            .sortedDescending()

    fun categoryOf(id: String): String = when {
        id.contains("lite") -> "Lite"
        id.contains("pro") -> "Pro"
        id.contains("flash") -> "Flash"
        else -> "Other"
    }

    /**
     * Models to try when [failedModel] is rate-limited: lighter models first, only ones the API
     * reported as available (or the official aliases if the list hasn't loaded).
     */
    fun fallbackCandidates(failedModel: String): List<String> {
        val live = _models.value
        val ordered = if (live.isEmpty()) {
            ALIAS_FALLBACKS
        } else {
            val text = live.filter { !it.id.contains("image") && !it.id.contains("tts") && !it.id.contains("live") }
            text.filter { it.category == "Lite" }.map { it.id } + text.filter { it.category == "Flash" }.map { it.id }
        }
        return ordered.filter { it != failedModel }.distinct().take(4)
    }
}

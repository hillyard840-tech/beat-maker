package com.example.api

/** Which backend answers chat requests. */
enum class LlmProvider(val id: String) {
    GEMINI("gemini"),
    OPENAI_COMPATIBLE("openai_compatible");

    companion object {
        fun from(id: String?): LlmProvider = entries.firstOrNull { it.id == id } ?: GEMINI
    }
}

data class LlmSettings(
    val provider: LlmProvider,
    val geminiKey: String,
    val compat: CompatConfig
) {
    val isReady: Boolean
        get() = when (provider) {
            LlmProvider.GEMINI -> geminiKey.isNotBlank()
            LlmProvider.OPENAI_COMPATIBLE -> compat.isConfigured
        }

    /** Shared settings keys (omni_agent_prefs) so the background worker reads the same choice. */
    companion object {
        const val PREF_PROVIDER = "llm_provider"
        const val PREF_COMPAT_BASE_URL = "compat_base_url"
        const val PREF_COMPAT_API_KEY = "compat_api_key"
        const val PREF_COMPAT_MODEL = "compat_model"
        const val DEFAULT_COMPAT_MODEL = "auto"

        fun fromPrefs(prefs: android.content.SharedPreferences): LlmSettings = LlmSettings(
            provider = LlmProvider.from(prefs.getString(PREF_PROVIDER, null)),
            geminiKey = ApiKeys.resolve(prefs.getString("custom_api_key", "")),
            compat = CompatConfig(
                baseUrl = prefs.getString(PREF_COMPAT_BASE_URL, "").orEmpty(),
                apiKey = prefs.getString(PREF_COMPAT_API_KEY, "").orEmpty(),
                model = prefs.getString(PREF_COMPAT_MODEL, DEFAULT_COMPAT_MODEL).orEmpty().ifBlank { DEFAULT_COMPAT_MODEL }
            )
        )
    }
}

/**
 * One entry point for non-streaming generation, used by Canvas, Deep Research and the
 * background worker. [geminiModel] is ignored for OpenAI-compatible servers, which use their
 * configured model (FreeLLMAPI's "auto" lets the router pick).
 */
object LlmGateway {
    suspend fun generate(settings: LlmSettings, geminiModel: String, request: GenerateContentRequest): GenerateContentResponse =
        when (settings.provider) {
            LlmProvider.GEMINI -> RetrofitClient.service.generateContent(geminiModel, settings.geminiKey, request)
            LlmProvider.OPENAI_COMPATIBLE -> try {
                OpenAiCompatClient.chat(settings.compat, request)
            } catch (e: CompatApiException) {
                if (!request.tools.isNullOrEmpty() && OpenAiCompat.looksLikeToolsUnsupported(e.message, e.errorCode)) {
                    OpenAiCompatClient.chat(settings.compat, request, includeTools = false)
                } else {
                    throw e
                }
            }
        }

    /** Name to show for the model that answered. */
    fun modelLabel(settings: LlmSettings, geminiModel: String): String =
        if (settings.provider == LlmProvider.OPENAI_COMPATIBLE) settings.compat.model else geminiModel
}

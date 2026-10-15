package com.example.api

data class GeminiModel(
    val id: String,
    val name: String,
    val category: String, // "Flash", "Lite", "Pro"
    val description: String,
    val quotaFriendly: Boolean = false
)

object ModelRegistry {
    val AVAILABLE_MODELS = listOf(
        GeminiModel(
            id = "gemini-3.5-flash-lite",
            name = "Gemini 3.5 Flash Lite",
            category = "Lite",
            description = "Lightweight & quota-optimized for high-frequency chats",
            quotaFriendly = true
        ),
        GeminiModel(
            id = "gemini-flash-lite-latest",
            name = "Gemini Flash Lite Latest",
            category = "Lite",
            description = "Dynamic pointer to the latest lightweight flash-lite build",
            quotaFriendly = true
        ),
        GeminiModel(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            category = "Flash",
            description = "Balanced multimodal foundation model with high quota allowance"
        ),
        GeminiModel(
            id = "gemini-3.7-flash",
            name = "Gemini 3.7 Flash",
            category = "Flash",
            description = "High-speed flagship with strong reasoning and coding abilities"
        ),
        GeminiModel(
            id = "gemini-3.8-flash",
            name = "Gemini 3.8 Flash",
            category = "Flash",
            description = "Latest high-speed multimodal model with cutting-edge capabilities"
        ),
        GeminiModel(
            id = "gemini-3.6-flash",
            name = "Gemini 3.6 Flash",
            category = "Flash",
            description = "Reliable standard model for versatile general tasks"
        ),
        GeminiModel(
            id = "gemini-flash-latest",
            name = "Gemini Flash Latest",
            category = "Flash",
            description = "Dynamic alias that automatically points to the latest stable flash"
        ),
        GeminiModel(
            id = "gemini-3.1-flash-lite-preview",
            name = "Gemini 3.1 Flash Lite",
            category = "Lite",
            description = "Ultra-fast preview lite model with minimal token overhead",
            quotaFriendly = true
        ),
        GeminiModel(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro Preview",
            category = "Pro",
            description = "Deep reasoning, complex STEM, and advanced logic analysis"
        )
    )

    // Ordered by quota resilience: Lite and standard models first to recover rapidly from 429
    val FALLBACK_CANDIDATES = listOf(
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3.5-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.8-flash"
    )
}

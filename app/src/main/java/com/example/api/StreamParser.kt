package com.example.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses Gemini's streamGenerateContent?alt=sse output ("data: {json}" lines) and merges the
 * chunks into one response that the rest of the app can handle like a normal generateContent reply.
 */
@OptIn(ExperimentalSerializationApi::class)
object StreamParser {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    class StreamError(message: String) : Exception(message)

    /** Returns the chunk on a "data:" line, null for other SSE lines; throws on an error payload. */
    fun parseLine(line: String): GenerateContentResponse? {
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty() || payload == "[DONE]") return null
        val element = json.parseToJsonElement(payload)
        val error = (element as? JsonObject)?.get("error") as? JsonObject
        if (error != null) {
            val message = (error["message"] as? JsonPrimitive)?.contentOrNull ?: "Streaming error"
            throw StreamError(message)
        }
        return json.decodeFromJsonElement(GenerateContentResponse.serializer(), element)
    }

    /** Visible (non-thought) text accumulated so far, for the live bubble. */
    fun visibleText(chunks: List<GenerateContentResponse>): String =
        chunks.joinToString("") { chunk ->
            chunk.candidates.firstOrNull()?.content?.parts.orEmpty()
                .filter { it.thought != true && it.functionCall == null }
                .joinToString("") { it.text.orEmpty() }
        }

    /** Merges streamed chunks: adjacent plain-text parts are joined, everything else is kept. */
    fun merge(chunks: List<GenerateContentResponse>): GenerateContentResponse {
        val merged = mutableListOf<Part>()
        var role: String? = null
        var finishReason: String? = null
        var sawCandidate = false

        for (chunk in chunks) {
            val candidate = chunk.candidates.firstOrNull() ?: continue
            sawCandidate = true
            candidate.finishReason?.let { finishReason = it }
            val content = candidate.content ?: continue
            content.role?.let { role = it }
            for (part in content.parts) {
                val last = merged.lastOrNull()
                val isPlainText = part.text != null && part.functionCall == null && part.functionResponse == null && part.inlineData == null
                val lastIsPlainText = last != null && last.text != null && last.functionCall == null &&
                    last.functionResponse == null && last.inlineData == null
                if (isPlainText && lastIsPlainText && last!!.thought == part.thought) {
                    merged[merged.size - 1] = last.copy(
                        text = last.text + part.text,
                        thoughtSignature = part.thoughtSignature ?: last.thoughtSignature
                    )
                } else {
                    merged += part
                }
            }
        }

        if (!sawCandidate) return GenerateContentResponse(candidates = emptyList())
        return GenerateContentResponse(
            candidates = listOf(
                Candidate(
                    content = if (merged.isEmpty()) null else Content(parts = merged, role = role ?: "model"),
                    finishReason = finishReason
                )
            )
        )
    }
}

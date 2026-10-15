package com.example

import com.example.api.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    @Test
    fun testSerialization() {
        val request = GenerateContentRequest(
            contents = emptyList(),
            tools = listOf(Tool(googleSearch = GoogleSearch()))
        )
        val out = json.encodeToString(request)
        println(out)
        assertTrue(out.contains("googleSearch"))
    }

    @Test
    fun testThoughtSignatureDeserializationAndSerialization() {
        val rawJson = """
            {
                "functionCall": {
                    "name": "fetch_website",
                    "args": { "url": "http://www.moltbook.com/skill.md" }
                },
                "thought_signature": "cryptographic_token_12345"
            }
        """.trimIndent()

        val part = json.decodeFromString<Part>(rawJson)
        assertNotNull(part.functionCall)
        assertEquals("fetch_website", part.functionCall?.name)
        assertEquals("cryptographic_token_12345", part.thoughtSignature)

        val encoded = json.encodeToString(part)
        assertTrue(encoded.contains("thought_signature"))
        assertTrue(encoded.contains("cryptographic_token_12345"))
    }

    @Test
    fun safetyBlockedResponseDecodesWithoutCandidates() {
        // Gemini omits "candidates" entirely when the prompt itself is blocked.
        val blocked = json.decodeFromString<GenerateContentResponse>(
            """{"promptFeedback":{"blockReason":"SAFETY"}}"""
        )
        assertTrue(blocked.candidates.isEmpty())

        // ...and omits "content" when a candidate is stopped by the safety filter.
        val stopped = json.decodeFromString<GenerateContentResponse>(
            """{"candidates":[{"finishReason":"SAFETY","index":0}]}"""
        )
        assertEquals("SAFETY", stopped.candidates.first().finishReason)
        assertEquals(null, stopped.candidates.first().content)
    }
}

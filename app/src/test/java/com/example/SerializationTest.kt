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
}

package com.example.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamAndModelsTest {

    @Test
    fun `merges streamed text chunks into one part`() {
        val lines = listOf(
            """data: {"candidates":[{"content":{"role":"model","parts":[{"text":"Hel"}]}}]}""",
            "",
            """data: {"candidates":[{"content":{"role":"model","parts":[{"text":"lo"}]},"finishReason":"STOP"}]}"""
        )
        val chunks = lines.mapNotNull { StreamParser.parseLine(it) }
        assertEquals("Hello", StreamParser.visibleText(chunks))
        val merged = StreamParser.merge(chunks)
        val parts = merged.candidates.single().content!!.parts
        assertEquals(1, parts.size)
        assertEquals("Hello", parts[0].text)
        assertEquals("STOP", merged.candidates.single().finishReason)
    }

    @Test
    fun `keeps function calls and thought signatures separate from text`() {
        val chunks = listOf(
            """data: {"candidates":[{"content":{"parts":[{"text":"thinking","thought":true}]}}]}""",
            """data: {"candidates":[{"content":{"parts":[{"functionCall":{"name":"search_live_web","args":{"query":"x"}},"thoughtSignature":"sig1"}]}}]}"""
        ).mapNotNull { StreamParser.parseLine(it) }
        assertEquals("", StreamParser.visibleText(chunks))
        val parts = StreamParser.merge(chunks).candidates.single().content!!.parts
        assertEquals(2, parts.size)
        assertEquals("search_live_web", parts[1].functionCall!!.name)
        assertEquals("sig1", parts[1].thoughtSignature)
    }

    @Test(expected = StreamParser.StreamError::class)
    fun `error payload in the stream is raised`() {
        StreamParser.parseLine("""data: {"error":{"code":429,"message":"Resource exhausted"}}""")
    }

    @Test
    fun `empty stream means no candidates`() {
        assertTrue(StreamParser.merge(emptyList()).candidates.isEmpty())
        assertNull(StreamParser.parseLine(": keep-alive"))
    }

    @Test
    fun `builds the chat model list from the real API response`() {
        val models = ModelRegistry.toChatModels(
            listOf(
                ApiModel("models/gemini-flash-lite-latest", "Gemini Flash-Lite Latest", "", listOf("generateContent", "countTokens")),
                ApiModel("models/gemini-pro-x", "Gemini Pro X", "", listOf("generateContent")),
                ApiModel("models/text-embedding-004", "Embedding", "", listOf("embedContent")),
                ApiModel("models/imagen-x", "Imagen", "", listOf("predict"))
            )
        )
        assertEquals(listOf("gemini-pro-x", "gemini-flash-lite-latest"), models.map { it.id })
        assertEquals("Lite", models.first { it.id == "gemini-flash-lite-latest" }.category)
        assertEquals(listOf("imagen-x"), ModelRegistry.toImageModels(listOf(ApiModel("models/imagen-x", supportedGenerationMethods = listOf("predict")))))
    }

    @Test
    fun `fallback uses documented aliases before the list loads`() {
        assertEquals(listOf("gemini-flash-latest"), ModelRegistry.fallbackCandidates("gemini-flash-lite-latest"))
    }
}

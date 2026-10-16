package com.example.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatTest {

    @Test
    fun `normalizes the server address`() {
        assertEquals("http://192.168.1.20:3001/v1", OpenAiCompat.normalizeBaseUrl("192.168.1.20:3001"))
        assertEquals("http://192.168.1.20:3001/v1", OpenAiCompat.normalizeBaseUrl("http://192.168.1.20:3001/"))
        assertEquals("https://llm.example.com/v1", OpenAiCompat.normalizeBaseUrl("https://llm.example.com/v1/"))
        assertEquals("https://example.com/openai/v1", OpenAiCompat.normalizeBaseUrl("https://example.com/openai/v1"))
    }

    @Test
    fun `plain http is only allowed on your own network`() {
        listOf(
            "http://localhost:3001/v1", "http://127.0.0.1:3001/v1", "http://192.168.1.20:3001/v1",
            "http://10.0.0.5:3001/v1", "http://100.101.102.103:3001/v1", "http://my-pc:3001/v1",
            "http://raspberrypi.local:3001/v1", "https://llm.example.com/v1"
        ).forEach { assertNull(it, OpenAiCompat.baseUrlProblem(it)) }
        assertNotNull(OpenAiCompat.baseUrlProblem("http://llm.example.com/v1"))
        assertNotNull(OpenAiCompat.baseUrlProblem("http://8.8.8.8:3001/v1"))
        assertNotNull(OpenAiCompat.baseUrlProblem("ftp://192.168.1.2/v1"))
        assertNotNull(OpenAiCompat.baseUrlProblem(""))
    }

    @Test
    fun `maps system prompt, roles, images and tools to the OpenAI format`() {
        val request = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(text = "Be brief."))),
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = "hi"))),
                Content(role = "model", parts = listOf(Part(text = "hello"))),
                Content(role = "user", parts = listOf(Part(text = "what is this?"), Part(inlineData = InlineData("image/jpeg", "QUJD"))))
            ),
            tools = listOf(Tool(functionDeclarations = listOf(FunctionDeclaration("search_live_web", "Search", Schema("OBJECT", mapOf("query" to Schema("STRING")), listOf("query"))))))
        )
        val body = OpenAiCompat.requestBody("auto", request, stream = true)
        assertEquals("auto", body["model"]!!.jsonPrimitive.content)
        assertEquals("true", body["stream"]!!.jsonPrimitive.content)
        val messages = body["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it["role"]!!.jsonPrimitive.content })
        assertEquals("Be brief.", messages[0]["content"]!!.jsonPrimitive.content)
        val blocks = messages[3]["content"]!!.jsonArray.map { it.jsonObject }
        assertEquals("text", blocks[0]["type"]!!.jsonPrimitive.content)
        assertEquals("data:image/jpeg;base64,QUJD", blocks[1]["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content)
        val fn = body["tools"]!!.jsonArray[0].jsonObject["function"]!!.jsonObject
        assertEquals("search_live_web", fn["name"]!!.jsonPrimitive.content)
        assertEquals("object", fn["parameters"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", fn["parameters"]!!.jsonObject["properties"]!!.jsonObject["query"]!!.jsonObject["type"]!!.jsonPrimitive.content)

        assertNull(OpenAiCompat.requestBody("auto", request, stream = false, includeTools = false)["tools"])
    }

    @Test
    fun `maps a tool call and its result into assistant and tool messages`() {
        val args = mapOf("query" to JsonPrimitive("weather"))
        val request = GenerateContentRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = "weather?"))),
                Content(role = "model", parts = listOf(Part(functionCall = FunctionCall("search_live_web", args, id = "call_1")))),
                Content(role = "user", parts = listOf(Part(functionResponse = FunctionResponse("search_live_web", buildJsonObject { put("content", "sunny") }, id = "call_1"))))
            )
        )
        val messages = OpenAiCompat.requestBody("auto", request, stream = false)["messages"]!!.jsonArray.map { it.jsonObject }
        assertEquals(JsonNull, messages[1]["content"])
        val call = messages[1]["tool_calls"]!!.jsonArray[0].jsonObject
        assertEquals("call_1", call["id"]!!.jsonPrimitive.content)
        assertEquals("""{"query":"weather"}""", call["function"]!!.jsonObject["arguments"]!!.jsonPrimitive.content)
        assertEquals("tool", messages[2]["role"]!!.jsonPrimitive.content)
        assertEquals("call_1", messages[2]["tool_call_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tool results without ids reuse the generated call id`() {
        val request = GenerateContentRequest(
            contents = listOf(
                Content(role = "model", parts = listOf(Part(functionCall = FunctionCall("fetch_website", emptyMap())))),
                Content(role = "user", parts = listOf(Part(functionResponse = FunctionResponse("fetch_website", JsonObject(emptyMap())))))
            )
        )
        val messages = OpenAiCompat.requestBody("auto", request, stream = false)["messages"]!!.jsonArray.map { it.jsonObject }
        val callId = messages[0]["tool_calls"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(callId, messages[1]["tool_call_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `parses a normal reply and a tool call reply`() {
        val text = OpenAiCompat.parseResponse(Json.parseToJsonElement(
            """{"choices":[{"message":{"role":"assistant","content":"Hi!"},"finish_reason":"stop"}]}"""
        ))
        assertEquals("Hi!", text.candidates.single().content!!.parts.single().text)

        val tool = OpenAiCompat.parseResponse(Json.parseToJsonElement(
            """{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"c1","type":"function","function":{"name":"search_live_web","arguments":"{\"query\":\"x\"}"}}]},"finish_reason":"tool_calls"}]}"""
        ))
        val call = tool.candidates.single().content!!.parts.single().functionCall!!
        assertEquals("search_live_web", call.name)
        assertEquals("x", call.args!!["query"]!!.jsonPrimitive.content)
        assertEquals("c1", call.id)

        val filtered = OpenAiCompat.parseResponse(Json.parseToJsonElement("""{"choices":[{"message":{"content":""},"finish_reason":"content_filter"}]}"""))
        assertEquals("SAFETY", filtered.candidates.single().finishReason)
    }

    @Test
    fun `accumulates streamed text and fragmented tool calls`() {
        val acc = OpenAiCompat.StreamAccumulator()
        val pieces = listOf(
            """data: {"choices":[{"index":0,"delta":{"role":"assistant","content":""}}]}""",
            """data: {"choices":[{"index":0,"delta":{"content":"Hel"}}]}""",
            ": keep-alive",
            """data: {"choices":[{"index":0,"delta":{"content":"lo"}}]}""",
            """data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"id":"c9","function":{"name":"fetch_website","arguments":"{\"url\":"}}]}}]}""",
            """data: {"choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\"https://a.b\"}"}}]}}]}""",
            """data: {"choices":[{"index":0,"delta":{},"finish_reason":"tool_calls"}]}""",
            "data: [DONE]"
        ).map { acc.accept(it) }
        assertEquals(listOf("", "Hel", "", "lo", "", "", "", ""), pieces)
        val parts = acc.result().candidates.single().content!!.parts
        assertEquals("Hello", parts[0].text)
        assertEquals("https://a.b", parts[1].functionCall!!.args!!["url"]!!.jsonPrimitive.content)
        assertEquals("c9", parts[1].functionCall!!.id)
    }

    @Test(expected = StreamParser.StreamError::class)
    fun `stream error payloads are raised`() {
        OpenAiCompat.StreamAccumulator().accept("""data: {"error":{"message":"All providers exhausted"}}""")
    }

    @Test
    fun `lists models and detects tool rejections`() {
        assertEquals(listOf("auto", "fake-1"), OpenAiCompat.parseModels(Json.parseToJsonElement("""{"data":[{"id":"auto"},{"id":"fake-1"}]}""")))
        assertTrue(OpenAiCompat.looksLikeToolsUnsupported(null, "no_tools_model"))
        assertTrue(OpenAiCompat.looksLikeToolsUnsupported("This request includes tools, but no tool-capable model is enabled."))
        // Real FreeLLMAPI response when the only keyed model can't call tools:
        assertTrue(OpenAiCompat.looksLikeToolsUnsupported(
            "Server returned 429: All models exhausted: 1 route checked (1 model lacks tool-calling). Add more API keys or wait for rate limits to reset.",
            "routing_exhausted"
        ))
        assertTrue(!OpenAiCompat.looksLikeToolsUnsupported("All models exhausted: 1 route checked (1 model lacks vision).", "routing_exhausted"))
        assertTrue(!OpenAiCompat.looksLikeToolsUnsupported("Rate limit exceeded"))
    }
}

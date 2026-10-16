package com.example.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.InetAddress
import java.net.URI

/**
 * Translates between the app's Gemini-shaped requests/responses and the OpenAI chat-completions
 * format used by FreeLLMAPI and other OpenAI-compatible servers. The rest of the app (tool loop,
 * streaming, voice) keeps working on [GenerateContentRequest] / [GenerateContentResponse].
 */
object OpenAiCompat {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---------------------------------------------------------------- base URL

    /**
     * Normalises what the user typed: adds http(s) if missing, drops a trailing slash and appends
     * "/v1" when no path was given (FreeLLMAPI serves the API at /v1).
     */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim().trimEnd('/')
        if (url.isEmpty()) return ""
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        val path = try { URI(url).path.orEmpty() } catch (_: Exception) { "" }
        if (path.isEmpty() || path == "/") url = "$url/v1"
        return url
    }

    /**
     * Returns null if [baseUrl] may be used, otherwise why not. Plain http is only allowed to
     * your own network (localhost, private IPs, *.local, single-label host names): sending the
     * key and your chats over unencrypted http across the internet would expose them.
     */
    fun baseUrlProblem(baseUrl: String): String? {
        if (baseUrl.isBlank()) return "Enter the server address."
        val uri = try { URI(baseUrl) } catch (_: Exception) { return "That address isn't a valid URL." }
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase() ?: return "That address has no host name."
        if (scheme == "https") return null
        if (scheme != "http") return "Use an http:// or https:// address."
        return if (isLocalHost(host)) null
        else "Plain http is only allowed on your own network. Use https:// for servers on the internet."
    }

    private fun isLocalHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home.arpa")) return true
        if (!host.contains('.') && !host.contains(':')) return true // LAN name like "my-pc"
        val literal = host.trim('[', ']')
        val looksLikeIp = literal.all { it.isDigit() || it == '.' } || literal.contains(':')
        if (!looksLikeIp) return false
        return try {
            val a = InetAddress.getByName(literal) // literal IPs don't trigger DNS
            a.isLoopbackAddress || a.isSiteLocalAddress || a.isLinkLocalAddress ||
                (a.address.size == 4 && (a.address[0].toInt() and 0xFF) == 100 && (a.address[1].toInt() and 0xFF) in 64..127) || // Tailscale / CGNAT
                (a.address.size == 16 && (a.address[0].toInt() and 0xFE) == 0xFC)
        } catch (_: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- requests

    fun requestBody(model: String, request: GenerateContentRequest, stream: Boolean, includeTools: Boolean = true): JsonObject =
        buildJsonObject {
            put("model", model.ifBlank { "auto" })
            put("stream", stream)
            put("messages", messages(request))
            val tools = request.tools?.flatMap { it.functionDeclarations.orEmpty() }.orEmpty()
            if (includeTools && tools.isNotEmpty()) {
                putJsonArray("tools") {
                    tools.forEach { fn ->
                        addJsonObject {
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", fn.name)
                                put("description", fn.description)
                                fn.parameters?.let { put("parameters", schema(it)) }
                            }
                        }
                    }
                }
            }
        }

    private fun messages(request: GenerateContentRequest): JsonArray = buildJsonArray {
        request.systemInstruction?.parts?.mapNotNull { it.text }?.joinToString("\n")?.takeIf { it.isNotBlank() }?.let { sys ->
            addJsonObject {
                put("role", "system")
                put("content", sys)
            }
        }
        var callCounter = 0
        val pendingCallIds = ArrayDeque<String>()
        for (content in request.contents) {
            val isModel = content.role == "model"
            val toolResponses = content.parts.mapNotNull { it.functionResponse }
            val calls = content.parts.mapNotNull { it.functionCall }
            val texts = content.parts.filter { it.thought != true }.mapNotNull { it.text }.filter { it.isNotBlank() }
            val images = content.parts.mapNotNull { it.inlineData }

            if (toolResponses.isNotEmpty()) {
                toolResponses.forEach { r ->
                    addJsonObject {
                        put("role", "tool")
                        put("tool_call_id", r.id ?: pendingCallIds.removeFirstOrNull() ?: "call_${callCounter++}")
                        put("content", r.response.toString())
                    }
                }
                continue
            }

            addJsonObject {
                put("role", if (isModel) "assistant" else "user")
                if (images.isEmpty()) {
                    if (texts.isEmpty() && calls.isNotEmpty()) put("content", JsonNull) else put("content", texts.joinToString("\n"))
                } else {
                    putJsonArray("content") {
                        texts.forEach { t -> addJsonObject { put("type", "text"); put("text", t) } }
                        images.forEach { img ->
                            addJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") { put("url", "data:${img.mimeType};base64,${img.data}") }
                            }
                        }
                    }
                }
                if (isModel && calls.isNotEmpty()) {
                    putJsonArray("tool_calls") {
                        calls.forEach { call ->
                            val id = call.id ?: "call_${callCounter++}"
                            pendingCallIds.addLast(id)
                            addJsonObject {
                                put("id", id)
                                put("type", "function")
                                putJsonObject("function") {
                                    put("name", call.name)
                                    put("arguments", JsonObject(call.args.orEmpty()).toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Gemini schemas use upper-case types ("OBJECT"); JSON Schema / OpenAI wants lower-case. */
    private fun schema(s: Schema): JsonObject = buildJsonObject {
        put("type", s.type.lowercase())
        s.description?.let { put("description", it) }
        s.properties?.let { props -> putJsonObject("properties") { props.forEach { (k, v) -> put(k, schema(v)) } } }
        s.required?.let { req -> putJsonArray("required") { req.forEach { add(it) } } }
    }

    // ---------------------------------------------------------------- responses

    fun parseResponse(root: JsonElement): GenerateContentResponse {
        val choice = ((root as? JsonObject)?.get("choices") as? JsonArray)?.firstOrNull() as? JsonObject
            ?: return GenerateContentResponse(emptyList())
        val message = choice["message"] as? JsonObject
        val parts = mutableListOf<Part>()
        message?.let { m ->
            textOf(m["content"])?.takeIf { it.isNotEmpty() }?.let { parts += Part(text = it) }
            (m["tool_calls"] as? JsonArray)?.forEach { tc ->
                val obj = tc as? JsonObject ?: return@forEach
                val fn = obj["function"] as? JsonObject ?: return@forEach
                val name = (fn["name"] as? JsonPrimitive)?.contentOrNull ?: return@forEach
                parts += Part(functionCall = FunctionCall(name = name, args = parseArgs((fn["arguments"] as? JsonPrimitive)?.contentOrNull), id = (obj["id"] as? JsonPrimitive)?.contentOrNull))
            }
        }
        return GenerateContentResponse(
            listOf(Candidate(content = if (parts.isEmpty()) null else Content(parts, "model"), finishReason = finishReason(choice)))
        )
    }

    private fun finishReason(choice: JsonObject): String? =
        when ((choice["finish_reason"] as? JsonPrimitive)?.contentOrNull) {
            null -> null
            "content_filter" -> "SAFETY"
            "length" -> "MAX_TOKENS"
            else -> "STOP"
        }

    /** OpenAI content is a string, or (for some servers) a list of {type:"text", text}. */
    private fun textOf(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.mapNotNull { ((it as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull }.joinToString("")
        else -> null
    }

    private fun parseArgs(raw: String?): Map<String, JsonElement> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            (json.parseToJsonElement(raw) as? JsonObject)?.toMap() ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Accumulates an SSE stream ("data: {...}" lines). Text arrives as deltas and is returned
     * immediately; tool calls arrive in fragments keyed by index and are only complete at the end.
     */
    class StreamAccumulator {
        private val text = StringBuilder()
        private val toolNames = sortedMapOf<Int, String>()
        private val toolIds = mutableMapOf<Int, String>()
        private val toolArgs = mutableMapOf<Int, StringBuilder>()
        private var finish: String? = null
        var sawChoice = false
            private set

        /** Returns the new visible text in this line ("" if none). Throws on an error payload. */
        fun accept(line: String): String {
            if (!line.startsWith("data:")) return ""
            val payload = line.removePrefix("data:").trim()
            if (payload.isEmpty() || payload == "[DONE]") return ""
            val root = json.parseToJsonElement(payload) as? JsonObject ?: return ""
            (root["error"] as? JsonObject)?.let { err ->
                throw StreamParser.StreamError((err["message"] as? JsonPrimitive)?.contentOrNull ?: "Streaming error")
            }
            val choice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return ""
            sawChoice = true
            (choice["finish_reason"] as? JsonPrimitive)?.contentOrNull?.let { finish = it }
            val delta = choice["delta"] as? JsonObject ?: return ""
            (delta["tool_calls"] as? JsonArray)?.forEach { tc ->
                val obj = tc as? JsonObject ?: return@forEach
                val index = (obj["index"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
                (obj["id"] as? JsonPrimitive)?.contentOrNull?.let { toolIds[index] = it }
                (obj["function"] as? JsonObject)?.let { fn ->
                    (fn["name"] as? JsonPrimitive)?.contentOrNull?.let { toolNames[index] = (toolNames[index] ?: "") + it }
                    (fn["arguments"] as? JsonPrimitive)?.contentOrNull?.let { toolArgs.getOrPut(index) { StringBuilder() }.append(it) }
                }
            }
            val piece = textOf(delta["content"]).orEmpty()
            text.append(piece)
            return piece
        }

        fun result(): GenerateContentResponse {
            if (!sawChoice) return GenerateContentResponse(emptyList())
            val parts = mutableListOf<Part>()
            if (text.isNotEmpty()) parts += Part(text = text.toString())
            toolNames.forEach { (index, name) ->
                parts += Part(functionCall = FunctionCall(name = name, args = parseArgs(toolArgs[index]?.toString()), id = toolIds[index]))
            }
            val reason = when (finish) {
                null -> null
                "content_filter" -> "SAFETY"
                "length" -> "MAX_TOKENS"
                else -> "STOP"
            }
            return GenerateContentResponse(listOf(Candidate(content = if (parts.isEmpty()) null else Content(parts, "model"), finishReason = reason)))
        }
    }

    /** Model ids from GET /models ({"data":[{"id":...}]}). */
    fun parseModels(root: JsonElement): List<String> =
        (((root as? JsonObject)?.get("data") ?: root) as? JsonArray)
            ?.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.contentOrNull }
            ?.distinct()
            .orEmpty()

    /**
     * True if the server refused because no model could take tools. FreeLLMAPI says so either as
     * 422 "no_tools_model" or as 429 "routing_exhausted ... lacks tool-calling" (verified live).
     */
    fun looksLikeToolsUnsupported(message: String?, errorCode: String? = null): Boolean {
        if (errorCode == "no_tools_model") return true
        val m = message?.lowercase() ?: return false
        if ("lacks tool" in m || "lack tool" in m) return true
        return ("tool" in m || "function" in m) &&
            ("support" in m || "capable" in m || "not allowed" in m || "invalid" in m || "unknown" in m)
    }
}

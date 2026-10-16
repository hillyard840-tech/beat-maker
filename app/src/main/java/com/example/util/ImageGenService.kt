package com.example.util

import android.content.Context
import com.example.api.API_KEY_HEADER
import com.example.api.ModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ImageGenResult {
    data class Success(val imagePath: String, val model: String) : ImageGenResult()
    data class Failure(val reason: String) : ImageGenResult()
}

/** Generates images with Google's Imagen API. There is no offline/placeholder fallback. */
object ImageGenService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generateImage(context: Context, prompt: String, apiKey: String): ImageGenResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext ImageGenResult.Failure("No Gemini API key is set.")

            if (ModelRegistry.imageModels.value.isEmpty()) ModelRegistry.refresh(apiKey)
            val model = ModelRegistry.imageModels.value.firstOrNull()
                ?: return@withContext ImageGenResult.Failure(
                    "No Imagen model is available to this API key. Google offers Imagen only on some (usually billing-enabled) keys."
                )

            try {
                val body = JSONObject().apply {
                    put("instances", JSONArray().put(JSONObject().put("prompt", prompt)))
                    put("parameters", JSONObject().apply {
                        put("sampleCount", 1)
                        put("aspectRatio", "1:1")
                    })
                }
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:predict")
                    .header(API_KEY_HEADER, apiKey)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val (code, text) = httpClient.newCall(request).execute().use { it.code to it.body?.string().orEmpty() }
                if (code !in 200..299) {
                    val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull()
                    return@withContext ImageGenResult.Failure("Imagen returned HTTP $code${if (!message.isNullOrBlank()) ": $message" else ""}")
                }

                val prediction = JSONObject(text).optJSONArray("predictions")?.optJSONObject(0)
                val base64 = prediction?.optString("bytesBase64Encoded").orEmpty()
                if (base64.isBlank()) {
                    // Imagen returns no image when its safety filters block the prompt.
                    return@withContext ImageGenResult.Failure("Imagen returned no image (the prompt may have been blocked by its safety filters).")
                }
                val mime = prediction?.optString("mimeType").orEmpty()
                val ext = if (mime.contains("jpeg") || mime.contains("jpg")) "jpg" else "png"
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val file = File(context.filesDir, "img_${UUID.randomUUID()}.$ext")
                FileOutputStream(file).use { it.write(bytes) }
                ImageGenResult.Success(file.absolutePath, model)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ImageGenResult.Failure("Image generation failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
}

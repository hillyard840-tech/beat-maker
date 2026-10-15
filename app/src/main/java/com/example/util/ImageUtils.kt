package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    /** Longest edge for images sent to Gemini; larger photos only add upload time and tokens. */
    private const val MAX_IMAGE_DIMENSION = 1536

    /**
     * Decodes the picked image, downscales it and re-encodes it as JPEG in internal storage.
     *
     * Re-encoding matters: the image is later sent with mimeType "image/jpeg", so copying a
     * PNG/HEIC/WebP file verbatim would send mislabeled bytes to the API.
     */
    suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmap(context, uri, MAX_IMAGE_DIMENSION) ?: return@withContext null
            val file = File(context.filesDir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation and supports HEIC.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val largest = maxOf(width, height)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    fun getBase64FromPath(path: String): String? {
        return try {
            val bytes = File(path).readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Deletes the given image files, but only if they live in the app's private storage. */
    fun deleteImages(context: Context, paths: Collection<String>) {
        val allowedRoots = listOf(context.filesDir, context.cacheDir).map { it.canonicalPath + File.separator }
        paths.forEach { path ->
            try {
                val file = File(path)
                if (allowedRoots.any { file.canonicalPath.startsWith(it) }) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

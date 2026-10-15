package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class UploadedFileInfo(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val contentSnippet: String,
    val totalCharacters: Int,
    val isTabularData: Boolean = false
)

object FileUtils {

    suspend fun processUploadedFile(context: Context, uri: Uri, maxChars: Int = 12000): UploadedFileInfo =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            var fileName = "document"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "document"
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val isAudio = mimeType.startsWith("audio/") ||
                    listOf(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac").any { fileName.endsWith(it, ignoreCase = true) }
            val isTabular = fileName.endsWith(".csv", ignoreCase = true) ||
                    fileName.endsWith(".tsv", ignoreCase = true) ||
                    mimeType.contains("csv")

            val fullTextBuilder = StringBuilder()
            if (isAudio) {
                fullTextBuilder.append("[Attached Audio File: $fileName (${formatFileSize(fileSize)}). Ready for transcription, audio memo synthesis, and conversational analysis.]")
            } else {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null && fullTextBuilder.length < maxChars) {
                                fullTextBuilder.appendLine(line)
                                line = reader.readLine()
                            }
                        }
                    }
                } catch (e: Exception) {
                    fullTextBuilder.append("Error reading file: ${e.message}")
                }
            }

            val rawContent = fullTextBuilder.toString()
            val totalChars = rawContent.length
            val snippet = if (totalChars >= maxChars) {
                rawContent.take(maxChars) + "\n\n... [File content truncated after $maxChars characters]"
            } else {
                rawContent
            }

            UploadedFileInfo(
                name = fileName,
                sizeBytes = fileSize,
                mimeType = mimeType,
                contentSnippet = snippet,
                totalCharacters = totalChars,
                isTabularData = isTabular
            )
        }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.Message
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun formatChatAsMarkdown(projectName: String?, messages: List<Message>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# Kora.Ai Conversation Export")
            appendLine("**Workspace:** ${projectName ?: "General Workspace"}")
            appendLine("**Export Date:** ${dateFormat.format(Date())}")
            appendLine("**Total Messages:** ${messages.size}")
            appendLine("---")
            appendLine()

            messages.forEach { msg ->
                val roleName = if (msg.role == "user") "### 👤 User" else "### 🤖 Kora.Ai"
                val timeStr = dateFormat.format(Date(msg.timestamp))
                appendLine("$roleName *($timeStr)*")
                if (msg.fileAttachmentName != null) {
                    appendLine("> 📎 **Attached:** ${msg.fileAttachmentName}")
                }
                appendLine()
                appendLine(msg.text)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share with Kora.Ai") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareAsMarkdownFile(context: Context, content: String, fileName: String = "kora_export.md") {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(content)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Markdown Document"))
        } catch (e: Exception) {
            // Fallback to sharing as plain text
            shareText(context, content, fileName)
        }
    }

    fun shareCanvasDocument(context: Context, title: String, content: String, language: String) {
        val ext = when (language.lowercase()) {
            "kotlin" -> "kt"
            "python" -> "py"
            "javascript" -> "js"
            "typescript" -> "ts"
            "html" -> "html"
            "json" -> "json"
            "sql" -> "sql"
            "markdown", "md" -> "md"
            else -> "txt"
        }
        val safeName = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30) + ".$ext"
        shareAsMarkdownFile(context, content, safeName)
    }
}

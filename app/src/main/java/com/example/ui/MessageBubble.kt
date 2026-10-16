package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Message
import com.example.ui.theme.status
import com.example.util.ExportUtils
import java.io.File

/**
 * One chat message. Replies render as Markdown across the full width; the user's own messages
 * sit in a bubble. Long-press opens the message actions (copy, select, share, read aloud, save).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
    onOpenSettingsTab: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onSwitchToLite: () -> Unit = {},
    onOpenInCanvas: (String, String) -> Unit = { _, _ -> },
    onModifyImage: (String) -> Unit = {},
    onToggleBookmark: () -> Unit = {},
    onReadAloud: (String) -> Unit = {}
) {
    if (message.text.startsWith("⚠️") && !isStreaming) {
        NoticeCard(message, onOpenSettingsTab, onRetry, onSwitchToLite)
        return
    }

    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    var showSelectDialog by remember { mutableStateOf(false) }

    val longPress = Modifier.combinedClickable(
        enabled = !isStreaming,
        onClick = {},
        onLongClickLabel = "Message actions",
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showMenu = true
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        message.fileAttachmentName?.let { name ->
            AssistChip(
                onClick = {},
                enabled = false,
                leadingIcon = { Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(name) },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box {
            if (isUser) {
                Surface(
                    shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                        .then(longPress)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        MessageImage(message.imagePath)
                        if (message.text.isNotBlank()) {
                            Text(message.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(longPress)
                        .padding(vertical = 2.dp)
                ) {
                    message.imagePath?.let {
                        MessageImage(it)
                        TextButton(onClick = { onModifyImage(message.text.take(40)) }) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Modify image")
                        }
                    }
                    if (message.text.isNotBlank()) {
                        MarkdownText(
                            markdown = if (isStreaming) message.text + " ▍" else message.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            onCopyCode = { code -> clipboard.setText(AnnotatedString(code)) },
                            onOpenInCanvas = onOpenInCanvas
                        )
                    }
                }
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    text = { Text("Copy") },
                    onClick = {
                        showMenu = false
                        clipboard.setText(AnnotatedString(message.text))
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                    text = { Text("Select text") },
                    onClick = {
                        showMenu = false
                        showSelectDialog = true
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    text = { Text("Share") },
                    onClick = {
                        showMenu = false
                        ExportUtils.shareText(context, message.text)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                    text = { Text("Read aloud") },
                    onClick = {
                        showMenu = false
                        onReadAloud(message.text)
                    }
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(if (message.isBookmarked) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd, contentDescription = null)
                    },
                    text = { Text(if (message.isBookmarked) "Remove from saved" else "Save message") },
                    onClick = {
                        showMenu = false
                        onToggleBookmark()
                    },
                    modifier = Modifier.testTag("bookmark_button_${message.id}")
                )
                if (!isUser) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        text = { Text("Open in Canvas") },
                        onClick = {
                            showMenu = false
                            onOpenInCanvas(message.text, "markdown")
                        }
                    )
                }
            }
        }

        if (message.isBookmarked) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.status.bookmark, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showSelectDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDialog = false },
            title = { Text("Select text") },
            text = {
                SelectionContainer(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(message.text, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = { TextButton(onClick = { showSelectDialog = false }) { Text("Done") } }
        )
    }
}

@Composable
private fun MessageImage(path: String?) {
    if (path == null) return
    AsyncImage(
        model = File(path),
        contentDescription = "Attached image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeCard(
    message: Message,
    onOpenSettingsTab: (Int) -> Unit,
    onRetry: () -> Unit,
    onSwitchToLite: () -> Unit
) {
    val body = message.text.removePrefix("⚠️").trim()
    val lower = body.lowercase()
    val aboutKey = "api key" in lower
    val aboutLimits = "rate limit" in lower || "quota" in lower

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("quota_warning_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Notice", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            MarkdownText(markdown = body, color = MaterialTheme.colorScheme.onErrorContainer)
            if (aboutKey || aboutLimits) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenSettingsTab(1) }, modifier = Modifier.testTag("enter_api_key_button")) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Enter API key")
                    }
                    if (aboutLimits) {
                        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag("retry_message_button")) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                        FilledTonalButton(onClick = onSwitchToLite, modifier = Modifier.testTag("switch_to_lite_button")) {
                            Text("Use lighter model")
                        }
                    }
                }
            }
        }
    }
}

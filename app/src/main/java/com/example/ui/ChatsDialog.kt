package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Conversation
import java.text.DateFormat
import java.util.Date

/** Lists the chats in the current workspace; pick one, start a new one, or delete one. */
@Composable
fun ChatsDialog(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Chats", fontWeight = FontWeight.Bold)
                Text(currentProject?.name ?: "General Chat", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (conversations.isEmpty()) {
                Text("No saved chats yet. Send a message to start one.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(conversations, key = { it.id }) { conversation ->
                        val isCurrent = conversation.id == currentId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectConversation(conversation.id)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    conversation.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    dateFormat.format(Date(conversation.updatedAt)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { pendingDelete = conversation }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete chat")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.newChat()
                onDismiss()
            }) { Text("New chat") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    pendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this chat?") },
            text = { Text("\"${conversation.title}\" and its attached images will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConversation(context, conversation.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

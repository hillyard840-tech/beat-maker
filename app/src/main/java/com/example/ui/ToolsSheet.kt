package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class ToolAction {
    PHOTO, DIAGNOSE, FILE, AUDIO_FILE, VOICE_NOTE, VOICE_CHAT,
    DEEP_RESEARCH, IMAGE, CANVAS, PROMPTS,
    PROJECTS, MEMORY, REMINDERS, MOLTBOOK, CLAW_EARN
}

private data class Tool(val action: ToolAction, val label: String, val icon: ImageVector)

private val SECTIONS = listOf(
    "Attach" to listOf(
        Tool(ToolAction.PHOTO, "Photo", Icons.Default.PhotoLibrary),
        Tool(ToolAction.DIAGNOSE, "Ask about a photo", Icons.Default.CameraAlt),
        Tool(ToolAction.FILE, "File", Icons.Default.AttachFile),
        Tool(ToolAction.AUDIO_FILE, "Audio file", Icons.Default.Audiotrack)
    ),
    "Voice" to listOf(
        Tool(ToolAction.VOICE_CHAT, "Voice chat", Icons.Default.GraphicEq),
        Tool(ToolAction.VOICE_NOTE, "Voice memo", Icons.Default.Mic)
    ),
    "Create" to listOf(
        Tool(ToolAction.DEEP_RESEARCH, "Deep research", Icons.Default.Science),
        Tool(ToolAction.IMAGE, "Image", Icons.Default.Palette),
        Tool(ToolAction.CANVAS, "Canvas", Icons.Default.EditNote),
        Tool(ToolAction.PROMPTS, "Prompts", Icons.Default.Lightbulb)
    ),
    "Workspace" to listOf(
        Tool(ToolAction.PROJECTS, "Projects", Icons.Default.Folder),
        Tool(ToolAction.MEMORY, "Memory", Icons.Default.Psychology),
        Tool(ToolAction.REMINDERS, "Reminders & tasks", Icons.Default.Alarm),
        Tool(ToolAction.MOLTBOOK, "Moltbook", Icons.Default.Forum),
        Tool(ToolAction.CLAW_EARN, "Claw Earn", Icons.Default.Work)
    )
)

/** Everything that used to crowd the chip row and the + menu, grouped in one sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsSheet(onDismiss: () -> Unit, onAction: (ToolAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SECTIONS.forEach { (title, tools) ->
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                tools.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tool -> ToolTile(tool, Modifier.weight(1f)) { onAction(tool.action) } }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolTile(tool: Tool, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(tool.icon, contentDescription = null) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tool.label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

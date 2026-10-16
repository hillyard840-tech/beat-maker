package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: ChatViewModel,
    onClose: () -> Unit
) {
    val title by viewModel.canvasTitle.collectAsStateWithLifecycle()
    val content by viewModel.canvasContent.collectAsStateWithLifecycle()
    val language by viewModel.canvasLanguage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isCanvasLoading.collectAsStateWithLifecycle()

    var customInstruction by remember { mutableStateOf("") }
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf(title) }
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.canvasTitle.value = titleInput
                                    isEditingTitle = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Title")
                                }
                            }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(onClick = { isEditingTitle = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Title", modifier = Modifier.size(16.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    text = language.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Canvas")
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        viewModel.exportCanvas(context)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export & Share Canvas")
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(content))
                        copied = true
                    }) {
                        Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = "Copy Content")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Canvas Live Editor Pane
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { viewModel.updateCanvasContent(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        fontFamily = if (language.lowercase() in listOf("kotlin", "python", "javascript", "json", "sql", "html", "css", "bash")) {
                            FontFamily.Monospace
                        } else {
                            FontFamily.Default
                        },
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    placeholder = {
                        Text(
                            "Start typing or ask Kora to draft your code or document...",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        )
                    }
                )
            }

            // Targeted AI Action & Assistant Bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Quick Action Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionChip(label = "🐛 Fix & Debug") {
                            viewModel.executeCanvasAiAction("Find all syntax errors, logic bugs, or edge cases and fix them.")
                        }
                        ActionChip(label = "⚡ Optimize") {
                            viewModel.executeCanvasAiAction("Optimize for performance, clarity, and clean structure.")
                        }
                        ActionChip(label = "🔄 Rewrite") {
                            viewModel.executeCanvasAiAction("Polish and rewrite for maximum conciseness and clarity.")
                        }
                        ActionChip(label = "➕ Add Tests") {
                            viewModel.executeCanvasAiAction("Add comprehensive test cases or documentation to this.")
                        }
                    }

                    // Custom Instruction Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInstruction,
                            onValueChange = { customInstruction = it },
                            placeholder = { Text("Ask AI to edit, translate, or refactor this...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (customInstruction.isNotBlank()) {
                                    viewModel.executeCanvasAiAction(customInstruction.trim())
                                    customInstruction = ""
                                }
                            },
                            enabled = customInstruction.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Run AI Action")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

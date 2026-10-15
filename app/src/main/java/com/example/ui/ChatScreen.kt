package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.api.ModelRegistry
import com.example.data.Message
import com.example.data.PromptTemplate
import com.example.util.ExportUtils
import com.example.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onDashboardClick: () -> Unit = {},
    onSeekClawClick: () -> Unit = { viewModel.openSeekClaw() },
    onMoltbookClick: () -> Unit = { viewModel.openMoltbook() }
) {
    val messages by viewModel.displayedMessages.collectAsStateWithLifecycle()
    val rawMessages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isLiveInternetEnabled by viewModel.isLiveInternetEnabled.collectAsStateWithLifecycle()
    val isAutoRoutingEnabled by viewModel.isAutoRoutingEnabled.collectAsStateWithLifecycle()
    val liveInternetStatus by viewModel.liveInternetStatus.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()
    val currentDataset by viewModel.currentDataset.collectAsStateWithLifecycle()

    // Search & Bookmarks State
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isBookmarkFilterActive by viewModel.isBookmarkFilterActive.collectAsStateWithLifecycle()

    // Persona & Prompt Templates
    val showPromptTemplates by viewModel.showPromptTemplates.collectAsStateWithLifecycle()
    val activePersona by viewModel.activePersona.collectAsStateWithLifecycle()
    val pendingInputText by viewModel.pendingInputText.collectAsStateWithLifecycle()

    // Voice Note Recorder
    val isVoiceNoteRecording by viewModel.isVoiceNoteRecording.collectAsStateWithLifecycle()
    val voiceNoteDurationSec by viewModel.voiceNoteDurationSec.collectAsStateWithLifecycle()
    val voiceNoteLiveTranscript by viewModel.voiceNoteLiveTranscript.collectAsStateWithLifecycle()
    val attachedVoiceNote by viewModel.attachedVoiceNote.collectAsStateWithLifecycle()

    // Dialog & Overlay controls
    val showMemoryDialog by viewModel.showMemoryDialog.collectAsStateWithLifecycle()
    val showProjectsDialog by viewModel.showProjectsDialog.collectAsStateWithLifecycle()
    val showDataAnalysisSheet by viewModel.showDataAnalysisSheet.collectAsStateWithLifecycle()
    val showImageGenDialog by viewModel.showImageGenDialog.collectAsStateWithLifecycle()
    val isDeepResearchOpen by viewModel.isDeepResearchOpen.collectAsStateWithLifecycle()
    val operationalState by viewModel.operationalState.collectAsStateWithLifecycle()
    val areBackgroundTasksPaused by viewModel.areBackgroundTasksPaused.collectAsStateWithLifecycle()
    val isLearningModeEnabled by viewModel.isLearningModeEnabled.collectAsStateWithLifecycle()

    var inputFieldState by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showModelSettingsDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableIntStateOf(0) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showOperationalStateDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Sync pending input text (from template insertion)
    LaunchedEffect(pendingInputText) {
        pendingInputText?.let {
            inputFieldState = TextFieldValue(it, selection = TextRange(it.length))
            viewModel.pendingInputText.value = null
        }
    }

    val onSendAction: () -> Unit = {
        val trimmedText = inputFieldState.text.trim()
        if (trimmedText.isNotEmpty() || selectedImageUri != null || attachedFile != null || attachedVoiceNote != null) {
            inputFieldState = TextFieldValue("", selection = TextRange.Zero)
            keyboardController?.hide()
            focusManager.clearFocus(force = false)
            // Image copy/compression happens off the main thread inside the ViewModel.
            viewModel.sendMessageWithImage(trimmedText, selectedImageUri)
            selectedImageUri = null
        }
    }

    // Media Picker (Images)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Audio File Picker
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    // Audio Permission Request
    val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceNoteRecording()
        }
    }

    // File Document Picker (PDF, CSV, TXT, Code)
    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.attachFile(context, uri)
        }
    }

    // Dialogs
    if (showModelSettingsDialog) {
        ModelSettingsDialog(
            viewModel = viewModel,
            initialTab = settingsInitialTab,
            onDismiss = { showModelSettingsDialog = false }
        )
    }

    if (showMemoryDialog) {
        MemoryDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showMemoryDialog.value = false }
        )
    }

    if (showProjectsDialog) {
        ProjectsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showProjectsDialog.value = false }
        )
    }

    if (showImageGenDialog) {
        ImageGenDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showImageGenDialog.value = false }
        )
    }

    if (isDeepResearchOpen) {
        DeepResearchDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isDeepResearchOpen.value = false }
        )
    }

    if (showDataAnalysisSheet) {
        DataAnalysisSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showDataAnalysisSheet.value = false }
        )
    }

    if (showDiagnosticDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            title = { Text("Camera & Visual Diagnostics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a visual inspection scenario:", style = MaterialTheme.typography.bodyMedium)
                    listOf(
                        "🔍 What's wrong? (Diagnose issue)",
                        "📐 Analyze Schematic / Diagram",
                        "📝 Read & Summarize Document",
                        "💻 Explain Error / Code Screenshot",
                        "🧾 Parse Receipt / Table Numbers"
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inputFieldState = TextFieldValue(preset)
                                    showDiagnosticDialog = false
                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                        ) {
                            Text(text = preset, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPromptTemplates) {
        PromptTemplatesDialog(
            onDismiss = { viewModel.closePromptTemplates() },
            onSelectTemplate = { template ->
                viewModel.useTemplatePrompt(template)
            },
            onApplyPersona = { template ->
                viewModel.applyPersona(template)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.clickable { viewModel.showProjectsDialog.value = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Kora.Ai", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = currentProject?.name ?: "General Chat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Visual Operational State Indicator (Idle, Learning Mode, Researching, etc.)
                        OperationalStateBadge(
                            state = operationalState,
                            onClick = { showOperationalStateDialog = true }
                        )
                    }
                },
                actions = {
                    // Quick-Toggle Switch to Pause / Resume Background Tasks
                    BackgroundTasksQuickToggle(
                        areTasksPaused = areBackgroundTasksPaused,
                        onToggle = { isPaused ->
                            viewModel.toggleBackgroundTasks(context, paused = isPaused)
                        },
                        onClickDetails = { showOperationalStateDialog = true }
                    )

                    // Search & Bookmarks toggle
                    IconButton(
                        onClick = { viewModel.toggleSearch(!isSearchOpen) },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Messages",
                            tint = if (isSearchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Export & Share Dropdown Menu
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("export_menu_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export & Share Chat")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                text = { Text("Export Chat as Markdown (.md)") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportChatAsMarkdown(context)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                text = { Text("Share Chat as Plain Text") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportChatAsText(context)
                                }
                            )
                        }
                    }

                    // Voice Mode button
                    IconButton(
                        onClick = { viewModel.isVoiceModeOpen.value = true },
                        modifier = Modifier.testTag("voice_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Persistent Memory shortcut with count badge
                    IconButton(
                        onClick = { viewModel.showMemoryDialog.value = true },
                        modifier = Modifier.testTag("memory_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (memories.isNotEmpty()) {
                                    Badge { Text("${memories.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "Memory", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Model & Settings button
                    IconButton(
                        onClick = {
                            settingsInitialTab = 0
                            showModelSettingsDialog = true
                        },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }

                    IconButton(
                        onClick = onDashboardClick,
                        modifier = Modifier.testTag("dashboard_button")
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = "Dashboard")
                    }

                    IconButton(
                        onClick = { viewModel.clearHistory(context) },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search & Bookmarks In-Chat Filter Bar
            AnimatedVisibility(visible = isSearchOpen) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search messages, code, facts...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            )
                        )
                        FilterChip(
                            selected = isBookmarkFilterActive,
                            onClick = { viewModel.toggleBookmarkFilter() },
                            label = { Text("⭐ Saved", fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { viewModel.toggleSearch(false) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Active Persona Banner
            AnimatedVisibility(visible = activePersona != null) {
                activePersona?.let { persona ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(persona.iconEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Active Persona: ${persona.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    persona.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearPersona() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove Persona",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Horizontal Feature Chips Bar
            val chipsScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScroll)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Autonomous Intent Detection Toggle Chip
                FilterChip(
                    selected = isAutoRoutingEnabled,
                    onClick = { viewModel.toggleAutoRouting(!isAutoRoutingEnabled) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isAutoRoutingEnabled) Color(0xFF673AB7) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("⚡ Auto", fontSize = 12.sp, fontWeight = if (isAutoRoutingEnabled) FontWeight.Bold else FontWeight.Normal) }
                )

                // Live Web Toggle Chip
                FilterChip(
                    selected = isLiveInternetEnabled,
                    onClick = { viewModel.toggleLiveInternet(!isLiveInternetEnabled) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            tint = if (isLiveInternetEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Live Web", fontSize = 12.sp) }
                )

                // Moltbook AI Agent Social Network Chip
                AssistChip(
                    onClick = onMoltbookClick,
                    leadingIcon = { Text("🦞", fontSize = 14.sp) },
                    label = { Text("Moltbook", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )

                // SeekClaw Agent Marketplace Chip
                AssistChip(
                    onClick = onSeekClawClick,
                    leadingIcon = { Text("💼", fontSize = 14.sp) },
                    label = { Text("SeekClaw", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )

                // Prompt Templates & Personas
                AssistChip(
                    onClick = { viewModel.openPromptTemplates() },
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("⚡ Prompts", fontSize = 12.sp) }
                )

                // Deep Research
                AssistChip(
                    onClick = { viewModel.isDeepResearchOpen.value = true },
                    leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("🔬 Deep Research", fontSize = 12.sp) }
                )

                // Interactive Canvas
                AssistChip(
                    onClick = { viewModel.openCanvas() },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("📝 Canvas", fontSize = 12.sp) }
                )

                // Image Generation
                AssistChip(
                    onClick = { viewModel.showImageGenDialog.value = true },
                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("🎨 Image Gen", fontSize = 12.sp) }
                )

                // Project Workspaces
                AssistChip(
                    onClick = { viewModel.showProjectsDialog.value = true },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("🗂️ Projects", fontSize = 12.sp) }
                )
            }

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                if (messages.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isBookmarkFilterActive) Icons.Default.Bookmark else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isBookmarkFilterActive) "No bookmarked messages" else if (searchQuery.isNotBlank()) "No messages match \"$searchQuery\"" else "Start a new conversation with Kora.Ai",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        onOpenSettingsTab = { tab ->
                            settingsInitialTab = tab
                            showModelSettingsDialog = true
                        },
                        onRetry = { viewModel.retryLastUserMessage() },
                        onSwitchToLite = { viewModel.switchToLiteModel() },
                        onOpenInCanvas = { code, lang ->
                            viewModel.openCanvas(
                                title = "Code Workspace",
                                content = code,
                                language = lang
                            )
                        },
                        onModifyImage = { prompt ->
                            inputFieldState = TextFieldValue("Modify image: $prompt ")
                        },
                        onToggleBookmark = {
                            viewModel.toggleBookmark(message)
                        }
                    )
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Selected Image Preview
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Attached Document / Dataset / Audio Pill
                    if (attachedFile != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (attachedFile!!.isTabularData) Icons.Default.BarChart else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attachedFile!!.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = FileUtils.formatFileSize(attachedFile!!.sizeBytes),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                if (attachedFile!!.isTabularData && currentDataset != null) {
                                    TextButton(
                                        onClick = { viewModel.showDataAnalysisSheet.value = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("📊 Analyze", fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeAttachedFile() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove File", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Attached Voice Memo Pill
                    if (attachedVoiceNote != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "🎙️ Voice Memo (${attachedVoiceNote!!.durationSec}s)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (attachedVoiceNote!!.transcript.isNotBlank()) {
                                        Text(
                                            "\"${attachedVoiceNote!!.transcript}\"",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.removeAttachedVoiceNote() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Voice Memo", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Live Voice Recording Indicator Banner
                    AnimatedVisibility(visible = isVoiceNoteRecording) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "🔴 Recording Voice Memo (${voiceNoteDurationSec}s)...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (voiceNoteLiveTranscript.isNotBlank()) {
                                        Text(
                                            voiceNoteLiveTranscript,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { viewModel.cancelVoiceNoteRecording() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.stopVoiceNoteRecording() },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Attach", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Live Web Status Ribbon
                    AnimatedVisibility(visible = liveInternetStatus != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveInternetStatus ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Input Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { showAttachmentMenu = true },
                                modifier = Modifier.testTag("add_attachment_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Attachment")
                            }

                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
                                    text = { Text("🎙️ Record Voice Note") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                                    text = { Text("🎵 Upload Audio File") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickAudio.launch("audio/*")
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                    text = { Text("Photos & Images") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                    text = { Text("Camera & Diagnostics") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        showDiagnosticDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                    text = { Text("Upload File (PDF, CSV, TXT)") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        pickDocument.launch(arrayOf("*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                                    text = { Text("⚡ Prompt Templates & Personas") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        viewModel.openPromptTemplates()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                    text = { Text("Generate Image") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        viewModel.showImageGenDialog.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Science, contentDescription = null) },
                                    text = { Text("Deep Research") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        viewModel.isDeepResearchOpen.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                    text = { Text("Open Canvas") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        viewModel.openCanvas()
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = inputFieldState,
                            onValueChange = { inputFieldState = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("message_input"),
                            placeholder = { Text("Ask Kora anything, brainstorm, code...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (!isLoading) {
                                    onSendAction()
                                }
                            })
                        )

                        IconButton(
                            onClick = onSendAction,
                            enabled = (inputFieldState.text.isNotBlank() || selectedImageUri != null || attachedFile != null || attachedVoiceNote != null) && !isLoading,
                            modifier = Modifier.testTag("send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Operational State & Background Tasks Details Dialog
        if (showOperationalStateDialog) {
            OperationalStateDialog(
                state = operationalState,
                areTasksPaused = areBackgroundTasksPaused,
                isLearningModeEnabled = isLearningModeEnabled,
                onToggleTasks = { isPaused ->
                    viewModel.toggleBackgroundTasks(context, paused = isPaused)
                },
                onDismiss = { showOperationalStateDialog = false }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onOpenSettingsTab: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onSwitchToLite: () -> Unit = {},
    onOpenInCanvas: (String, String) -> Unit = { _, _ -> },
    onModifyImage: (String) -> Unit = {},
    onToggleBookmark: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val isWarning = message.text.startsWith("⚠️")
    val clipboardManager = LocalClipboardManager.current

    if (isWarning) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("quota_warning_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "API Notice / Quota Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message.text.removePrefix("⚠️").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onOpenSettingsTab(1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("enter_api_key_button")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enter API Key", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("retry_message_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = onSwitchToLite,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("switch_to_lite_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flash Lite", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        return
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // User File Attachment Label if present
        if (message.fileAttachmentName != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = message.fileAttachmentName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.imagePath != null) {
                    AsyncImage(
                        model = File(message.imagePath),
                        contentDescription = "Message Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(bottom = if (message.text.isNotBlank()) 8.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                    if (!isUser) {
                        TextButton(
                            onClick = { onModifyImage(message.text.take(30)) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modify Image", fontSize = 11.sp)
                        }
                    }
                }

                if (message.text.isNotBlank()) {
                    // Check for code blocks
                    val codeBlockRegex = Regex("""```([a-zA-Z0-9_\-#+]*)\n([\s\S]*?)```""")
                    val hasCodeBlocks = codeBlockRegex.containsMatchIn(message.text)

                    if (hasCodeBlocks && !isUser) {
                        RenderRichTextWithCode(
                            text = message.text,
                            textColor = textColor,
                            onCopyCode = { code -> clipboardManager.setText(AnnotatedString(code)) },
                            onOpenInCanvas = onOpenInCanvas
                        )
                    } else {
                        Text(text = message.text, color = textColor)
                    }

                    // Message Action Row (Bookmark, Copy, Share)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current

                        // Bookmark
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("bookmark_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = if (message.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (message.isBookmarked) "Saved to Bookmarks" else "Save Bookmark",
                                tint = if (message.isBookmarked) Color(0xFFFFB300) else textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Copy
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(message.text)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = { ExportUtils.shareText(context, message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share message",
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderRichTextWithCode(
    text: String,
    textColor: Color,
    onCopyCode: (String) -> Unit,
    onOpenInCanvas: (String, String) -> Unit
) {
    val codeBlockRegex = Regex("""```([a-zA-Z0-9_\-#+]*)\n([\s\S]*?)```""")
    var lastIndex = 0

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        codeBlockRegex.findAll(text).forEach { match ->
            val preText = text.substring(lastIndex, match.range.first).trim()
            if (preText.isNotEmpty()) {
                Text(text = preText, color = textColor)
            }

            val lang = match.groupValues[1].ifBlank { "code" }
            val code = match.groupValues[2].trimEnd()

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            TextButton(
                                onClick = { onCopyCode(code) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = { onOpenInCanvas(code, lang) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Canvas", fontSize = 11.sp)
                            }
                        }
                    }
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            val postText = text.substring(lastIndex).trim()
            if (postText.isNotEmpty()) {
                Text(text = postText, color = textColor)
            }
        }
    }
}

@Composable
fun OperationalStateBadge(
    state: AgentOperationalState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (badgeBg, dotColor) = when (state) {
        AgentOperationalState.IDLE -> Pair(
            Color(0xFF4CAF50).copy(alpha = 0.12f),
            Color(0xFF2E7D32)
        )
        AgentOperationalState.LEARNING_MODE -> Pair(
            Color(0xFF00C853).copy(alpha = 0.18f),
            Color(0xFF00C853)
        )
        AgentOperationalState.RESEARCHING -> Pair(
            Color(0xFF00B0FF).copy(alpha = 0.18f),
            Color(0xFF0288D1)
        )
        AgentOperationalState.GENERATING -> Pair(
            Color(0xFFFFB300).copy(alpha = 0.18f),
            Color(0xFFFF8F00)
        )
        AgentOperationalState.TASKS_PAUSED -> Pair(
            Color(0xFFFF9800).copy(alpha = 0.18f),
            Color(0xFFE65100)
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeBg,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("operational_state_indicator")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        dotColor.copy(alpha = if (state.isPulsing) pulseAlpha else 1.0f)
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = state.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = dotColor,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BackgroundTasksQuickToggle(
    areTasksPaused: Boolean,
    onToggle: (Boolean) -> Unit,
    onClickDetails: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (areTasksPaused) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (areTasksPaused) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.padding(end = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Icon(
                imageVector = if (areTasksPaused) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                contentDescription = if (areTasksPaused) "Tasks Paused" else "Tasks Running",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClickDetails() },
                tint = if (areTasksPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (areTasksPaused) "Paused" else "Tasks",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (areTasksPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onClickDetails() }
            )
            Spacer(modifier = Modifier.width(2.dp))
            Switch(
                checked = !areTasksPaused,
                onCheckedChange = { isActive ->
                    onToggle(!isActive)
                },
                modifier = Modifier
                    .scale(0.7f)
                    .testTag("background_tasks_toggle"),
                thumbContent = {
                    Icon(
                        imageVector = if (!areTasksPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun OperationalStateDialog(
    state: AgentOperationalState,
    areTasksPaused: Boolean,
    isLearningModeEnabled: Boolean,
    onToggleTasks: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val (dotColor, iconVector) = when (state) {
        AgentOperationalState.IDLE -> Pair(Color(0xFF2E7D32), Icons.Default.CheckCircle)
        AgentOperationalState.LEARNING_MODE -> Pair(Color(0xFF00C853), Icons.Default.Psychology)
        AgentOperationalState.RESEARCHING -> Pair(Color(0xFF0288D1), Icons.Default.TravelExplore)
        AgentOperationalState.GENERATING -> Pair(Color(0xFFFF8F00), Icons.Default.AutoAwesome)
        AgentOperationalState.TASKS_PAUSED -> Pair(Color(0xFFE65100), Icons.Default.PauseCircle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text("Agent Operational Status", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "State: ${state.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (areTasksPaused) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (areTasksPaused) "Background Tasks: Paused" else "Background Tasks: Active",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (areTasksPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (areTasksPaused) "Periodic research and updates suspended" else "Autonomous background routines executing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !areTasksPaused,
                            onCheckedChange = { isActive ->
                                onToggleTasks(!isActive)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Active Subsystems",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔋 Learning Worker:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (areTasksPaused) "Paused" else if (isLearningModeEnabled) "Active (3h cadence)" else "Disabled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (areTasksPaused) Color(0xFFE65100) else if (isLearningModeEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦞 Moltbook Agent Sync:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (areTasksPaused) "Paused" else "Online (30m heartbeat)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (areTasksPaused) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💼 SeekClaw Marketplace:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (areTasksPaused) "Paused" else "Online (Escrow Guarded)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (areTasksPaused) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

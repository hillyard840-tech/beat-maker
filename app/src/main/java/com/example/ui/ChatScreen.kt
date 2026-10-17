package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.AgentAction
import com.example.data.Message
import com.example.ui.theme.status
import com.example.util.ChatTimeline
import com.example.util.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onDashboardClick: () -> Unit = {},
    onClawEarnClick: () -> Unit = { viewModel.openClawEarn() },
    onMoltbookClick: () -> Unit = { viewModel.openMoltbook() }
) {
    val messages by viewModel.displayedMessages.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val isLiveInternetEnabled by viewModel.isLiveInternetEnabled.collectAsStateWithLifecycle()
    val isAutoRoutingEnabled by viewModel.isAutoRoutingEnabled.collectAsStateWithLifecycle()
    val liveInternetStatus by viewModel.liveInternetStatus.collectAsStateWithLifecycle()
    val attachedFile by viewModel.attachedFile.collectAsStateWithLifecycle()
    val currentDataset by viewModel.currentDataset.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()

    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isBookmarkFilterActive by viewModel.isBookmarkFilterActive.collectAsStateWithLifecycle()

    val showPromptTemplates by viewModel.showPromptTemplates.collectAsStateWithLifecycle()
    val activePersona by viewModel.activePersona.collectAsStateWithLifecycle()
    val pendingInputText by viewModel.pendingInputText.collectAsStateWithLifecycle()

    val isVoiceNoteRecording by viewModel.isVoiceNoteRecording.collectAsStateWithLifecycle()
    val voiceNoteDurationSec by viewModel.voiceNoteDurationSec.collectAsStateWithLifecycle()
    val voiceNoteLiveTranscript by viewModel.voiceNoteLiveTranscript.collectAsStateWithLifecycle()
    val attachedVoiceNote by viewModel.attachedVoiceNote.collectAsStateWithLifecycle()

    val showMemoryDialog by viewModel.showMemoryDialog.collectAsStateWithLifecycle()
    val showProjectsDialog by viewModel.showProjectsDialog.collectAsStateWithLifecycle()
    val showDataAnalysisSheet by viewModel.showDataAnalysisSheet.collectAsStateWithLifecycle()
    val showImageGenDialog by viewModel.showImageGenDialog.collectAsStateWithLifecycle()
    val isDeepResearchOpen by viewModel.isDeepResearchOpen.collectAsStateWithLifecycle()
    val operationalState by viewModel.operationalState.collectAsStateWithLifecycle()
    val areBackgroundTasksPaused by viewModel.areBackgroundTasksPaused.collectAsStateWithLifecycle()

    var inputFieldState by remember { mutableStateOf(TextFieldValue("")) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showModelSettingsDialog by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableIntStateOf(0) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showOperationalStateDialog by remember { mutableStateOf(false) }
    var showChatsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun openSettings(tab: Int) {
        settingsInitialTab = tab
        showModelSettingsDialog = true
    }

    // Widget "camera" button: open the visual-diagnostics picker.
    val visualDiagnosticsRequested by viewModel.showVisualDiagnostics.collectAsStateWithLifecycle()
    LaunchedEffect(visualDiagnosticsRequested) {
        if (visualDiagnosticsRequested) {
            showDiagnosticDialog = true
            viewModel.showVisualDiagnostics.value = false
        }
    }

    // Template insertion fills the input box.
    LaunchedEffect(pendingInputText) {
        pendingInputText?.let {
            inputFieldState = TextFieldValue(it, selection = TextRange(it.length))
            viewModel.pendingInputText.value = null
        }
    }

    val canSend = inputFieldState.text.isNotBlank() || selectedImageUri != null || attachedFile != null || attachedVoiceNote != null
    val onSendAction: () -> Unit = {
        if (canSend && !isLoading) {
            val trimmedText = inputFieldState.text.trim()
            inputFieldState = TextFieldValue("", selection = TextRange.Zero)
            keyboardController?.hide()
            focusManager.clearFocus(force = false)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            // Image copy/compression happens off the main thread inside the ViewModel.
            viewModel.sendMessageWithImage(trimmedText, selectedImageUri)
            selectedImageUri = null
        }
    }

    // ---------- Pickers & permissions ----------
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImageUri = uri
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.attachFile(context, uri)
    }
    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.attachFile(context, uri)
    }
    val requestAudioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startVoiceNoteRecording()
    }

    // ---------- Dialogs ----------
    if (showModelSettingsDialog) {
        ModelSettingsDialog(viewModel = viewModel, initialTab = settingsInitialTab, onDismiss = { showModelSettingsDialog = false })
    }
    if (showMemoryDialog) MemoryDialog(viewModel = viewModel, onDismiss = { viewModel.showMemoryDialog.value = false })
    if (showProjectsDialog) ProjectsDialog(viewModel = viewModel, onDismiss = { viewModel.showProjectsDialog.value = false })
    if (showImageGenDialog) ImageGenDialog(viewModel = viewModel, onDismiss = { viewModel.showImageGenDialog.value = false })
    if (isDeepResearchOpen) DeepResearchDialog(viewModel = viewModel, onDismiss = { viewModel.isDeepResearchOpen.value = false })
    if (showDataAnalysisSheet) DataAnalysisSheet(viewModel = viewModel, onDismiss = { viewModel.showDataAnalysisSheet.value = false })
    if (showChatsDialog) ChatsDialog(viewModel = viewModel, onDismiss = { showChatsDialog = false })
    if (showPromptTemplates) {
        PromptTemplatesDialog(
            onDismiss = { viewModel.closePromptTemplates() },
            onSelectTemplate = { template -> viewModel.useTemplatePrompt(template) },
            onApplyPersona = { template -> viewModel.applyPersona(template) }
        )
    }
    if (showDiagnosticDialog) {
        VisualDiagnosticsDialog(
            onDismiss = { showDiagnosticDialog = false },
            onPick = { preset ->
                inputFieldState = TextFieldValue(preset, selection = TextRange(preset.length))
                showDiagnosticDialog = false
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
    if (showOperationalStateDialog) {
        OperationalStateDialog(
            state = operationalState,
            areTasksPaused = areBackgroundTasksPaused,
            onToggleTasks = { isPaused -> viewModel.toggleBackgroundTasks(paused = isPaused) },
            onOpenReminders = {
                showOperationalStateDialog = false
                viewModel.isRemindersOpen.value = true
            },
            onDismiss = { showOperationalStateDialog = false }
        )
    }
    if (showToolsSheet) {
        ToolsSheet(
            onDismiss = { showToolsSheet = false },
            onAction = { action ->
                showToolsSheet = false
                when (action) {
                    ToolAction.PHOTO -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    ToolAction.DIAGNOSE -> showDiagnosticDialog = true
                    ToolAction.FILE -> pickDocument.launch(arrayOf("*/*"))
                    ToolAction.AUDIO_FILE -> pickAudio.launch("audio/*")
                    ToolAction.VOICE_NOTE -> requestAudioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    ToolAction.VOICE_CHAT -> viewModel.startVoiceChat()
                    ToolAction.DEEP_RESEARCH -> viewModel.isDeepResearchOpen.value = true
                    ToolAction.IMAGE -> viewModel.showImageGenDialog.value = true
                    ToolAction.CANVAS -> viewModel.openCanvas()
                    ToolAction.PROMPTS -> viewModel.openPromptTemplates()
                    ToolAction.PROJECTS -> viewModel.showProjectsDialog.value = true
                    ToolAction.MEMORY -> viewModel.showMemoryDialog.value = true
                    ToolAction.REMINDERS -> viewModel.isRemindersOpen.value = true
                    ToolAction.MOLTBOOK -> onMoltbookClick()
                    ToolAction.CLAW_EARN -> onClawEarnClick()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                viewModel = viewModel,
                onOpenChats = { showChatsDialog = true },
                onOpenStatus = { showOperationalStateDialog = true },
                onOpenSettings = { openSettings(0) },
                onOpenDashboard = onDashboardClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            // Search bar
            AnimatedVisibility(visible = isSearchOpen) {
                SearchBarRow(
                    query = searchQuery,
                    bookmarksOnly = isBookmarkFilterActive,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleBookmarks = { viewModel.toggleBookmarkFilter() },
                    onClose = { viewModel.toggleSearch(false) }
                )
            }

            // ---------- Message list ----------
            val listState = rememberLazyListState()
            val isAtBottom by remember {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 2
                }
            }
            // Follow new messages and the streaming reply, but don't yank the list while the user
            // is reading older messages (unless they just sent something).
            val lastIsUser = messages.lastOrNull()?.role == "user"
            LaunchedEffect(messages.size, actions.size, streamingText?.length) {
                val total = listState.layoutInfo.totalItemsCount
                if (total > 0 && (isAtBottom || lastIsUser)) listState.scrollToItem(total - 1)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Messages and action cards in time order. Cards are hidden while searching.
                val filtering = isSearchOpen && (searchQuery.isNotBlank() || isBookmarkFilterActive)
                val timeline = remember(messages, actions, filtering) {
                    val items = messages.map { TimelineEntry(it.timestamp, message = it) } +
                        (if (filtering) emptyList() else actions.map { TimelineEntry(it.createdAt, action = it) })
                    items.sortedBy { it.timestamp }
                }
                val showEmpty = timeline.isEmpty() && streamingText == null && !isLoading
                if (showEmpty) {
                    if (isSearchOpen && (searchQuery.isNotBlank() || isBookmarkFilterActive)) {
                        Text(
                            text = if (isBookmarkFilterActive) "No saved messages" else "No messages match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        EmptyChatState(
                            hasApiKey = hasApiKey,
                            onAddApiKey = { openSettings(1) },
                            onStarter = { prompt ->
                                inputFieldState = TextFieldValue(prompt, selection = TextRange(prompt.length))
                            },
                            onVoiceChat = { viewModel.startVoiceChat() }
                        )
                    }
                }

                if (!showEmpty) LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(timeline, key = { _, e -> e.key }) { index, entry ->
                        val previous = timeline.getOrNull(index - 1)
                        if (ChatTimeline.needsSeparator(previous?.timestamp, entry.timestamp)) {
                            TimeSeparator(ChatTimeline.label(entry.timestamp))
                        }
                        entry.action?.let { action ->
                            ActionCard(
                                action = action,
                                onConfirm = { viewModel.confirmAction(it) },
                                onCancel = { viewModel.cancelAction(it) },
                                onCalendarPermission = { a, granted -> viewModel.onCalendarPermissionResult(a, granted) }
                            )
                        }
                        val message = entry.message ?: return@itemsIndexed
                        MessageBubble(
                            message = message,
                            onOpenSettingsTab = { tab -> openSettings(tab) },
                            onRetry = { viewModel.retryLastUserMessage() },
                            onSwitchToLite = { viewModel.switchToLiteModel() },
                            onOpenInCanvas = { code, lang -> viewModel.openCanvas(title = "Code Workspace", content = code, language = lang) },
                            onModifyImage = { prompt -> inputFieldState = TextFieldValue("Modify image: $prompt ") },
                            onToggleBookmark = { viewModel.toggleBookmark(message) },
                            onReadAloud = { text -> viewModel.readAloud(text) }
                        )
                    }
                    streamingText?.let { partial ->
                        item(key = "streaming_reply") {
                            MessageBubble(message = Message(role = "model", text = partial), isStreaming = true)
                        }
                    }
                    if (isLoading && streamingText == null) {
                        item(key = "thinking") { ThinkingIndicator(status = liveInternetStatus) }
                    }
                }

                // Jump to latest
                JumpToLatestButton(
                    visible = !isAtBottom && messages.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            val total = listState.layoutInfo.totalItemsCount
                            if (total > 0) listState.animateScrollToItem(total - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }

            // ---------- Input area ----------
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Live web status while a turn is running and something is already streaming
                    if (liveInternetStatus != null && streamingText != null) {
                        StatusRibbon(text = liveInternetStatus ?: "")
                    }

                    // Persona
                    activePersona?.let { persona ->
                        InputPill(
                            icon = { Text(persona.iconEmoji) },
                            title = "Persona: ${persona.title}",
                            subtitle = null,
                            onRemove = { viewModel.clearPersona() },
                            removeLabel = "Remove persona"
                        )
                    }

                    // Selected image
                    selectedImageUri?.let { uri ->
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            FilledTonalIconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.align(Alignment.TopEnd).size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Attached document / dataset
                    attachedFile?.let { file ->
                        InputPill(
                            icon = { Icon(if (file.isTabularData) Icons.Default.BarChart else Icons.Default.Description, contentDescription = null) },
                            title = file.name,
                            subtitle = FileUtils.formatFileSize(file.sizeBytes),
                            onRemove = { viewModel.removeAttachedFile() },
                            removeLabel = "Remove file",
                            actionLabel = if (file.isTabularData && currentDataset != null) "Analyze" else null,
                            onAction = { viewModel.showDataAnalysisSheet.value = true }
                        )
                    }

                    // Attached voice memo
                    attachedVoiceNote?.let { note ->
                        InputPill(
                            icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                            title = "Voice memo (${note.durationSec}s)",
                            subtitle = note.transcript.takeIf { it.isNotBlank() }?.let { "\"$it\"" },
                            onRemove = { viewModel.removeAttachedVoiceNote() },
                            removeLabel = "Remove voice memo"
                        )
                    }

                    // Recording banner
                    AnimatedVisibility(visible = isVoiceNoteRecording) {
                        RecordingBanner(
                            seconds = voiceNoteDurationSec,
                            transcript = voiceNoteLiveTranscript,
                            onCancel = { viewModel.cancelVoiceNoteRecording() },
                            onAttach = { viewModel.stopVoiceNoteRecording() }
                        )
                    }

                    // The two real toggles stay one tap away; everything else lives in the Tools sheet.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isAutoRoutingEnabled,
                            onClick = { viewModel.toggleAutoRouting(!isAutoRoutingEnabled) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text("Auto tools") }
                        )
                        FilterChip(
                            selected = isLiveInternetEnabled,
                            onClick = { viewModel.toggleLiveInternet(!isLiveInternetEnabled) },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text("Live web") }
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = { showToolsSheet = true }, modifier = Modifier.testTag("add_attachment_button")) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Attach and tools")
                        }

                        OutlinedTextField(
                            value = inputFieldState,
                            onValueChange = { inputFieldState = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .testTag("message_input"),
                            placeholder = { Text("Message Kora") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendAction() })
                        )

                        when {
                            isLoading -> FilledIconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.stopGeneration()
                                },
                                modifier = Modifier.testTag("stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop generating")
                            }
                            canSend -> FilledIconButton(
                                onClick = onSendAction,
                                modifier = Modifier.testTag("send_message_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                            else -> FilledTonalIconButton(
                                onClick = { viewModel.startVoiceChat() },
                                modifier = Modifier.testTag("voice_chat_button")
                            ) {
                                Icon(Icons.Default.GraphicEq, contentDescription = "Start voice chat")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    viewModel: ChatViewModel,
    onOpenChats: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    val context = LocalContext.current
    val chatTitle by viewModel.currentConversationTitle.collectAsStateWithLifecycle()
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val operationalState by viewModel.operationalState.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme") },
            text = {
                Column {
                    listOf(
                        ChatViewModel.THEME_SYSTEM to "Match system",
                        ChatViewModel.THEME_LIGHT to "Light",
                        ChatViewModel.THEME_DARK to "Dark"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Done") } }
        )
    }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenChats, modifier = Modifier.testTag("chats_button")) {
                Icon(Icons.Default.Menu, contentDescription = "Chats")
            }
        },
        title = {
            Column {
                Text(chatTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.showProjectsDialog.value = true }
                ) {
                    if (operationalState != AgentOperationalState.IDLE) {
                        StatusDot(operationalState, modifier = Modifier.clickable(onClick = onOpenStatus))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = currentProject?.name ?: "General",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch workspace", modifier = Modifier.size(18.dp))
                }
            }
        },
        actions = {
            IconButton(onClick = { viewModel.toggleSearch(!isSearchOpen) }, modifier = Modifier.testTag("search_button")) {
                Icon(Icons.Default.Search, contentDescription = "Search messages")
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("overflow_menu_button")) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    MenuItem(Icons.Default.AddComment, "New chat") { showMenu = false; viewModel.newChat() }
                    MenuItem(Icons.Default.GraphicEq, "Voice chat") { showMenu = false; viewModel.startVoiceChat() }
                    HorizontalDivider()
                    MenuItem(
                        Icons.Default.Psychology,
                        if (memories.isEmpty()) "Memory" else "Memory (${memories.size})"
                    ) { showMenu = false; viewModel.showMemoryDialog.value = true }
                    MenuItem(Icons.Default.Insights, "Dashboard") { showMenu = false; onOpenDashboard() }
                    MenuItem(Icons.Default.Alarm, "Reminders & tasks") { showMenu = false; viewModel.isRemindersOpen.value = true }
                    MenuItem(Icons.Default.Tune, "Background tasks") { showMenu = false; onOpenStatus() }
                    MenuItem(Icons.Default.Settings, "Settings") { showMenu = false; onOpenSettings() }
                    MenuItem(Icons.Default.DarkMode, "Theme") { showMenu = false; showThemeDialog = true }
                    HorizontalDivider()
                    MenuItem(Icons.Default.Description, "Export as Markdown") { showMenu = false; viewModel.exportChatAsMarkdown(context) }
                    MenuItem(Icons.Default.Share, "Share as text") { showMenu = false; viewModel.exportChatAsText(context) }
                    MenuItem(Icons.Default.Delete, "Delete this chat") { showMenu = false; viewModel.requestClearHistory() }
                }
            }
        }
    )
}

@Composable
private fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(leadingIcon = { Icon(icon, contentDescription = null) }, text = { Text(label) }, onClick = onClick)
}

@Composable
private fun StatusDot(state: AgentOperationalState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(statusColor(state))
    )
}

@Composable
fun statusColor(state: AgentOperationalState) = when (state) {
    AgentOperationalState.IDLE -> MaterialTheme.status.success
    AgentOperationalState.RESEARCHING -> MaterialTheme.status.info
    AgentOperationalState.GENERATING -> MaterialTheme.colorScheme.primary
    AgentOperationalState.TASKS_PAUSED -> MaterialTheme.status.warning
}

// ---------------------------------------------------------------------------------------------
// Pieces of the chat screen
// ---------------------------------------------------------------------------------------------

@Composable
private fun SearchBarRow(
    query: String,
    bookmarksOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleBookmarks: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search this chat") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = bookmarksOnly,
            onClick = onToggleBookmarks,
            label = { Text("Saved") },
            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.padding(start = 8.dp)
        )
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close search") }
    }
}

/** Separate composable so AnimatedVisibility isn't resolved against the surrounding ColumnScope. */
@Composable
private fun JumpToLatestButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        SmallFloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Jump to latest message")
        }
    }
}

@Composable
private fun TimeSeparator(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
    )
}

@Composable
private fun ThinkingIndicator(status: String?) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = status ?: "Thinking…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusRibbon(text: String) {
    Row(
        modifier = Modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InputPill(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onRemove: () -> Unit,
    removeLabel: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = removeLabel) }
        }
    }
}

@Composable
private fun RecordingBanner(seconds: Int, transcript: String, onCancel: () -> Unit, onAttach: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text("Recording voice memo · ${seconds}s", style = MaterialTheme.typography.labelLarge)
                if (transcript.isNotBlank()) {
                    Text(transcript, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
            FilledTonalButton(onClick = onAttach) { Text("Attach") }
        }
    }
}

@Composable
private fun VisualDiagnosticsDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ask about a photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick what you want to know, then choose the image.", style = MaterialTheme.typography.bodyMedium)
                listOf(
                    "What's wrong here? Diagnose the issue in this photo.",
                    "Explain this schematic or diagram.",
                    "Read and summarize this document.",
                    "Explain this error or code screenshot.",
                    "Extract the numbers from this receipt or table."
                ).forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(preset) }
                    ) {
                        Text(preset, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------------------------
// Operational status dialog (background tasks)
// ---------------------------------------------------------------------------------------------

@Composable
fun OperationalStateDialog(
    state: AgentOperationalState,
    areTasksPaused: Boolean,
    onToggleTasks: (Boolean) -> Unit,
    onOpenReminders: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = when (state) {
        AgentOperationalState.IDLE -> Icons.Default.CheckCircle
        AgentOperationalState.RESEARCHING -> Icons.Default.TravelExplore
        AgentOperationalState.GENERATING -> Icons.Default.AutoAwesome
        AgentOperationalState.TASKS_PAUSED -> Icons.Default.PauseCircle
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, tint = statusColor(state)) },
        title = { Text(state.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.description, style = MaterialTheme.typography.bodyMedium)
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Scheduled tasks", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (areTasksPaused) "Paused. Tasks are skipped; reminders still arrive."
                                else "On. Tasks you scheduled run at their set times.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = !areTasksPaused, onCheckedChange = { active -> onToggleTasks(!active) })
                    }
                }
                TextButton(onClick = onOpenReminders) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reminders & tasks")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/** One row of the chat: a message or an action card. */
private data class TimelineEntry(val timestamp: Long, val message: Message? = null, val action: AgentAction? = null) {
    val key: String get() = message?.let { "m${it.id}" } ?: "a${action?.id}"
}

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.ModelRegistry
import com.example.ui.theme.status
import com.example.util.LiveInternetService
import com.example.util.LiveSearchResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsDialog(
    viewModel: ChatViewModel,
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val autoFallbackEnabled by viewModel.autoFallbackEnabled.collectAsStateWithLifecycle()
    val isLiveInternetEnabled by viewModel.isLiveInternetEnabled.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val liveModels by ModelRegistry.models.collectAsStateWithLifecycle()
    val llmProvider by viewModel.llmProvider.collectAsStateWithLifecycle()
    val compatModelName by viewModel.compatModel.collectAsStateWithLifecycle()
    val modelLoadError by ModelRegistry.loadError.collectAsStateWithLifecycle()

    // Load the real model list for this key when the dialog opens.
    LaunchedEffect(Unit) { viewModel.refreshModels() }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var categoryFilter by remember { mutableStateOf("All") }
    var customModelInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var apiKeySaveSuccess by remember { mutableStateOf(false) }

    var testSearchQuery by remember { mutableStateOf("Latest technology news") }
    var isTestingSearch by remember { mutableStateOf(false) }
    var testSearchResults by remember { mutableStateOf<List<LiveSearchResult>?>(null) }
    var testSearchError by remember { mutableStateOf<String?>(null) }

    var testUrlQuery by remember { mutableStateOf("https://en.wikipedia.org/wiki/Artificial_intelligence") }
    var isTestingUrl by remember { mutableStateOf(false) }
    var testUrlTitle by remember { mutableStateOf<String?>(null) }
    var testUrlSnippet by remember { mutableStateOf<String?>(null) }
    var testUrlError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(8.dp)
                .testTag("model_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Model & Quota Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (llmProvider == com.example.api.LlmProvider.OPENAI_COMPATIBLE) "Active: your server · $compatModelName" else "Active: $selectedModel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Available Models") },
                        icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Provider & keys") },
                        icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Live Internet") },
                        icon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (selectedTab == 0) {
                    // Models Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Category filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Flash", "Lite", "Pro", "Other").forEach { category ->
                                FilterChip(
                                    selected = categoryFilter == category,
                                    onClick = { categoryFilter = category },
                                    label = {
                                        Text(
                                            category,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }

                        // Custom Model Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = { customModelInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_model_input"),
                                label = { Text("Custom Model ID") },
                                placeholder = { Text("e.g. gemini-flash-latest") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customModelInput.isNotBlank()) {
                                        viewModel.updateModel(customModelInput.trim())
                                        customModelInput = ""
                                    }
                                },
                                enabled = customModelInput.isNotBlank(),
                                modifier = Modifier.testTag("apply_custom_model_button")
                            ) {
                                Text("Apply")
                            }
                        }

                        if (llmProvider == com.example.api.LlmProvider.OPENAI_COMPATIBLE) {
                            Text(
                                "Chats currently go to your OpenAI-compatible server (model \"$compatModelName\"). These Gemini models are used only if you switch back to Gemini in \"Provider & keys\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        // Model List (from the Gemini models.list API for this key)
                        val filteredModels = liveModels.filter {
                            categoryFilter == "All" || it.category.equals(categoryFilter, ignoreCase = true)
                        }
                        modelLoadError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (liveModels.isEmpty() && modelLoadError == null) {
                            Text("Loading models available to your key…", style = MaterialTheme.typography.bodySmall)
                        }
                        if (liveModels.isNotEmpty() && liveModels.none { it.id == selectedModel } && !selectedModel.endsWith("-latest")) {
                            Text(
                                "The selected model \"$selectedModel\" isn't in your key's model list. Pick one below.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredModels) { model ->
                                val isSelected = selectedModel == model.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateModel(model.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = model.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = when (model.category) {
                                                        "Lite" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        "Pro" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                                    }
                                                ) {
                                                    Text(
                                                        text = model.category.uppercase(),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = when (model.category) {
                                                            "Lite" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                            "Pro" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        },
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = model.id,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = model.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // Quota & API Key Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { ProviderSettingsCard(viewModel) }

                        // Auto-Fallback Switch
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Fallback on Quota Exceeded (429)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Automatically retry with alternate models (Flash/Lite) when the active model exceeds rate limits.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = autoFallbackEnabled,
                                        onCheckedChange = { viewModel.updateAutoFallback(it) },
                                        modifier = Modifier.testTag("auto_fallback_switch")
                                    )
                                }
                            }
                        }

                        // Custom API Key Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Custom / Paid Gemini API Key",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "By default, the app uses the key built into the app (if any). Enter your own Gemini API key from Google AI Studio to use your own quota.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = {
                                            apiKeyInput = it
                                            apiKeySaveSuccess = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("api_key_input"),
                                        label = { Text("Gemini API Key") },
                                        placeholder = { Text("AIzaSy...") },
                                        singleLine = true,
                                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                                Icon(
                                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showApiKey) "Hide key" else "Show key"
                                                )
                                            }
                                        }
                                    )

                                    if (apiKeySaveSuccess) {
                                        Text(
                                            text = "✓ Key saved successfully!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.status.success,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.updateCustomApiKey(apiKeyInput.trim())
                                                apiKeySaveSuccess = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("save_api_key_button")
                                        ) {
                                            Text("Save Key")
                                        }

                                        if (customApiKey.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = {
                                                    apiKeyInput = ""
                                                    viewModel.updateCustomApiKey("")
                                                    apiKeySaveSuccess = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reset to Default")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Quota Increase Guide
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📈 How to Increase Your Quota",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "1. Enable billing in Google AI Studio:\n" +
                                                "   Free keys have per-minute and per-day limits. Google lists the current limits for each tier on the AI Studio rate-limits page; billing-enabled keys get higher limits.\n\n" +
                                                "2. Use Lite models:\n" +
                                                "   Flash-Lite models (such as 'gemini-flash-lite-latest') are Google's lowest-cost option and usually have the most generous limits.\n\n" +
                                                "3. Auto-Fallback:\n" +
                                                "   Keep the Auto-Fallback switch ON above. When one model is temporarily busy or rate-limited, the app automatically reroutes through other available models.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 2) {
                    // Live Internet Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Master Switch Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLiveInternetEnabled)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = if (isLiveInternetEnabled) MaterialTheme.status.success else MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Direct Live Internet Access",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isLiveInternetEnabled)
                                                "Active: Real-time search and direct URL browsing are enabled."
                                            else
                                                "Disabled: Assistant will only use internal model training weights.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isLiveInternetEnabled,
                                        onCheckedChange = { viewModel.toggleLiveInternet(it) },
                                        modifier = Modifier.testTag("dialog_live_internet_switch")
                                    )
                                }
                            }
                        }

                        // Architecture Highlights Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "⚡ Live Internet Architecture",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "• Real-Time Web Search: Queries DuckDuckGo and Wikipedia organic indices live.\n" +
                                                "• Live Page Extraction: Directly loads HTML and extracts clean article content via Jsoup.\n" +
                                                "• Runs on your device: searches and page loads are made by the app, not by Google Search grounding. Your search terms are sent to DuckDuckGo (and Wikipedia as a fallback).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Live Search Connection Tester
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🔍 Test Live Web Search",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Verify live internet connectivity by running a real search query now:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = testSearchQuery,
                                        onValueChange = { testSearchQuery = it },
                                        label = { Text("Search Query") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isTestingSearch = true
                                                testSearchError = null
                                                try {
                                                    val res = LiveInternetService.searchLiveInternet(testSearchQuery)
                                                    testSearchResults = res
                                                    if (res.isEmpty()) {
                                                        testSearchError = "No search results returned. Try another query."
                                                    }
                                                } catch (e: Exception) {
                                                    testSearchError = "Search failed: ${e.message}"
                                                } finally {
                                                    isTestingSearch = false
                                                }
                                            }
                                        },
                                        enabled = !isTestingSearch && testSearchQuery.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isTestingSearch) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Connecting to Live Web...")
                                        } else {
                                            Text("Run Live Web Search")
                                        }
                                    }

                                    if (testSearchError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = testSearchError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (testSearchResults != null && testSearchResults!!.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "✅ Found ${testSearchResults!!.size} live results:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.status.success
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        testSearchResults!!.take(3).forEach { item ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = item.url,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1
                                                    )
                                                    if (item.snippet.isNotEmpty()) {
                                                        Text(
                                                            text = item.snippet,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live URL Webpage Tester
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🌐 Test Live URL Reader",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Test fetching and extracting text from any live webpage:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = testUrlQuery,
                                        onValueChange = { testUrlQuery = it },
                                        label = { Text("Webpage URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                isTestingUrl = true
                                                testUrlError = null
                                                try {
                                                    val page = LiveInternetService.fetchLiveWebpage(testUrlQuery)
                                                    testUrlTitle = page.title
                                                    testUrlSnippet = page.content.take(300)
                                                } catch (e: Exception) {
                                                    testUrlError = "Failed to load page: ${e.message}"
                                                } finally {
                                                    isTestingUrl = false
                                                }
                                            }
                                        },
                                        enabled = !isTestingUrl && testUrlQuery.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isTestingUrl) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Fetching Webpage...")
                                        } else {
                                            Text("Fetch & Read Webpage")
                                        }
                                    }

                                    if (testUrlError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = testUrlError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (testUrlTitle != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "✅ Page Title: $testUrlTitle",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.status.success
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = testUrlSnippet ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

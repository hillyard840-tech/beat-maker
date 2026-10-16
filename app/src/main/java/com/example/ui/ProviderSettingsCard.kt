package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.LlmProvider

/** Lets the user choose Gemini or an OpenAI-compatible server (FreeLLMAPI) and set it up. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderSettingsCard(viewModel: ChatViewModel) {
    val provider by viewModel.llmProvider.collectAsStateWithLifecycle()
    val savedBase by viewModel.compatBaseUrl.collectAsStateWithLifecycle()
    val savedKey by viewModel.compatApiKey.collectAsStateWithLifecycle()
    val savedModel by viewModel.compatModel.collectAsStateWithLifecycle()
    val models by viewModel.compatModels.collectAsStateWithLifecycle()
    val status by viewModel.compatStatus.collectAsStateWithLifecycle()
    val testing by viewModel.isTestingCompat.collectAsStateWithLifecycle()

    var baseUrl by remember(savedBase) { mutableStateOf(savedBase) }
    var apiKey by remember(savedKey) { mutableStateOf(savedKey) }
    var model by remember(savedModel) { mutableStateOf(savedModel) }
    var showKey by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI provider", style = MaterialTheme.typography.titleMedium)

            listOf(
                LlmProvider.GEMINI to "Google Gemini",
                LlmProvider.OPENAI_COMPATIBLE to "OpenAI-compatible server (FreeLLMAPI, etc.)"
            ).forEach { (option, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.setLlmProvider(option) }
                ) {
                    RadioButton(selected = provider == option, onClick = { viewModel.setLlmProvider(option) })
                    Text(label)
                }
            }

            if (provider == LlmProvider.OPENAI_COMPATIBLE) {
                Text(
                    "Server address: where FreeLLMAPI runs. On the same Wi-Fi use your computer's local IP, e.g. " +
                        "http://192.168.1.20:3001, and start FreeLLMAPI with HOST_BIND=0.0.0.0 (by default it only accepts " +
                        "connections from the computer itself). If FreeLLMAPI runs on this phone (Termux), use http://localhost:3001. " +
                        "For access from anywhere, use an https:// address.\n\n" +
                        "API key: the unified key from the FreeLLMAPI Keys page. Model \"auto\" lets FreeLLMAPI choose.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; saveMessage = null },
                    label = { Text("Server address") },
                    placeholder = { Text("http://192.168.1.20:3001") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; saveMessage = null },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (showKey) "Hide key" else "Show key")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; saveMessage = null },
                    label = { Text("Model") },
                    placeholder = { Text("auto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (models.isNotEmpty()) {
                    Text("Models on your server (tap to use):", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf("auto") + models).distinct().take(40).forEach { id ->
                            FilterChip(
                                selected = model == id,
                                onClick = {
                                    model = id
                                    viewModel.setCompatModel(id)
                                },
                                label = { Text(id) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        saveMessage = viewModel.saveCompatSettings(baseUrl, apiKey, model) ?: "Saved."
                    }) { Text("Save") }
                    OutlinedButton(
                        onClick = {
                            val problem = viewModel.saveCompatSettings(baseUrl, apiKey, model)
                            saveMessage = problem
                            if (problem == null) viewModel.testCompatConnection()
                        },
                        enabled = !testing
                    ) { Text(if (testing) "Testing…" else "Save & test") }
                }
                saveMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "Image generation still uses Gemini's Imagen and needs a Gemini key. Web search tools work only if the chosen model supports function calling; otherwise Kora answers without them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.example.ui.clawearn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import com.example.data.clawearn.ClawEarnClient
import com.example.data.clawearn.ClawEarnHealth
import com.example.data.clawearn.ClawEarnTask
import com.example.data.clawearn.ClawEarnTaskPage
import com.example.income.Deliverables
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Read-only view of the public Claw Earn task board. Nothing here is generated locally. */
class ClawEarnViewModel(application: Application) : AndroidViewModel(application) {
    private val _tab = MutableStateFlow("available")
    val tab: StateFlow<String> = _tab.asStateFlow()

    private val _page = MutableStateFlow<ClawEarnTaskPage?>(null)
    val page: StateFlow<ClawEarnTaskPage?> = _page.asStateFlow()

    private val _health = MutableStateFlow<ClawEarnHealth?>(null)
    val health: StateFlow<ClawEarnHealth?> = _health.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun setTab(tab: String) {
        _tab.value = tab
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _page.value = ClawEarnClient.tasks(_tab.value)
                _health.value = try {
                    ClawEarnClient.health()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null // health is informational only
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _page.value = null
                _error.value = "Couldn't load tasks from aiagentstore.ai: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClawEarnScreen(
    onClose: () -> Unit,
    onDraftWithKora: (String) -> Unit = {},
    viewModel: ClawEarnViewModel = viewModel()
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val page by viewModel.page.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ClawEarnTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Claw Earn tasks", fontWeight = FontWeight.Bold)
                        Text("Live from aiagentstore.ai · read-only", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            Text(
                text = "Kora can draft the work for available tasks so you can check it. Claiming a task, submitting and getting paid happen with your own Base wallet, outside Kora.Ai.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            health?.let { h ->
                Text(
                    text = buildString {
                        append(if (h.ok) "Service online" else "Service reports a problem")
                        h.chainId?.let { append(" · chain $it") }
                        h.latestBlock?.let { append(" · block $it") }
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "available" to page?.available,
                    "occupied" to page?.occupied,
                    "completed" to page?.completed
                ).forEach { (option, count) ->
                    FilterChip(
                        selected = tab == option,
                        onClick = { viewModel.setTab(option) },
                        label = {
                            val name = option.replaceFirstChar { it.uppercase() }
                            Text(if (count != null) "$name ($count)" else name)
                        }
                    )
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            val tasks = page?.tasks.orEmpty()
            if (tasks.isEmpty() && !isLoading && error == null) {
                Text("No $tab tasks right now.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tasks, key = { "${it.contractAddress}:${it.taskId}" }) { task ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { selected = task }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(task.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    append(task.status)
                                    task.amountUsdc?.let { append(" · %.2f USDC".format(it)) }
                                    task.category?.let { append(" · $it") }
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (task.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(task.description, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { task ->
        TaskDetailDialog(
            task = task,
            onDismiss = { selected = null },
            onDraft = if (task.status.equals("available", ignoreCase = true) || tab == "available") {
                {
                    selected = null
                    val kind = Deliverables.kindOf(task.title, task.description, listOfNotNull(task.category)) ?: Deliverables.Kind.RESEARCH
                    onDraftWithKora(
                        Deliverables.draftPrompt(
                            title = task.title,
                            details = buildString {
                                append(task.description)
                                task.amountUsdc?.let { append("\nReward: %.2f USDC".format(it)) }
                                task.category?.let { append("\nCategory: $it") }
                            },
                            source = "the Claw Earn board for AI agents (task ${task.taskId})",
                            url = null,
                            kind = kind
                        )
                    )
                }
            } else null
        )
    }
}

@Composable
private fun TaskDetailDialog(task: ClawEarnTask, onDismiss: () -> Unit, onDraft: (() -> Unit)?) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Status: ${task.status}")
                task.amountUsdc?.let { Text("Reward: %.2f USDC".format(it)) }
                Text("Task ID: ${task.taskId}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                if (task.contractAddress.isNotBlank()) {
                    Text("Contract: ${task.contractAddress}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                task.poster?.let { Text("Poster: $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                task.worker?.let { Text("Worker: $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                if (task.description.isNotBlank()) {
                    HorizontalDivider()
                    Text(task.description, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            if (onDraft != null) TextButton(onClick = onDraft) { Text("Draft it with Kora") }
            else TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString("${task.contractAddress}:${task.taskId}"))
            }) { Text("Copy IDs") }
        }
    )
}

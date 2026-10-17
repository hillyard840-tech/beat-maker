package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.AlarmScheduling
import com.example.agent.Notifications
import com.example.data.Reminder
import com.example.data.ScheduledTask
import com.example.util.AgentTime

/** Everything Kora will do later: reminders it delivers and tasks it runs on a schedule. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val tasks by viewModel.scheduledTasks.collectAsStateWithLifecycle()
    val tasksPaused by viewModel.areBackgroundTasksPaused.collectAsStateWithLifecycle()

    // Permissions can change in system settings, so check again whenever the screen resumes.
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    var exactAlarms by remember { mutableStateOf(AlarmScheduling.canScheduleExact(context)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Notifications.canPost(context)
        exactAlarms = AlarmScheduling.canScheduleExact(context)
        onPauseOrDispose { }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = Notifications.canPost(context)
    }

    var editing by remember { mutableStateOf<ScheduledTask?>(null) }
    var confirmDelete by remember { mutableStateOf<ScheduledTask?>(null) }

    androidx.activity.compose.BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders & tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = newTask() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New task") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!canNotify) {
                item {
                    PermissionCard(
                        icon = Icons.Default.NotificationsOff,
                        text = "Notifications are off, so reminders and task results can't reach you.",
                        button = "Turn on"
                    ) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
            if (!exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PermissionCard(
                        icon = Icons.Default.AlarmOff,
                        text = "Without \"Alarms & reminders\" access, Android may deliver reminders a few minutes late.",
                        button = "Allow"
                    ) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            item { SectionHeader("Scheduled tasks") }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (tasksPaused) "Paused: tasks are skipped until you turn this back on."
                        else "Kora runs these by itself and notifies you with the answer. Each run uses one or more AI requests.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = !tasksPaused, onCheckedChange = { on -> viewModel.toggleBackgroundTasks(paused = !on) })
                }
            }
            if (tasks.isEmpty()) {
                item {
                    OutlinedCard(onClick = { editing = morningBriefing() }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Try a morning briefing", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Weather, top news and your day, every morning at 7:30. Or ask in chat: \"every weekday at 8 send me tech news\".",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            items(tasks, key = { "t${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { viewModel.setTaskEnabled(task.id, it) },
                    onRunNow = { viewModel.runTaskNow(task.id) },
                    onEdit = { editing = task },
                    onDelete = { confirmDelete = task },
                    onOpenChat = task.conversationId?.let { id -> { viewModel.openConversation(id) } }
                )
            }

            item { SectionHeader("Reminders") }
            if (reminders.isEmpty()) {
                item {
                    Text(
                        "No reminders. Ask Kora: \"remind me to call mom tomorrow at 6pm\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(reminders, key = { "r${it.id}" }) { reminder ->
                ReminderRow(reminder, onDelete = { viewModel.deleteReminder(reminder.id) })
            }
            if (reminders.any { it.isDone }) {
                item {
                    TextButton(onClick = { viewModel.clearDoneReminders() }) { Text("Clear past reminders") }
                }
            }
        }
    }

    editing?.let { task ->
        TaskEditorDialog(
            initial = task,
            onDismiss = { editing = null },
            onSave = {
                viewModel.saveTask(it)
                editing = null
            }
        )
    }
    confirmDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${task.title}\"?") },
            text = { Text("It won't run again. Answers it already saved stay in its chat.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task.id)
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}

private fun newTask() = ScheduledTask(title = "", prompt = "", hour = 8, minute = 0)

private fun morningBriefing() = ScheduledTask(
    title = "Morning briefing",
    prompt = "Give me a short morning briefing: today's weather where I am (ask me to add my city to memory if you don't know it), " +
        "the 3 most important news headlines, and anything on my calendar today.",
    hour = 7,
    minute = 30,
    repeat = ScheduledTask.REPEAT_DAILY
)

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun PermissionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, button: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskRow(
    task: ScheduledTask,
    onToggle: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenChat: (() -> Unit)?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${AgentTime.repeatLabel(task.repeat)} at ${AgentTime.clock(task.hour, task.minute)}" +
                            (task.nextRunAt?.takeIf { task.enabled }?.let { " · next ${AgentTime.label(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = task.enabled, onCheckedChange = onToggle)
            }
            Text(task.prompt, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
            task.lastResult?.let {
                Text(
                    "Last run" + (task.lastRunAt?.let { t -> " ${AgentTime.label(t)}" } ?: "") + ": $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
            }
            FlowRow {
                TextButton(onClick = onRunNow) { Text("Run now") }
                if (onOpenChat != null) TextButton(onClick = onOpenChat) { Text("Open chat") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onDelete: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                if (reminder.isDone) Icons.Default.NotificationsNone else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (reminder.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(reminder.title) },
        supportingContent = {
            Text(
                (if (reminder.isDone) "Delivered " else "") + AgentTime.label(reminder.triggerAt) +
                    (reminder.note.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = if (reminder.isDone) "Remove" else "Cancel reminder")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(initial: ScheduledTask, onDismiss: () -> Unit, onSave: (ScheduledTask) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial.title) }
    var prompt by remember { mutableStateOf(initial.prompt) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var useWeb by remember { mutableStateOf(initial.useWeb) }
    val time = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0) "New task" else "Edit task") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it.take(2000) },
                    label = { Text("What should Kora do?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                TimeInput(state = time)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ScheduledTask.REPEAT_DAILY, ScheduledTask.REPEAT_WEEKDAYS, ScheduledTask.REPEAT_ONCE).forEach { option ->
                        FilterChip(selected = repeat == option, onClick = { repeat = option }, label = { Text(AgentTime.repeatLabel(option)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Can search the web", modifier = Modifier.weight(1f))
                    Switch(checked = useWeb, onCheckedChange = { useWeb = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && prompt.isNotBlank(),
                onClick = {
                    onSave(initial.copy(title = title, prompt = prompt, hour = time.hour, minute = time.minute, repeat = repeat, useWeb = useWeb, enabled = true))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

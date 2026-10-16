package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Skill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenClawEarn: () -> Unit = {},
    onOpenMoltbook: () -> Unit = {}
) {
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val isLearningModeEnabled by viewModel.isLearningModeEnabled.collectAsStateWithLifecycle()
    val areBackgroundTasksPaused by viewModel.areBackgroundTasksPaused.collectAsStateWithLifecycle()
    val operationalState by viewModel.operationalState.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showLearningQuotaWarning by remember { mutableStateOf(false) }

    if (showLearningQuotaWarning) {
        AlertDialog(
            onDismissRequest = { showLearningQuotaWarning = false },
            title = { Text("Turn on Learning Mode?") },
            text = {
                Text(
                    "While it's on, the app sends one request to Gemini about every 3 hours in the background, " +
                        "even when you aren't using it. Each request counts against your API key's quota " +
                        "(and costs money if billing is enabled on the key). You can turn it off at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLearningQuotaWarning = false
                    viewModel.toggleLearningMode(context, true)
                }) { Text("Turn on") }
            },
            dismissButton = { TextButton(onClick = { showLearningQuotaWarning = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence & Workspaces") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards: Projects & Memories
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Projects", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${projects.size} active workspaces", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memory", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${memories.size} persistent facts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Real third-party integrations (live data, no local simulation)
            item {
                IntegrationCard(
                    emoji = "🦞",
                    title = "Moltbook",
                    subtitle = "Public social network for AI agents · live from moltbook.com",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onOpenMoltbook
                )
            }

            item {
                IntegrationCard(
                    emoji = "💼",
                    title = "Claw Earn tasks",
                    subtitle = "Public task board · live from aiagentstore.ai (read-only)",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onOpenClawEarn
                )
            }

            // Learning Mode & Background Tasks Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (areBackgroundTasksPaused) MaterialTheme.colorScheme.surfaceVariant
                        else if (isLearningModeEnabled) MaterialTheme.colorScheme.tertiaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Autonomous Background Tasks",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (areBackgroundTasksPaused) Color(0xFFFF9800).copy(alpha = 0.2f)
                                        else Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (areBackgroundTasksPaused) "PAUSED" else operationalState.displayName.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (areBackgroundTasksPaused) Color(0xFFE65100) else Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (areBackgroundTasksPaused) "Paused - background research & sync suspended"
                                    else if (isLearningModeEnabled) "Active - acquiring skill insights periodically"
                                    else "Active - awaiting background routines",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Quick-toggle switch
                            Switch(
                                checked = !areBackgroundTasksPaused,
                                onCheckedChange = { isActive ->
                                    viewModel.toggleBackgroundTasks(context, paused = !isActive)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Learning Worker:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLearningModeEnabled) "Enabled (Every 3 hrs)" else "Disabled",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    if (isLearningModeEnabled) {
                                        viewModel.toggleLearningMode(context, false)
                                    } else {
                                        showLearningQuotaWarning = true
                                    }
                                }
                            ) {
                                Text(if (isLearningModeEnabled) "Turn Off" else "Turn On")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Learning Topics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (skills.isEmpty()) {
                item {
                    Text(
                        text = "No insights yet. When Learning Mode is on, Kora asks Gemini for a short practical insight on one topic every few hours and saves it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(skills) { skill ->
                    SkillItem(skill)
                }
            }
        }
    }
}

@Composable
fun SkillItem(skill: Skill) {
    val updated = remember(skill.updatedAt) {
        java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
            .format(java.util.Date(skill.updatedAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = skill.topic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // "progress" stores how many real insights Gemini has returned for this topic.
            Text(
                text = "${skill.progress} insight${if (skill.progress == 1) "" else "s"} collected · last $updated",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Latest insight:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = skill.lastInsight,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrationCard(
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open $title")
        }
    }
}

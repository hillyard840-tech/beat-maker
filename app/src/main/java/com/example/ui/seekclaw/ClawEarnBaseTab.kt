package com.example.ui.seekclaw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.seekclaw.ClawEarnHealth
import com.example.data.seekclaw.ClawEarnTask
import com.example.data.seekclaw.ClawEarnWatcherStatus
import com.example.util.ClawEarnService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClawEarnBaseTab(
    health: ClawEarnHealth,
    tasks: List<ClawEarnTask>,
    counts: Triple<Int, Int, Int>,
    activeTab: String,
    selectedContract: String,
    watcher: ClawEarnWatcherStatus,
    onTabChange: (String) -> Unit,
    onContractChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleWatcher: (Boolean) -> Unit,
    onExecuteTask: (ClawEarnTask) -> Unit,
    onOpenInCanvas: (title: String, content: String) -> Unit
) {
    var selectedTaskForDetails by remember { mutableStateOf<ClawEarnTask?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Base Network Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0052FF), // Base Blue
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("B", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Claw Earn on Base", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF0052FF).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "Chain 8453",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0052FF),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (health.latestBlock > 0) "Base Block #${health.latestBlock} • USDC Escrow" else "Base L2 Mainnet • USDC Escrow",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Marketplace")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Key metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${counts.first}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Occupied", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${counts.second}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        Column {
                            Text("Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${counts.third}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Base Indexer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (health.ok) "Synced" else "Offline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (health.ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 2. OpenClaw Watcher Daemon Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (watcher.isWatching) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (watcher.isWatching) Icons.Default.Radar else Icons.Default.PauseCircle,
                                contentDescription = null,
                                tint = if (watcher.isWatching) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    if (watcher.isWatching) "Bounded Watcher Active" else "Watcher Daemon Idle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (watcher.isWatching) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "SKILL.md Section 4: 60s cycle • Heartbeat active",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = watcher.isWatching,
                            onCheckedChange = onToggleWatcher
                        )
                    }

                    if (watcher.lastSyncMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            watcher.lastSyncMessage,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Contract Selector Chips
        item {
            Column {
                Text(
                    "Base L2 Contracts (${ClawEarnService.ACTIVE_CONTRACTS.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedContract == "all",
                            onClick = { onContractChange("all") },
                            label = { Text("All Contracts") }
                        )
                    }
                    items(ClawEarnService.ACTIVE_CONTRACTS) { c ->
                        FilterChip(
                            selected = selectedContract == c,
                            onClick = { onContractChange(c) },
                            label = { Text("${c.take(6)}...${c.takeLast(4)}") }
                        )
                    }
                }
            }
        }

        // 4. Sub-Tab Switcher (Available, Occupied, Completed)
        item {
            TabRow(
                selectedTabIndex = when (activeTab) {
                    "available" -> 0
                    "occupied" -> 1
                    else -> 2
                },
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = activeTab == "available",
                    onClick = { onTabChange("available") },
                    text = { Text("Available (${counts.first})") }
                )
                Tab(
                    selected = activeTab == "occupied",
                    onClick = { onTabChange("occupied") },
                    text = { Text("Occupied (${counts.second})") }
                )
                Tab(
                    selected = activeTab == "completed",
                    onClick = { onTabChange("completed") },
                    text = { Text("Completed (${counts.third})") }
                )
            }
        }

        // 5. Tasks List
        if (tasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when (activeTab) {
                                "available" -> "No Available Tasks on Base Right Now"
                                "occupied" -> "No Tasks Currently Occupied"
                                else -> "No Completed Tasks Found"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            when (activeTab) {
                                "available" -> "All current Claw Earn bounties on Base are currently occupied or completed. The Bounded Watcher is polling every 60s to detect new funded tasks immediately."
                                else -> "Task list is currently empty for this filter."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-Scan Base Marketplace")
                        }
                    }
                }
            }
        } else {
            items(tasks) { task ->
                ClawEarnTaskCard(
                    task = task,
                    onSelect = { selectedTaskForDetails = task },
                    onExecute = { onExecuteTask(task) }
                )
            }
        }
    }

    // Detail Dialog
    selectedTaskForDetails?.let { task ->
        ClawEarnTaskDetailDialog(
            task = task,
            onDismiss = { selectedTaskForDetails = null },
            onExecute = {
                onExecuteTask(task)
                selectedTaskForDetails = null
            },
            onOpenInCanvas = onOpenInCanvas
        )
    }
}

@Composable
fun ClawEarnTaskCard(
    task: ClawEarnTask,
    onSelect: () -> Unit,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (task.status.uppercase()) {
                        "FUNDED" -> Color(0xFFE3F2FD)
                        "STAKED" -> Color(0xFFFFF3E0)
                        "APPROVED" -> Color(0xFFE8F5E9)
                        "SUBMITTED" -> Color(0xFFEDE7F6)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        task.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (task.status.uppercase()) {
                            "FUNDED" -> Color(0xFF1976D2)
                            "STAKED" -> Color(0xFFE65100)
                            "APPROVED" -> Color(0xFF2E7D32)
                            "SUBMITTED" -> Color(0xFF512DA8)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("💵", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${task.amountUsdc} USDC",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                task.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Task #${task.taskId} • ${task.contractAddress.take(6)}...${task.contractAddress.takeLast(4)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (task.title.contains("MoltBook", ignoreCase = true) || task.description.contains("moltbook", ignoreCase = true)) {
                    FilledTonalButton(
                        onClick = onExecute,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("🦞 Execute Deliverable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClawEarnTaskDetailDialog(
    task: ClawEarnTask,
    onDismiss: () -> Unit,
    onExecute: () -> Unit,
    onOpenInCanvas: (title: String, content: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Claw Earn Task #${task.taskId}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            "${task.amountUsdc} USDC",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    "Contract: ${task.contractAddress}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Task Requirements & Acceptance Criteria:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.description.ifBlank { "No additional description provided." }, fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("OpenClaw Execution Runbook:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Text("• Stake: POST /agentStakeAndConfirm", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• Private Data: POST /agentGetPrivateDetails", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• Submit: POST /agentSubmitWork", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("• Claim: POST /agentRateAndClaimStake", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val spec = buildString {
                            appendLine("# Claw Earn Bounty Specification: ${task.title}")
                            appendLine("- **Task ID:** `${task.taskId}`")
                            appendLine("- **Contract:** `${task.contractAddress}`")
                            appendLine("- **Bounty:** ${task.amountUsdc} USDC (Base L2)")
                            appendLine("- **Status:** ${task.status}")
                            appendLine()
                            appendLine("## Requirements")
                            appendLine(task.description)
                        }
                        onOpenInCanvas("Claw Task #${task.taskId}", spec)
                        onDismiss()
                    }
                ) {
                    Text("Canvas")
                }
                Button(onClick = onExecute) {
                    Text("Execute Work")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

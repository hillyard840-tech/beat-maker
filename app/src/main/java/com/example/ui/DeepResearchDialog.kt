package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepResearchDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val researchState by viewModel.deepResearchState.collectAsStateWithLifecycle()
    var inquiryQuery by remember { mutableStateOf("") }
    var siteRestrictions by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var copiedReport by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔬 Deep Research Engine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Text(
                    text = "Autonomously plans research, crawls multiple sources, cross-references findings, and produces a cited comparative report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (researchState.logs.isEmpty() && researchState.report == null) {
                    // Query & Website restriction input form
                    OutlinedTextField(
                        value = inquiryQuery,
                        onValueChange = { inquiryQuery = it },
                        label = { Text("Research Inquiry *") },
                        placeholder = { Text("e.g. Compare Washington state lenders for 520 credit scores with exact requirements") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = siteRestrictions,
                        onValueChange = { siteRestrictions = it },
                        label = { Text("Restrict/Prioritize Websites (Optional)") },
                        placeholder = { Text("e.g. wa.gov, creditkarma.com, nerdwallet.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Example Deep Inquiries:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "Compare EV tax incentives across US states in 2026",
                        "Latest Android 16 system architecture changes vs Android 15",
                        "Best high-yield savings accounts with no fee breakdown"
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• $preset",
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .padding(start = 2.dp)
                            )
                        }
                    }
                } else {
                    // Ongoing or Completed Research View
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = researchState.phase,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!researchState.isFinished) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (researchState.step.toFloat() / researchState.totalSteps).coerceIn(0.1f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Research Logs & Crawled Sources
                    Text(
                        text = "LIVE ACTIVITY & EVIDENCE LOG (${researchState.sources.size} sources)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(researchState.logs) { log ->
                            Text(
                                text = log,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (researchState.report != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Final Synthesized Report Ready",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                            val context = LocalContext.current
                            Row {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.exportResearchReport(context, researchState.report ?: "")
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(researchState.report ?: ""))
                                        copiedReport = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copiedReport) "Copied!" else "Copy", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (researchState.logs.isEmpty() && researchState.report == null) {
                Button(
                    onClick = {
                        if (inquiryQuery.isNotBlank()) {
                            viewModel.runDeepResearch(inquiryQuery.trim(), siteRestrictions.trim())
                        }
                    },
                    enabled = inquiryQuery.isNotBlank()
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch Deep Research")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text(if (researchState.isFinished) "View In Chat" else "Run in Background")
                }
            }
        },
        dismissButton = {
            if (researchState.logs.isEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else if (researchState.isFinished) {
                TextButton(
                    onClick = {
                        viewModel.deepResearchState.value = com.example.util.DeepResearchState()
                    }
                ) {
                    Text("New Research")
                }
            }
        }
    )
}

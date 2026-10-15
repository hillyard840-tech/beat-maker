package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.DataColumnStats
import com.example.util.ParsedDataset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataAnalysisSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val dataset by viewModel.currentDataset.collectAsStateWithLifecycle()
    val currentFile by viewModel.attachedFile.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Data Analysis Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentFile?.name ?: "Dataset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dataset == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No dataset currently loaded. Attach a CSV file to begin analysis.")
                }
            } else {
                val data = dataset!!

                // Overview Stat Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Rows", "${data.totalRows}", Modifier.weight(1f))
                    StatCard("Columns", "${data.totalColumns}", Modifier.weight(1f))
                    val numCols = data.columnStats.count { it.isNumeric }
                    StatCard("Numeric Metrics", "$numCols", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Visualizer
                val firstNumeric = data.columnStats.firstOrNull { it.isNumeric }
                if (firstNumeric != null) {
                    Text(
                        text = "Distribution: ${firstNumeric.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val colIndex = data.headers.indexOf(firstNumeric.name)
                    val numericVals = remember(data, colIndex) {
                        if (colIndex != -1) {
                            data.rows.take(15).mapNotNull { it.getOrNull(colIndex)?.toDoubleOrNull() }
                        } else emptyList()
                    }

                    if (numericVals.isNotEmpty()) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                val maxVal = numericVals.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                                val barWidth = (size.width / numericVals.size) * 0.7f
                                val spacing = (size.width / numericVals.size) * 0.3f

                                numericVals.forEachIndexed { i, v ->
                                    val barHeight = ((v / maxVal) * (size.height - 10)).toFloat()
                                    val left = i * (barWidth + spacing) + (spacing / 2f)
                                    val top = size.height - barHeight

                                    drawRect(
                                        color = primaryColor,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Table Preview
                Text(
                    text = "Table Preview (First ${data.rows.size} rows)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val horizontalScroll = rememberScrollState()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .horizontalScroll(horizontalScroll)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Header row
                        Row {
                            data.headers.forEach { h ->
                                Text(
                                    text = h,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(100.dp).padding(4.dp)
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        // Data rows
                        data.rows.take(5).forEach { row ->
                            Row {
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.width(100.dp).padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick AI Analysis Actions
                Text(
                    text = "AI Data Analysis Actions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.sendMessage("Analyze this dataset: find the key trends, correlations, and business projections.", null)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Find Trends", fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            viewModel.sendMessage("Compute comprehensive statistics for this data, identify outliers, and evaluate data quality.", null)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Statistics", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

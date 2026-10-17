package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class Starter(val icon: ImageVector, val title: String, val prompt: String)

private val STARTERS = listOf(
    Starter(Icons.Default.Work, "Find me work", "Find me remote jobs or gigs for someone who can ___. Show real listings with links."),
    Starter(Icons.Default.Savings, "Side income", "I have about ___ hours a week and these skills: ___. What are realistic ways I could earn extra money?"),
    Starter(Icons.Default.Construction, "What to build", "What's a small program I could build around ___ that people actually need and would pay for?"),
    Starter(Icons.Default.EventNote, "Plan my week", "Help me plan my week. My priorities are: ___")
)

/** First screen of a new chat: what Kora can do, a few starters, and API-key setup if needed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyChatState(
    hasApiKey: Boolean,
    onAddApiKey: () -> Unit,
    onStarter: (String) -> Unit,
    onVoiceChat: () -> Unit,
    onOpportunities: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Let's get you ahead", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Kora finds real work, side income and things worth building, and still helps with everything else.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (!hasApiKey) {
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add your Gemini API key", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Kora needs a free key from Google AI Studio (aistudio.google.com) before it can answer.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onAddApiKey) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add API key")
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        STARTERS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { starter ->
                    OutlinedCard(onClick = { onStarter(starter.prompt) }, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(starter.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(starter.title, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpportunities) {
                Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Opportunities")
            }
            FilledTonalButton(onClick = onVoiceChat) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voice chat")
            }
        }
    }
}

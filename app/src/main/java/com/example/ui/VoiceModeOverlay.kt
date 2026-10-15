package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.VoiceState

@Composable
fun VoiceModeOverlay(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val voiceManager = viewModel.voiceManager
    val voiceState by voiceManager.voiceState.collectAsStateWithLifecycle()
    val spokenText by voiceManager.spokenText.collectAsStateWithLifecycle()
    val volumeLevel by voiceManager.volumeLevel.collectAsStateWithLifecycle()
    val isAutoSpeak by viewModel.isVoiceAutoSpeak.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            voiceManager.startListening(
                onFinalResult = { query ->
                    viewModel.sendMessage(query, null)
                }
            )
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (voiceState == VoiceState.IDLE) {
            voiceManager.startListening(
                onFinalResult = { query ->
                    viewModel.sendMessage(query, null)
                }
            )
        }
    }

    // Glowing Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = {
                    voiceManager.stopSpeaking()
                    voiceManager.stopListening()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close Voice Mode")
                }
            }

            // Center Dynamic Sphere / Soundwave
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                val baseScale = if (voiceState == VoiceState.LISTENING) {
                    1f + (volumeLevel * 0.4f)
                } else if (voiceState == VoiceState.SPEAKING) {
                    pulseScale
                } else {
                    1f
                }

                val orbColor = when (voiceState) {
                    VoiceState.LISTENING -> Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                    VoiceState.PROCESSING -> Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    )
                    VoiceState.SPEAKING -> Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
                    )
                    VoiceState.IDLE -> Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(baseScale)
                        .clip(CircleShape)
                        .background(orbColor)
                        .clickable {
                            if (voiceState == VoiceState.SPEAKING) {
                                voiceManager.stopSpeaking()
                            } else if (voiceState == VoiceState.LISTENING) {
                                voiceManager.stopListening()
                            } else {
                                voiceManager.startListening(
                                    onFinalResult = { query -> viewModel.sendMessage(query, null) }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            VoiceState.LISTENING -> Icons.Default.Mic
                            VoiceState.PROCESSING -> Icons.Default.HourglassTop
                            VoiceState.SPEAKING -> Icons.Default.VolumeUp
                            VoiceState.IDLE -> Icons.Default.MicNone
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening... Speak naturally"
                        VoiceState.PROCESSING -> "Thinking & processing..."
                        VoiceState.SPEAKING -> "Speaking... Tap orb to interrupt"
                        VoiceState.IDLE -> "Tap orb to speak"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live transcription
                if (spokenText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "\"$spokenText\"",
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Bottom controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isAutoSpeak,
                        onClick = { viewModel.toggleVoiceAutoSpeak(!isAutoSpeak) },
                        leadingIcon = {
                            Icon(
                                if (isAutoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Spoken Responses") }
                    )

                    if (voiceState == VoiceState.SPEAKING) {
                        FilledTonalButton(
                            onClick = { voiceManager.stopSpeaking() },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Interrupt")
                        }
                    }
                }
            }
        }
    }
}

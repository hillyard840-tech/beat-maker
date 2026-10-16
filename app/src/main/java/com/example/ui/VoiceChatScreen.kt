package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ChatViewModel.VoicePhase
import com.example.util.SpeechText

/**
 * Hands-free voice conversation. Kora listens, answers out loud as the reply streams in, then
 * listens again. Tap the orb to interrupt or to talk; every turn is also saved in the chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val phase by viewModel.voicePhase.collectAsStateWithLifecycle()
    val error by viewModel.voiceError.collectAsStateWithLifecycle()
    val reply by viewModel.voiceReply.collectAsStateWithLifecycle()
    val heard by viewModel.voiceManager.spokenText.collectAsStateWithLifecycle()
    val volume by viewModel.voiceManager.volumeLevel.collectAsStateWithLifecycle()
    val handsFree by viewModel.voiceHandsFree.collectAsStateWithLifecycle()
    val speakReplies by viewModel.isVoiceAutoSpeak.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startVoiceListening()
    }

    // Start listening as soon as the screen opens (asking for the mic the first time).
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.startVoiceListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    BackHandler(onBack = onDismiss)

    val pulse = rememberInfiniteTransition(label = "voicePulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val orbScale = when (phase) {
        VoicePhase.LISTENING -> 1f + volume * 0.35f
        VoicePhase.SPEAKING, VoicePhase.THINKING -> pulse.value
        VoicePhase.IDLE -> 1f
    }
    val colors = MaterialTheme.colorScheme
    val orbBrush = when (phase) {
        VoicePhase.LISTENING -> Brush.radialGradient(listOf(colors.primary, colors.tertiary))
        VoicePhase.THINKING -> Brush.radialGradient(listOf(colors.secondary, colors.primary))
        VoicePhase.SPEAKING -> Brush.radialGradient(listOf(colors.tertiary, colors.primary))
        VoicePhase.IDLE -> Brush.radialGradient(listOf(colors.surfaceVariant, colors.outline))
    }

    val statusText = when {
        !hasPermission -> "Microphone access is needed for voice chat."
        error != null -> error!!
        phase == VoicePhase.LISTENING -> "Listening… tap when you're done"
        phase == VoicePhase.THINKING -> "Thinking…"
        phase == VoicePhase.SPEAKING -> "Speaking… tap to interrupt"
        else -> "Tap to talk"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Voice chat", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "End voice chat") }
            }

            Spacer(Modifier.weight(1f))

            // The orb is the main control: talk, finish talking, or interrupt.
            Surface(
                onClick = {
                    when {
                        !hasPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        phase == VoicePhase.LISTENING -> viewModel.finishVoiceListening()
                        phase == VoicePhase.SPEAKING -> viewModel.interruptVoiceReply()
                        phase == VoicePhase.THINKING -> viewModel.stopGeneration()
                        else -> viewModel.startVoiceListening()
                    }
                },
                shape = CircleShape,
                color = colors.surface,
                modifier = Modifier
                    .size(180.dp)
                    .scale(orbScale)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(orbBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (phase) {
                            VoicePhase.LISTENING -> Icons.Default.Mic
                            VoicePhase.THINKING -> Icons.Default.Stop
                            VoicePhase.SPEAKING -> Icons.Default.VolumeUp
                            VoicePhase.IDLE -> Icons.Default.MicNone
                        },
                        contentDescription = when (phase) {
                            VoicePhase.LISTENING -> "Finish speaking"
                            VoicePhase.THINKING -> "Stop"
                            VoicePhase.SPEAKING -> "Interrupt"
                            VoicePhase.IDLE -> "Talk"
                        },
                        tint = colors.onPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) colors.error else colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )

            Spacer(Modifier.height(16.dp))
            // What Kora heard, then what it's saying.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (heard.isNotBlank() && phase != VoicePhase.SPEAKING) {
                    Text(
                        "“$heard”",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant
                    )
                }
                if (reply.isNotBlank() && phase != VoicePhase.LISTENING) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        SpeechText.clean(reply),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = handsFree,
                    onClick = { viewModel.setVoiceHandsFree(!handsFree) },
                    leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Hands-free") }
                )
                FilterChip(
                    selected = speakReplies,
                    onClick = { viewModel.toggleVoiceAutoSpeak(!speakReplies) },
                    leadingIcon = {
                        Icon(if (speakReplies) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    label = { Text("Speak replies") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Hands-free listens again after each answer. Replies are also saved in the chat.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

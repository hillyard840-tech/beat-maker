package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CanvasScreen
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.DashboardScreen
import com.example.ui.VoiceChatScreen
import com.example.ui.clawearn.ClawEarnScreen
import com.example.ui.moltbook.MoltbookScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ACTION = "EXTRA_ACTION"
    }

    private var chatViewModel: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only handle a widget action on a fresh launch. After a configuration change (e.g.
        // rotation) the same intent is redelivered, and re-running "new_chat" would wipe the
        // conversation again.
        val action = if (savedInstanceState == null) consumeWidgetAction(intent) else null

        setContent {
            MyApplicationTheme {
                val viewModel: ChatViewModel = viewModel()
                chatViewModel = viewModel

                LaunchedEffect(action) {
                    if (!action.isNullOrBlank()) {
                        viewModel.handleWidgetAction(action, applicationContext)
                    }
                }

                var showDashboard by remember { mutableStateOf(false) }
                val isMoltbookOpen by viewModel.isMoltbookOpen.collectAsStateWithLifecycle()
                val isClawEarnOpen by viewModel.isClawEarnOpen.collectAsStateWithLifecycle()
                val isCanvasOpen by viewModel.isCanvasOpen.collectAsStateWithLifecycle()
                val isVoiceModeOpen by viewModel.isVoiceModeOpen.collectAsStateWithLifecycle()

                when {
                    isMoltbookOpen -> {
                        MoltbookScreen(
                            onClose = { viewModel.closeMoltbook() },
                            onOpenInCanvas = { title, content ->
                                viewModel.closeMoltbook()
                                viewModel.openCanvas(title, content, "markdown")
                            }
                        )
                    }
                    isClawEarnOpen -> {
                        ClawEarnScreen(onClose = { viewModel.closeClawEarn() })
                    }
                    isCanvasOpen -> {
                        CanvasScreen(
                            viewModel = viewModel,
                            onClose = { viewModel.closeCanvas() }
                        )
                    }
                    showDashboard -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onBack = { showDashboard = false },
                            onOpenClawEarn = {
                                showDashboard = false
                                viewModel.openClawEarn()
                            },
                            onOpenMoltbook = {
                                showDashboard = false
                                viewModel.openMoltbook()
                            }
                        )
                    }
                    else -> {
                        ChatScreen(
                            viewModel = viewModel,
                            onDashboardClick = { showDashboard = true },
                            onClawEarnClick = { viewModel.openClawEarn() },
                            onMoltbookClick = { viewModel.openMoltbook() }
                        )
                    }
                }

                val showClearHistoryConfirm by viewModel.showClearHistoryConfirm.collectAsStateWithLifecycle()
                if (showClearHistoryConfirm) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showClearHistoryConfirm.value = false },
                        title = { Text("Delete this chat?") },
                        text = { Text("All messages and attached images in this chat will be permanently deleted. Your other chats are kept.") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.clearHistory(applicationContext) }) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.showClearHistoryConfirm.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (isVoiceModeOpen) {
                    VoiceChatScreen(
                        viewModel = viewModel,
                        onDismiss = { viewModel.stopVoiceChat() }
                    )
                }
            }
        }
    }

    private fun consumeWidgetAction(intent: Intent?): String? {
        val action = intent?.getStringExtra(EXTRA_ACTION)
        intent?.removeExtra(EXTRA_ACTION)
        return action
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = consumeWidgetAction(intent)
        if (!action.isNullOrBlank()) {
            chatViewModel?.handleWidgetAction(action, applicationContext)
        }
    }
}

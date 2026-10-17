package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.ui.OpportunitiesScreen
import com.example.ui.RemindersScreen
import com.example.ui.VoiceChatScreen
import com.example.ui.clawearn.ClawEarnScreen
import com.example.ui.moltbook.MoltbookScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ACTION = "EXTRA_ACTION"
        /** Set by reminder and task notifications: the chat to open. */
        const val EXTRA_CONVERSATION_ID = "EXTRA_CONVERSATION_ID"
    }

    private var chatViewModel: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only handle a widget action on a fresh launch. After a configuration change (e.g.
        // rotation) the same intent is redelivered, and re-running "new_chat" would wipe the
        // conversation again.
        val action = if (savedInstanceState == null) consumeWidgetAction(intent) else null
        val openChatId = if (savedInstanceState == null) consumeConversationId(intent) else null

        setContent {
            val viewModel: ChatViewModel = viewModel()
            chatViewModel = viewModel
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ChatViewModel.THEME_LIGHT -> false
                ChatViewModel.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {

                LaunchedEffect(action) {
                    if (!action.isNullOrBlank()) {
                        viewModel.handleWidgetAction(action, applicationContext)
                    }
                }
                LaunchedEffect(openChatId) {
                    openChatId?.let { viewModel.openConversation(it) }
                }

                var showDashboard by remember { mutableStateOf(false) }
                val isMoltbookOpen by viewModel.isMoltbookOpen.collectAsStateWithLifecycle()
                val isClawEarnOpen by viewModel.isClawEarnOpen.collectAsStateWithLifecycle()
                val isCanvasOpen by viewModel.isCanvasOpen.collectAsStateWithLifecycle()
                val isVoiceModeOpen by viewModel.isVoiceModeOpen.collectAsStateWithLifecycle()
                val isRemindersOpen by viewModel.isRemindersOpen.collectAsStateWithLifecycle()
                val isOpportunitiesOpen by viewModel.isOpportunitiesOpen.collectAsStateWithLifecycle()

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
                    isOpportunitiesOpen -> {
                        OpportunitiesScreen(
                            onBack = { viewModel.isOpportunitiesOpen.value = false },
                            onAskKora = { prompt ->
                                viewModel.isOpportunitiesOpen.value = false
                                viewModel.newChat()
                                viewModel.sendMessage(prompt, null)
                            },
                            onOpenCanvas = { title, markdown ->
                                viewModel.isOpportunitiesOpen.value = false
                                viewModel.openCanvas(title, markdown, "markdown")
                            },
                            onOpenAgentJobs = {
                                viewModel.isOpportunitiesOpen.value = false
                                viewModel.openClawEarn()
                            }
                        )
                    }
                    isRemindersOpen -> {
                        RemindersScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.isRemindersOpen.value = false }
                        )
                    }
                    showDashboard -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onBack = { showDashboard = false },
                            onOpenReminders = {
                                showDashboard = false
                                viewModel.isRemindersOpen.value = true
                            },
                            onOpenOpportunities = {
                                showDashboard = false
                                viewModel.isOpportunitiesOpen.value = true
                            },
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

    private fun consumeConversationId(intent: Intent?): Int? {
        val id = intent?.getIntExtra(EXTRA_CONVERSATION_ID, -1) ?: -1
        intent?.removeExtra(EXTRA_CONVERSATION_ID)
        return id.takeIf { it > 0 }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = consumeWidgetAction(intent)
        if (!action.isNullOrBlank()) {
            chatViewModel?.handleWidgetAction(action, applicationContext)
        }
        consumeConversationId(intent)?.let { chatViewModel?.openConversation(it) }
    }
}

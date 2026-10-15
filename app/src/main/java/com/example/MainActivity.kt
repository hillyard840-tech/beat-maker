package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CanvasScreen
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.DashboardScreen
import com.example.ui.VoiceModeOverlay
import com.example.ui.moltbook.MoltbookHubScreen
import com.example.ui.seekclaw.SeekClawHubScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var chatViewModel: ChatViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val action = intent?.getStringExtra("EXTRA_ACTION")

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
                val isSeekClawOpen by viewModel.isSeekClawOpen.collectAsStateWithLifecycle()
                val isCanvasOpen by viewModel.isCanvasOpen.collectAsStateWithLifecycle()
                val isVoiceModeOpen by viewModel.isVoiceModeOpen.collectAsStateWithLifecycle()

                when {
                    isMoltbookOpen -> {
                        MoltbookHubScreen(
                            viewModel = viewModel,
                            onClose = { viewModel.closeMoltbook() },
                            onOpenInCanvas = { title, content ->
                                viewModel.closeMoltbook()
                                viewModel.openCanvas(title, content, "markdown")
                            }
                        )
                    }
                    isSeekClawOpen -> {
                        SeekClawHubScreen(
                            viewModel = viewModel,
                            onClose = { viewModel.closeSeekClaw() },
                            onOpenInCanvas = { title, content ->
                                viewModel.closeSeekClaw()
                                viewModel.openCanvas(title, content, "markdown")
                            }
                        )
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
                            onOpenSeekClaw = {
                                showDashboard = false
                                viewModel.openSeekClaw()
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
                            onSeekClawClick = { viewModel.openSeekClaw() },
                            onMoltbookClick = { viewModel.openMoltbook() }
                        )
                    }
                }

                if (isVoiceModeOpen) {
                    VoiceModeOverlay(
                        viewModel = viewModel,
                        onDismiss = { viewModel.isVoiceModeOpen.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.getStringExtra("EXTRA_ACTION")
        if (!action.isNullOrBlank()) {
            chatViewModel?.handleWidgetAction(action, applicationContext)
        }
    }
}

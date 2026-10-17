package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.agent.AgentTools
import com.example.data.AgentAction
import com.example.ui.theme.status

/**
 * A phone action Kora proposed. Nothing happens until Confirm is tapped; any permission the
 * action needs is asked for at that moment.
 */
@Composable
fun ActionCard(
    action: AgentAction,
    onConfirm: (AgentAction) -> Unit,
    onCancel: (AgentAction) -> Unit,
    onCalendarPermission: (AgentAction, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // A reminder is still saved without notifications; the card then says how to turn them on.
        onConfirm(action)
    }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onCalendarPermission(action, granted)
    }

    fun confirm() {
        when {
            action.tool == AgentTools.GRANT_CALENDAR -> calendarPermission.launch(Manifest.permission.READ_CALENDAR)
            (action.tool == AgentTools.SET_REMINDER || action.tool == AgentTools.SCHEDULE_TASK) &&
                Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> onConfirm(action)
        }
    }

    val pending = action.status == AgentAction.STATUS_PENDING
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("action_card_${action.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (pending) colors.secondaryContainer else colors.surfaceVariant,
            contentColor = if (pending) colors.onSecondaryContainer else colors.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(action.tool), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(action.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (action.details.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(action.details, style = MaterialTheme.typography.bodyMedium, maxLines = 8)
            }
            Spacer(Modifier.height(12.dp))
            if (pending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirm() }, modifier = Modifier.testTag("action_confirm_${action.id}")) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (action.tool == AgentTools.GRANT_CALENDAR) "Allow" else "Confirm")
                    }
                    OutlinedButton(onClick = { onCancel(action) }) { Text("Cancel") }
                }
            } else {
                val (icon, tint, label) = when (action.status) {
                    AgentAction.STATUS_DONE -> Triple(Icons.Default.CheckCircle, MaterialTheme.status.success, action.resultNote ?: "Done")
                    AgentAction.STATUS_FAILED -> Triple(Icons.Default.ErrorOutline, colors.error, action.resultNote ?: "Couldn't do this")
                    else -> Triple(Icons.Default.Block, colors.onSurfaceVariant, "Cancelled")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun iconFor(tool: String): ImageVector = when (tool) {
    AgentTools.SET_REMINDER -> Icons.Default.NotificationsActive
    AgentTools.SET_ALARM -> Icons.Default.Alarm
    AgentTools.SET_TIMER -> Icons.Default.Timer
    AgentTools.ADD_CALENDAR_EVENT, AgentTools.GRANT_CALENDAR -> Icons.Default.Event
    AgentTools.DRAFT_TEXT -> Icons.Default.Sms
    AgentTools.DRAFT_EMAIL -> Icons.Default.Email
    AgentTools.OPEN_MAP -> Icons.Default.Map
    AgentTools.DIAL_NUMBER -> Icons.Default.Call
    AgentTools.OPEN_APP -> Icons.Default.Apps
    AgentTools.SCHEDULE_TASK -> Icons.Default.Schedule
    else -> Icons.Default.TouchApp
}

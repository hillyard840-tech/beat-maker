package com.example.agent

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/** Kora's notifications: reminders (urgent) and scheduled task results (normal). */
object Notifications {
    const val CHANNEL_REMINDERS = "kora_reminders"
    const val CHANNEL_TASKS = "kora_tasks"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders you asked Kora to set"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TASKS, "Scheduled tasks", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Answers from tasks Kora runs on a schedule, like a morning briefing"
            }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }

    /** Posts a notification that opens chat [conversationId] (or the app) when tapped. */
    fun post(context: Context, id: Int, channel: String, title: String, text: String, conversationId: Int?) {
        ensureChannels(context)
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            conversationId?.let { putExtra(MainActivity.EXTRA_CONVERSATION_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channel) else Notification.Builder(context)
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && channel == CHANNEL_REMINDERS) {
                    @Suppress("DEPRECATION")
                    setPriority(Notification.PRIORITY_HIGH)
                    @Suppress("DEPRECATION")
                    setDefaults(Notification.DEFAULT_ALL)
                }
            }
            .build()
        try {
            context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }
}

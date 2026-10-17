package com.example.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stores reminders and arms an alarm for each one. */
object ReminderScheduler {
    private const val EXTRA_ID = "reminder_id"
    // Request codes keep reminder and task alarms apart.
    internal fun requestCode(id: Int) = 100_000 + id

    suspend fun add(context: Context, reminder: Reminder): Int {
        val id = AppDatabase.getInstance(context).reminderDao().insert(reminder).toInt()
        arm(context, reminder.copy(id = id))
        return id
    }

    fun arm(context: Context, reminder: Reminder) {
        if (reminder.isDone) return
        AlarmScheduling.set(context, reminder.triggerAt, pending(context, reminder.id))
    }

    suspend fun delete(context: Context, id: Int) {
        AlarmScheduling.cancel(context, pending(context, id))
        AppDatabase.getInstance(context).reminderDao().delete(id)
    }

    /** After a reboot or update alarms are gone; re-arm everything still pending. */
    suspend fun rearmAll(context: Context) {
        AppDatabase.getInstance(context).reminderDao().pendingOnce().forEach { arm(context, it) }
    }

    internal fun idFrom(intent: Intent): Int = intent.getIntExtra(EXTRA_ID, -1)

    private fun pending(context: Context, id: Int) =
        AlarmScheduling.broadcast(context, ReminderReceiver::class.java, requestCode(id), EXTRA_ID, id)
}

/** Fires when a reminder is due: shows the notification and marks it done. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = ReminderScheduler.idFrom(intent)
        if (id < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).reminderDao()
                val reminder = dao.getById(id)
                if (reminder != null && !reminder.isDone) {
                    Notifications.post(
                        context,
                        id = ReminderScheduler.requestCode(id),
                        channel = Notifications.CHANNEL_REMINDERS,
                        title = reminder.title,
                        text = reminder.note.ifBlank { "Reminder from Kora" },
                        conversationId = reminder.conversationId
                    )
                    dao.markDone(id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms don't survive a reboot or app update, so re-arm reminders and tasks. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderScheduler.rearmAll(context)
                TaskScheduler.rearmAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}

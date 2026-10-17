package com.example.agent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.data.AgentAction
import com.example.data.Reminder
import com.example.data.ScheduledTask
import com.example.util.AgentTime
import java.util.Locale

/** Carries out a confirmed action card. Each result is a short note shown on the card. */
object PhoneActions {
    data class Outcome(val ok: Boolean, val note: String)

    data class InstalledApp(val label: String, val packageName: String)

    /** Finds a launchable app whose name matches [name] (exact match first, then contains). */
    fun findApp(context: Context, name: String): InstalledApp? {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = try {
            pm.queryIntentActivities(launcher, 0).map {
                InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName)
            }
        } catch (_: Exception) {
            emptyList()
        }
        val wanted = name.trim().lowercase(Locale.getDefault())
        if (wanted.isEmpty()) return null
        return apps.firstOrNull { it.label.lowercase(Locale.getDefault()) == wanted }
            ?: apps.filter { it.label.lowercase(Locale.getDefault()).contains(wanted) }.minByOrNull { it.label.length }
    }

    suspend fun execute(context: Context, action: AgentAction): Outcome {
        val args = AgentTools.argsOf(action.argsJson)
        fun s(key: String) = AgentTools.string(args, key)
        fun i(key: String) = AgentTools.int(args, key)

        return when (action.tool) {
            AgentTools.SET_REMINDER -> {
                val at = AgentTime.parseLocal(s("when")) ?: return Outcome(false, "Invalid time")
                if (at <= System.currentTimeMillis()) return Outcome(false, "That time has passed")
                ReminderScheduler.add(
                    context,
                    Reminder(title = s("title") ?: "Reminder", note = s("note").orEmpty(), triggerAt = at, conversationId = action.conversationId)
                )
                val exact = AlarmScheduling.canScheduleExact(context)
                val notifications = Notifications.canPost(context)
                Outcome(
                    true,
                    "Reminder set for ${AgentTime.label(at)}" +
                        (if (!notifications) ". Notifications are off, so it can't alert you; turn them on in Reminders & tasks." else "") +
                        (if (notifications && !exact) ". It may arrive a few minutes late; allow exact alarms in Reminders & tasks." else "")
                )
            }
            AgentTools.SET_ALARM -> launch(
                context,
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, i("hour") ?: 0)
                    .putExtra(AlarmClock.EXTRA_MINUTES, i("minute") ?: 0)
                    .apply { s("label")?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } },
                "Alarm sent to your clock app",
                "No clock app can set alarms"
            )
            AgentTools.SET_TIMER -> launch(
                context,
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, i("seconds") ?: 60)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    .apply { s("label")?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } },
                "Timer sent to your clock app",
                "No clock app can set timers"
            )
            AgentTools.ADD_CALENDAR_EVENT -> {
                val start = AgentTime.parseLocal(s("start")) ?: return Outcome(false, "Invalid start time")
                val end = AgentTime.parseLocal(s("end"))?.takeIf { it > start } ?: (start + 60 * 60 * 1000L)
                launch(
                    context,
                    Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, s("title"))
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                        .apply {
                            s("location")?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
                            s("description")?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
                        },
                    "Opened in your calendar; tap Save there",
                    "No calendar app found"
                )
            }
            AgentTools.DRAFT_TEXT -> launch(
                context,
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(s("to").orEmpty())))
                    .putExtra("sms_body", s("body").orEmpty()),
                "Draft opened in Messages",
                "No messaging app found"
            )
            AgentTools.DRAFT_EMAIL -> launch(
                context,
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(s("to").orEmpty())))
                    .putExtra(Intent.EXTRA_SUBJECT, s("subject").orEmpty())
                    .putExtra(Intent.EXTRA_TEXT, s("body").orEmpty()),
                "Draft opened in your email app",
                "No email app found"
            )
            AgentTools.OPEN_MAP -> launch(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(s("query").orEmpty()))),
                "Opened in Maps",
                "No maps app found"
            )
            AgentTools.DIAL_NUMBER -> launch(
                context,
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(s("number").orEmpty().filter { it.isDigit() || it in "+*#" }))),
                "Number ready in the dialer",
                "No dialer found"
            )
            AgentTools.OPEN_APP -> {
                val pkg = s(AgentTools.ARG_PACKAGE) ?: return Outcome(false, "App not found")
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    ?: return Outcome(false, "That app can't be opened")
                launch(context, intent, "Opened", "That app can't be opened")
            }
            AgentTools.SCHEDULE_TASK -> {
                val hour = i("hour") ?: return Outcome(false, "Invalid time")
                val minute = i("minute") ?: 0
                val repeat = AgentTools.normalizeRepeat(s("repeat"))
                val id = TaskScheduler.save(
                    context,
                    ScheduledTask(
                        title = s("title") ?: "Task",
                        prompt = s("prompt").orEmpty(),
                        hour = hour.coerceIn(0, 23),
                        minute = minute.coerceIn(0, 59),
                        repeat = repeat
                    )
                )
                val next = com.example.data.AppDatabase.getInstance(context).scheduledTaskDao().getById(id)?.nextRunAt
                Outcome(true, "Scheduled" + (next?.let { ". First run ${AgentTime.label(it)}" } ?: ""))
            }
            else -> Outcome(false, "This action isn't supported")
        }
    }

    private fun launch(context: Context, intent: Intent, success: String, missing: String): Outcome {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            Outcome(true, success)
        } catch (_: ActivityNotFoundException) {
            Outcome(false, missing)
        } catch (_: SecurityException) {
            Outcome(false, missing)
        }
    }
}

package com.example.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.income.JobBoards
import com.example.util.AgentTime
import com.example.util.LiveInternetService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement

/** Tools that only read (the web, the calendar, Kora's own lists) and answer right away. */
object ReadTools {

    suspend fun searchWeb(query: String): String {
        val results = LiveInternetService.searchLiveInternet(query)
        if (results.isEmpty()) return "No direct search results found for query: \"$query\"."
        return buildString {
            appendLine("Direct Live Internet Search Results for: \"$query\":")
            results.forEachIndexed { i, item ->
                appendLine("[${i + 1}] ${item.title}")
                appendLine("URL: ${item.url}")
                appendLine("Snippet: ${item.snippet}")
                appendLine()
            }
        }
    }

    suspend fun fetchWebsite(url: String): String {
        val page = LiveInternetService.fetchLiveWebpage(url)
        return buildString {
            appendLine("Direct Live Webpage Content:")
            appendLine("URL: ${page.url}")
            appendLine("Title: ${page.title}")
            if (page.metaDescription.isNotEmpty()) appendLine("Description: ${page.metaDescription}")
            appendLine("Text Content:\n${page.content}")
        }
    }

    fun canReadCalendar(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Upcoming events from every calendar on the phone, or null without permission. */
    suspend fun readCalendar(context: Context, days: Int): String? = withContext(Dispatchers.IO) {
        if (!canReadCalendar(context)) return@withContext null
        val span = days.coerceIn(1, 14)
        val start = System.currentTimeMillis()
        val end = start + span * 24L * 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            android.content.ContentUris.appendId(it, start)
            android.content.ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )
        val lines = mutableListOf<String>()
        try {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext() && lines.size < 50) {
                    val title = c.getString(0)?.ifBlank { null } ?: "(no title)"
                    val begin = c.getLong(1)
                    val allDay = c.getInt(3) == 1
                    val location = c.getString(4)?.takeIf { it.isNotBlank() }
                    val whenText = if (allDay) "${AgentTime.label(begin).substringBefore(',').trim()} (all day)" else AgentTime.label(begin)
                    lines += "• $whenText — $title" + (location?.let { " @ $it" } ?: "")
                }
            }
        } catch (e: SecurityException) {
            return@withContext null
        }
        if (lines.isEmpty()) "No events in the next $span day(s)."
        else "Calendar events for the next $span day(s):\n" + lines.joinToString("\n")
    }

    suspend fun listRemindersAndTasks(context: Context): String {
        val db = AppDatabase.getInstance(context)
        val reminders = db.reminderDao().pendingOnce()
        val tasks = db.scheduledTaskDao().allOnce()
        return buildString {
            if (reminders.isEmpty()) appendLine("No upcoming reminders.")
            else {
                appendLine("Upcoming reminders:")
                reminders.forEach { appendLine("• ${AgentTime.label(it.triggerAt)} — ${it.title}") }
            }
            if (tasks.isEmpty()) appendLine("No scheduled tasks.")
            else {
                appendLine("Scheduled tasks:")
                tasks.forEach {
                    appendLine("• ${it.title}: ${AgentTime.repeatLabel(it.repeat)} at ${AgentTime.clock(it.hour, it.minute)}" + if (it.enabled) "" else " (paused)")
                }
            }
            append("The user can manage these under Reminders & tasks.")
        }
    }

    /** Runs a read tool. Returns null for tools this object doesn't handle. */
    suspend fun run(context: Context, name: String, args: Map<String, JsonElement>?, fallbackQuery: String = ""): String? = when (name) {
        AgentTools.SEARCH_WEB -> searchWeb(AgentTools.string(args, "query") ?: fallbackQuery)
        AgentTools.FETCH_WEBSITE -> fetchWebsite(AgentTools.string(args, "url").orEmpty())
        AgentTools.SEARCH_JOBS -> JobBoards.describeForModel(
            JobBoards.search(AgentTools.string(args, "query") ?: fallbackQuery, remoteOnly = AgentTools.string(args, "remote_only") != "false")
        )
        AgentTools.FIND_BUILD_DEMAND -> JobBoards.describeDemand(JobBoards.demandSignals(AgentTools.string(args, "topic") ?: fallbackQuery))
        AgentTools.READ_CALENDAR -> readCalendar(context, AgentTools.int(args, "days") ?: 1)
            ?: "Calendar access hasn't been granted. A card asking for it is shown to the user; tell them to tap Allow and then ask again."
        AgentTools.LIST_REMINDERS -> listRemindersAndTasks(context)
        else -> null
    }
}

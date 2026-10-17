package com.example.util

import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date and time helpers for reminders, calendar events and scheduled tasks. Calendar based so
 * it works on every supported Android version without desugaring.
 */
object AgentTime {
    private val PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )

    /**
     * Parses a local date-time such as "2026-10-18T07:30" (the format the model is asked for).
     * A trailing "Z" or UTC offset is honoured; otherwise [zone] is used. Returns null if the
     * text isn't a complete, valid date.
     */
    fun parseLocal(text: String?, zone: TimeZone = TimeZone.getDefault()): Long? {
        var raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return null
        var effectiveZone = zone
        val offset = Regex("""(Z|[+-]\d{2}:?\d{2})$""").find(raw)
        if (offset != null && raw.length > 10) {
            val o = offset.value
            effectiveZone = if (o == "Z") TimeZone.getTimeZone("UTC") else TimeZone.getTimeZone("GMT" + if (o.contains(':')) o else o.substring(0, 3) + ":" + o.substring(3))
            raw = raw.removeSuffix(o)
        }
        raw = raw.replace(Regex("""\.\d+$"""), "") // drop fractional seconds
        for (pattern in PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = effectiveZone
            }
            val position = ParsePosition(0)
            val date = format.parse(raw, position) ?: continue
            if (position.index == raw.length) return date.time
        }
        return null
    }

    /**
     * Next time a task at [hour]:[minute] should run, strictly after [now]. "weekdays" skips
     * Saturday and Sunday, "sundays" runs weekly on Sunday; "daily" and "once" take the next
     * matching clock time.
     */
    fun nextRun(hour: Int, minute: Int, repeat: String, now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        if (repeat == "sundays") {
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (repeat == "weekdays") {
            while (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    /** "Saturday, 2026-10-17 14:05 (America/New_York, UTC-04:00)" for the system prompt. */
    fun describeNow(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("EEEE, yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = zone }
        val offsetMinutes = zone.getOffset(now) / 60_000
        val sign = if (offsetMinutes < 0) "-" else "+"
        val abs = kotlin.math.abs(offsetMinutes)
        val offset = String.format(Locale.US, "UTC%s%02d:%02d", sign, abs / 60, abs % 60)
        return "${format.format(Date(now))} (${zone.id}, $offset)"
    }

    /** Short, local, human label such as "Sat, Oct 18, 7:30 AM". */
    fun label(millis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

    fun clock(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)
    }

    /** "Every day", "Weekdays" or "Once". */
    fun repeatLabel(repeat: String): String = when (repeat) {
        "weekdays" -> "Weekdays"
        "sundays" -> "Sundays"
        "once" -> "Once"
        else -> "Every day"
    }

    /** "1 h 5 min", "45 s" — for timers. */
    fun duration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return listOfNotNull(
            if (h > 0) "$h h" else null,
            if (m > 0) "$m min" else null,
            if (s > 0 || (h == 0 && m == 0)) "$s s" else null
        ).joinToString(" ")
    }
}

package com.example.util

import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** Decides where the chat shows a time separator, and what it says. */
object ChatTimeline {
    private const val GAP_MILLIS = 60 * 60 * 1000L // one hour of silence starts a new "session"

    fun needsSeparator(previousTimestamp: Long?, timestamp: Long): Boolean {
        if (previousTimestamp == null) return true
        if (timestamp - previousTimestamp >= GAP_MILLIS) return true
        return !sameDay(previousTimestamp, timestamp)
    }

    fun label(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(timestamp))
        return when {
            sameDay(timestamp, now) -> "Today $time"
            sameDay(timestamp, now - 24 * 60 * 60 * 1000L) -> "Yesterday $time"
            else -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
        }
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}

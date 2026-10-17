package com.example.agent

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AgentToolsTest {
    private val zone = TimeZone.getTimeZone("UTC")
    private val now = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.OCTOBER, 17, 12, 0, 0) }.timeInMillis

    private fun args(vararg pairs: Pair<String, Any>) = pairs.associate { (k, v) ->
        k to when (v) {
            is Number -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
    }

    private fun prepare(tool: String, vararg pairs: Pair<String, Any>) = AgentTools.prepare(tool, args(*pairs), now, zone)

    @Test
    fun `reminder in the future becomes a card`() {
        val p = prepare(AgentTools.SET_REMINDER, "title" to "Call mom", "when" to "2026-10-17T18:00")
        assertTrue(p is AgentTools.Prepared.Card)
        assertEquals("Remind me: Call mom", (p as AgentTools.Prepared.Card).title)
    }

    @Test
    fun `reminder in the past or with a vague time is refused`() {
        assertTrue(prepare(AgentTools.SET_REMINDER, "title" to "x", "when" to "2026-10-17T09:00") is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.SET_REMINDER, "title" to "x", "when" to "tonight") is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.SET_REMINDER, "when" to "2026-10-18T09:00") is AgentTools.Prepared.Invalid)
    }

    @Test
    fun `numbers arrive as ints, doubles or strings`() {
        assertTrue(prepare(AgentTools.SET_ALARM, "hour" to 7, "minute" to 30) is AgentTools.Prepared.Card)
        assertTrue(prepare(AgentTools.SET_ALARM, "hour" to 7.0, "minute" to 30.0) is AgentTools.Prepared.Card)
        assertTrue(prepare(AgentTools.SET_ALARM, "hour" to "7", "minute" to "30") is AgentTools.Prepared.Card)
        assertTrue(prepare(AgentTools.SET_ALARM, "hour" to 24, "minute" to 0) is AgentTools.Prepared.Invalid)
    }

    @Test
    fun `timer bounds`() {
        assertTrue(prepare(AgentTools.SET_TIMER, "seconds" to 600) is AgentTools.Prepared.Card)
        assertTrue(prepare(AgentTools.SET_TIMER, "seconds" to 0) is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.SET_TIMER, "seconds" to 100_000) is AgentTools.Prepared.Invalid)
    }

    @Test
    fun `calendar event end must follow start`() {
        assertTrue(prepare(AgentTools.ADD_CALENDAR_EVENT, "title" to "Dentist", "start" to "2026-10-20T09:00") is AgentTools.Prepared.Card)
        assertTrue(
            prepare(AgentTools.ADD_CALENDAR_EVENT, "title" to "Dentist", "start" to "2026-10-20T09:00", "end" to "2026-10-20T08:00")
                is AgentTools.Prepared.Invalid
        )
    }

    @Test
    fun `dialer keeps only dialable characters`() {
        val p = prepare(AgentTools.DIAL_NUMBER, "number" to "+1 (555) 010-9999")
        assertEquals("Call +15550109999", (p as AgentTools.Prepared.Card).title)
        assertTrue(prepare(AgentTools.DIAL_NUMBER, "number" to "pizza place") is AgentTools.Prepared.Invalid)
    }

    @Test
    fun `open app needs a resolved package`() {
        assertTrue(prepare(AgentTools.OPEN_APP, "name" to "Spotify") is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.OPEN_APP, "name" to "Spotify", AgentTools.ARG_PACKAGE to "com.spotify.music") is AgentTools.Prepared.Card)
    }

    @Test
    fun `schedule task normalises repeat`() {
        assertEquals("weekdays", AgentTools.normalizeRepeat("Weekday"))
        assertEquals("once", AgentTools.normalizeRepeat("one-time"))
        assertEquals("daily", AgentTools.normalizeRepeat(null))
        assertTrue(prepare(AgentTools.SCHEDULE_TASK, "title" to "News", "prompt" to "Top news", "hour" to 8, "minute" to 0) is AgentTools.Prepared.Card)
        assertTrue(prepare(AgentTools.SCHEDULE_TASK, "title" to "News", "hour" to 8) is AgentTools.Prepared.Invalid)
    }

    @Test
    fun `web tools follow the switch and phone tools are always there`() {
        val on = AgentTools.forChat(webEnabled = true).flatMap { it.functionDeclarations.orEmpty() }.map { it.name }
        val off = AgentTools.forChat(webEnabled = false).flatMap { it.functionDeclarations.orEmpty() }.map { it.name }
        assertTrue(AgentTools.SEARCH_WEB in on)
        assertTrue(AgentTools.SEARCH_WEB !in off && AgentTools.FETCH_WEBSITE !in off)
        assertTrue(AgentTools.SET_REMINDER in off)
        // Every confirm tool is declared, and names are unique.
        assertTrue(on.containsAll(AgentTools.CONFIRM_TOOLS))
        assertEquals(on.size, on.toSet().size)
    }

    @Test
    fun `background tasks can only read`() {
        val names = AgentTools.forTask(webEnabled = true, calendarReadable = true)!!.flatMap { it.functionDeclarations.orEmpty() }.map { it.name }
        assertTrue(names.none { it in AgentTools.CONFIRM_TOOLS })
    }

    @Test
    fun `args round trip through json`() {
        val original = args("title" to "Call mom", "hour" to 7)
        assertEquals(original, AgentTools.argsOf(AgentTools.argsJson(original)))
        assertTrue(AgentTools.argsOf("not json").isEmpty())
    }

    @Test
    fun `log earning validates amount, hours and date`() {
        assertTrue(prepare(AgentTools.LOG_EARNING, "source" to "Fiverr", "amount" to 45.5) is AgentTools.Prepared.Card)
        assertEquals("Log 45.50 from Fiverr", (prepare(AgentTools.LOG_EARNING, "source" to "Fiverr", "amount" to "45.5") as AgentTools.Prepared.Card).title)
        assertTrue(prepare(AgentTools.LOG_EARNING, "source" to "Fiverr", "amount" to -3) is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.LOG_EARNING, "amount" to 10) is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.LOG_EARNING, "source" to "x", "amount" to 10, "date" to "2027-01-01") is AgentTools.Prepared.Invalid)
        assertTrue(prepare(AgentTools.LOG_EARNING, "source" to "x", "amount" to 10, "hours" to 0) is AgentTools.Prepared.Invalid)
        assertEquals("sundays", AgentTools.normalizeRepeat("weekly"))
    }

    @Test
    fun `earnings can be read in background tasks but not logged`() {
        val names = AgentTools.forTask(webEnabled = false, calendarReadable = false)!!.flatMap { it.functionDeclarations.orEmpty() }.map { it.name }
        assertEquals(listOf(AgentTools.READ_EARNINGS), names)
    }
}

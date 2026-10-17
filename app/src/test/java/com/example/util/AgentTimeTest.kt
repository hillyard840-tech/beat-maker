package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AgentTimeTest {
    private val zone = TimeZone.getTimeZone("America/New_York")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `parses the formats the model is asked for`() {
        val expected = at(2026, Calendar.OCTOBER, 18, 7, 30)
        assertEquals(expected, AgentTime.parseLocal("2026-10-18T07:30", zone))
        assertEquals(expected, AgentTime.parseLocal("2026-10-18 07:30", zone))
        assertEquals(expected, AgentTime.parseLocal("2026-10-18T07:30:00", zone))
        assertEquals(expected, AgentTime.parseLocal("2026-10-18T07:30:00.000", zone))
    }

    @Test
    fun `honours an explicit offset`() {
        assertEquals(at(2026, Calendar.OCTOBER, 18, 7, 30), AgentTime.parseLocal("2026-10-18T11:30Z", zone))
        assertEquals(at(2026, Calendar.OCTOBER, 18, 7, 30), AgentTime.parseLocal("2026-10-18T07:30-04:00", zone))
    }

    @Test
    fun `rejects junk and impossible dates`() {
        assertNull(AgentTime.parseLocal("tomorrow at 7", zone))
        assertNull(AgentTime.parseLocal("2026-02-30T07:00", zone))
        assertNull(AgentTime.parseLocal("2026-10-18T25:00", zone))
        assertNull(AgentTime.parseLocal("", zone))
        assertNull(AgentTime.parseLocal(null, zone))
    }

    @Test
    fun `next run is later today or tomorrow`() {
        val now = at(2026, Calendar.OCTOBER, 14, 6, 0) // a Wednesday
        assertEquals(at(2026, Calendar.OCTOBER, 14, 7, 30), AgentTime.nextRun(7, 30, "daily", now, zone))
        val later = at(2026, Calendar.OCTOBER, 14, 8, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 15, 7, 30), AgentTime.nextRun(7, 30, "daily", later, zone))
        // Exactly at the time counts as passed, so a task never runs twice in the same minute.
        assertEquals(at(2026, Calendar.OCTOBER, 15, 7, 30), AgentTime.nextRun(7, 30, "once", at(2026, Calendar.OCTOBER, 14, 7, 30), zone))
    }

    @Test
    fun `weekdays skip the weekend`() {
        val fridayEvening = at(2026, Calendar.OCTOBER, 16, 20, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 19, 8, 0), AgentTime.nextRun(8, 0, "weekdays", fridayEvening, zone))
        val saturday = at(2026, Calendar.OCTOBER, 17, 6, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 19, 8, 0), AgentTime.nextRun(8, 0, "weekdays", saturday, zone))
    }

    @Test
    fun `survives the daylight saving change`() {
        // US clocks go back on 2026-11-01; 7:30 must stay 7:30 local.
        val before = at(2026, Calendar.OCTOBER, 31, 9, 0)
        val next = AgentTime.nextRun(7, 30, "daily", before, zone)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = next }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `describes now with zone and offset`() {
        val text = AgentTime.describeNow(at(2026, Calendar.OCTOBER, 17, 14, 5), zone)
        assertEquals("Saturday, 2026-10-17 14:05 (America/New_York, UTC-04:00)", text)
    }

    @Test
    fun `durations read naturally`() {
        assertEquals("20 min", AgentTime.duration(1200))
        assertEquals("1 h 5 min", AgentTime.duration(3900))
        assertEquals("45 s", AgentTime.duration(45))
        assertTrue(AgentTime.duration(0).isNotEmpty())
    }

    @Test
    fun `sundays repeat weekly on sunday`() {
        val wednesday = at(2026, Calendar.OCTOBER, 14, 9, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 18, 18, 0), AgentTime.nextRun(18, 0, "sundays", wednesday, zone))
        val sundayEvening = at(2026, Calendar.OCTOBER, 18, 19, 0)
        assertEquals(at(2026, Calendar.OCTOBER, 25, 18, 0), AgentTime.nextRun(18, 0, "sundays", sundayEvening, zone))
    }
}

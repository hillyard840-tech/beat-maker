package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChatTimelineTest {
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply { set(2026, Calendar.MARCH, day, hour, minute, 0) }.timeInMillis

    @Test
    fun `first message and long gaps get a separator`() {
        assertTrue(ChatTimeline.needsSeparator(null, at(1, 9)))
        assertTrue(ChatTimeline.needsSeparator(at(1, 9), at(1, 10, 30)))
    }

    @Test
    fun `quick replies on the same day do not`() {
        assertFalse(ChatTimeline.needsSeparator(at(1, 9), at(1, 9, 5)))
    }

    @Test
    fun `a new day always gets a separator`() {
        assertTrue(ChatTimeline.needsSeparator(at(1, 23, 50), at(2, 0, 5)))
    }

    @Test
    fun `labels today and yesterday`() {
        val now = at(10, 12)
        assertTrue(ChatTimeline.label(at(10, 9), now).startsWith("Today"))
        assertTrue(ChatTimeline.label(at(9, 9), now).startsWith("Yesterday"))
    }
}

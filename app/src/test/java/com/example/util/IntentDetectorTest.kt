package com.example.util

import com.example.util.IntentDetector.RoutedIntent
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentDetectorTest {

    @Test
    fun `routes memory directives and extracts the fact`() {
        val result = IntentDetector.detect("Remember that I prefer Kotlin over Java")
        assertEquals(RoutedIntent.PERSISTENT_MEMORY, result.intent)
        assertEquals("I prefer Kotlin over Java", result.extractedParam)
    }

    @Test
    fun `routes image generation prompts`() {
        val result = IntentDetector.detect("draw a lighthouse at sunset")
        assertEquals(RoutedIntent.IMAGE_GEN, result.intent)
        assertEquals("lighthouse at sunset", result.extractedParam)
    }

    @Test
    fun `routes deep research and strips the command prefix`() {
        val result = IntentDetector.detect("deep research on solid state batteries")
        assertEquals(RoutedIntent.DEEP_RESEARCH, result.intent)
        assertEquals("solid state batteries", result.extractedParam)
    }

    @Test
    fun `flags time sensitive questions for live web`() {
        val result = IntentDetector.detect("What is the weather in Lisbon today?")
        assertEquals(RoutedIntent.STANDARD_WITH_LIVE_WEB, result.intent)
        assertEquals(true, result.autoTriggerLiveWeb)
    }

    @Test
    fun `plain questions stay standard`() {
        assertEquals(RoutedIntent.STANDARD, IntentDetector.detect("Explain how a hash map works").intent)
    }

    @Test
    fun `common words alone do not trigger a web search`() {
        listOf(
            "What is the current best practice for Kotlin coroutines?",
            "Explain how live templates work in Android Studio",
            "Now write me a poem"
        ).forEach { prompt ->
            assertEquals(prompt, RoutedIntent.STANDARD, IntentDetector.detect(prompt).intent)
        }
    }

    @Test
    fun `routes explicit integration requests`() {
        assertEquals(RoutedIntent.MOLTBOOK, IntentDetector.detect("open moltbook").intent)
        assertEquals(RoutedIntent.CLAW_EARN, IntentDetector.detect("show me claw earn tasks").intent)
    }

    @Test
    fun `remember to is a reminder, not a memory`() {
        org.junit.Assert.assertNotEquals(
            IntentDetector.RoutedIntent.PERSISTENT_MEMORY,
            IntentDetector.detect("remember to call mom at 6pm").intent
        )
        org.junit.Assert.assertEquals(
            IntentDetector.RoutedIntent.PERSISTENT_MEMORY,
            IntentDetector.detect("remember that I live in Denver").intent
        )
    }
}

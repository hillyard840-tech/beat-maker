package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun `strips markdown so it reads naturally`() {
        val spoken = SpeechText.clean("## Plan\n- **Buy** milk\n- See [the docs](https://x.com) or https://y.com 🎉")
        assertEquals("Plan\nBuy milk\nSee the docs or a link", spoken)
    }

    @Test
    fun `code blocks are not read out`() {
        val spoken = SpeechText.clean("Here:\n```kotlin\nval x = 1\n```\nDone.")
        assertFalse(spoken.contains("val x"))
        assertTrue(spoken.contains("Code is shown on screen"))
    }

    @Test
    fun `an unclosed fence while streaming stops the speakable text`() {
        assertEquals("Here is the code:", SpeechText.clean("Here is the code:\n```python\nprint(1"))
    }

    @Test
    fun `tables read as comma separated rows`() {
        val spoken = SpeechText.clean("| Name | Age |\n|---|---|\n| Ann | 30 |")
        assertEquals("Name , Age\nAnn , 30", spoken)
    }

    @Test
    fun `chunks only complete sentences until the reply is final`() {
        val text = "Hello there. How are you? I am fin"
        val (first, offset) = SpeechText.nextChunk(text, 0, final = false)
        assertEquals("Hello there. How are you?", first)
        val (none, same) = SpeechText.nextChunk(text, offset, final = false)
        assertEquals("", none)
        assertEquals(offset, same)
        val (rest, end) = SpeechText.nextChunk("$text e.", offset, final = true)
        assertEquals("I am fin e.", rest)
        assertEquals(text.length + 3, end)
    }

    @Test
    fun `newlines count as sentence breaks`() {
        assertEquals("First point" to 12, SpeechText.nextChunk("First point\nSecond", 0, final = false))
    }
}

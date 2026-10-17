package com.example.income

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaParserTest {
    @Test
    fun `reads a fenced json array and keeps only real source links`() {
        val text = """
            Here you go:
            ```json
            [{"title":"Shopify speed audits","summary":"Audit small stores' page speed for a fee.","steps":["Pick a niche","Audit 3 stores free"],
              "startupCost":"$0","timeToFirstIncome":"2-4 weeks","earnings":"$100-300 per audit","risks":"Crowded market",
              "sources":["https://real.example/a","https://made-up.example/b"]},
             {"title":"","summary":"no title, skipped"}]
            ```
        """.trimIndent()
        val ideas = IdeaParser.parse(text, allowedUrls = setOf("https://real.example/a"))
        assertEquals(1, ideas.size)
        assertEquals(listOf("https://real.example/a"), ideas[0].sources)
        assertEquals(2, ideas[0].steps.size)
        assertTrue(ideas[0].toMarkdown().contains("## First steps"))
    }

    @Test
    fun `accepts alternative key names`() {
        val ideas = IdeaParser.parse("""[{"name":"Invoice bot","description":"Sends reminders","first_steps":["a"],"who_pays":"Freelancers","techStack":"Kotlin"}]""")
        assertEquals("Invoice bot", ideas[0].title)
        assertEquals("Freelancers", ideas[0].whoPays)
        assertEquals("Kotlin", ideas[0].stack)
    }

    @Test
    fun `garbage gives nothing`() {
        assertTrue(IdeaParser.parse("Sorry, I can't help with that.").isEmpty())
        assertTrue(IdeaParser.parse("[not json]").isEmpty())
    }
}

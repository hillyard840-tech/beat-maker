package com.example.util

import com.example.util.Markdown.Block
import com.example.util.Markdown.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    @Test
    fun `parses headings paragraphs and rules`() {
        val blocks = Markdown.parse("# Title\n\nFirst line\nsecond line\n\n---\n### Small")
        assertEquals(4, blocks.size)
        val h = blocks[0] as Block.Heading
        assertEquals(1, h.level)
        assertEquals("Title", Markdown.plainText(h.runs))
        assertEquals("First line\nsecond line", Markdown.plainText((blocks[1] as Block.Paragraph).runs))
        assertTrue(blocks[2] is Block.Rule)
        assertEquals(3, (blocks[3] as Block.Heading).level)
    }

    @Test
    fun `parses inline emphasis code and links`() {
        val runs = Markdown.parseInline("Use **bold**, *italic*, ~~old~~, `code` and [docs](https://example.com).")
        assertEquals(Run("bold", bold = true), runs.first { it.text == "bold" })
        assertEquals(Run("italic", italic = true), runs.first { it.text == "italic" })
        assertTrue(runs.first { it.text == "old" }.strike)
        assertTrue(runs.first { it.text == "code" }.code)
        assertEquals("https://example.com", runs.first { it.text == "docs" }.url)
        assertEquals("Use bold, italic, old, code and docs.", Markdown.plainText(runs))
    }

    @Test
    fun `nested emphasis and bold italic`() {
        val runs = Markdown.parseInline("***both*** and **bold with *italic* inside**")
        val both = runs.first { it.text == "both" }
        assertTrue(both.bold && both.italic)
        val inner = runs.first { it.text == "italic" }
        assertTrue(inner.bold && inner.italic)
    }

    @Test
    fun `leaves snake_case math and unmatched markers alone`() {
        assertEquals("my_variable_name", Markdown.plainText(Markdown.parseInline("my_variable_name")))
        assertEquals("2 * 3 * 4", Markdown.plainText(Markdown.parseInline("2 * 3 * 4")))
        assertEquals("**not closed", Markdown.plainText(Markdown.parseInline("**not closed")))
        assertEquals("a*b", Markdown.plainText(Markdown.parseInline("a\\*b")))
    }

    @Test
    fun `bare urls become links without trailing punctuation`() {
        val runs = Markdown.parseInline("See https://example.com/a?b=1. Thanks")
        assertEquals("https://example.com/a?b=1", runs.first { it.url != null }.url)
        assertEquals("See https://example.com/a?b=1. Thanks", Markdown.plainText(runs))
    }

    @Test
    fun `unsafe link schemes are not tappable`() {
        val runs = Markdown.parseInline("[click](javascript:alert(1)) and [file](file:///etc/passwd)")
        assertTrue(runs.all { it.url == null })
        assertTrue(Markdown.plainText(runs).startsWith("click"))
    }

    @Test
    fun `parses bullet numbered nested and task lists`() {
        val blocks = Markdown.parse("- one\n- two\n  - nested\n1. first\n2) second\n- [x] done\n- [ ] todo")
        val list = blocks.single() as Block.ListBlock
        assertEquals(listOf("•", "•", "•", "1.", "2.", null, null), list.items.map { it.marker })
        assertEquals(1, list.items[2].depth)
        assertEquals(true, list.items[5].checked)
        assertEquals(false, list.items[6].checked)
        assertNull(list.items[0].checked)
    }

    @Test
    fun `list item continuation lines join the item`() {
        val list = Markdown.parse("- first line\n  continues here\n- second").single() as Block.ListBlock
        assertEquals(2, list.items.size)
        assertEquals("first line\ncontinues here", Markdown.plainText(list.items[0].runs))
    }

    @Test
    fun `parses fenced code including an unclosed fence while streaming`() {
        val closed = Markdown.parse("Intro\n```kotlin\nval x = 1\n```\nAfter")
        assertEquals(3, closed.size)
        val code = closed[1] as Block.Code
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)

        val streaming = Markdown.parse("```python\nprint('hi')")
        assertEquals("print('hi')", (streaming.single() as Block.Code).code)
    }

    @Test
    fun `code fence content is not parsed as markdown`() {
        val code = Markdown.parse("```\n# not a heading\n**not bold**\n```").single() as Block.Code
        assertEquals("# not a heading\n**not bold**", code.code)
    }

    @Test
    fun `parses tables and pads short rows`() {
        val table = Markdown.parse("| Name | Score |\n|---|:---:|\n| **Ann** | 10 |\n| Bob |").single() as Block.Table
        assertEquals(listOf("Name", "Score"), table.header.map { Markdown.plainText(it) })
        assertEquals(2, table.rows.size)
        assertTrue(table.rows[0][0].single().bold)
        assertEquals("", Markdown.plainText(table.rows[1][1]))
    }

    @Test
    fun `parses block quotes recursively`() {
        val quote = Markdown.parse("> **Note**\n> - item").single() as Block.Quote
        assertEquals(2, quote.blocks.size)
        assertTrue(quote.blocks[1] is Block.ListBlock)
    }

    @Test
    fun `plain text with stars and pipes stays a paragraph`() {
        val blocks = Markdown.parse("Rating: 5/5 | great value")
        assertTrue(blocks.single() is Block.Paragraph)
    }
}

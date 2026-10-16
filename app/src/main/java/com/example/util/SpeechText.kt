package com.example.util

/**
 * Turns Markdown replies into something pleasant to hear, and splits a reply that is still
 * streaming into complete sentences so speech can start before the reply is finished.
 */
object SpeechText {
    private val FENCED_CODE = Regex("""```[\s\S]*?```""")
    private val IMAGE = Regex("""!\[([^\]]*)]\([^)]*\)""")
    private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")
    private val URL = Regex("""https?://\S+""")
    private val TABLE_SEPARATOR = Regex("""(?m)^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")
    private val LINE_MARKERS = Regex("""(?m)^\s{0,3}(#{1,6}\s+|>\s?|[-*+•]\s+(\[[ xX]]\s+)?|\d{1,3}[.)]\s+)""")
    private val RULE = Regex("""(?m)^\s*([-*_]\s*){3,}$""")
    private val EMPHASIS = Regex("""[*_~`]+""")
    private val SYMBOLS = Regex("""[\x{1F000}-\x{1FAFF}\x{2600}-\x{27BF}\x{2B00}-\x{2BFF}\x{FE0F}\x{200D}]""")
    private val SPACES = Regex("""[ \t]+""")
    private val SENTENCE_END = Regex("""[.!?…](["')\]]*)(\s+|$)|\n""")

    /**
     * Speakable version of [markdown]. Code blocks are not read out. An unclosed code fence (the
     * reply is still streaming a code block) cuts the text there, since nothing after it is
     * speakable yet.
     */
    fun clean(markdown: String): String {
        var text = markdown
        val openFence = text.indexOf("```").let { first ->
            if (first == -1) -1 else {
                // Count fences; an odd count means the last one is still open.
                val count = Regex("```").findAll(text).count()
                if (count % 2 == 1) text.lastIndexOf("```") else -1
            }
        }
        if (openFence != -1) text = text.substring(0, openFence)
        text = FENCED_CODE.replace(text, "\n(Code is shown on screen.)\n")
        text = IMAGE.replace(text, "$1")
        text = LINK.replace(text, "$1")
        text = URL.replace(text, "a link")
        text = TABLE_SEPARATOR.replace(text, "")
        text = RULE.replace(text, "")
        text = LINE_MARKERS.replace(text, "")
        text = text.replace("|", ", ")
        text = EMPHASIS.replace(text, "")
        text = SYMBOLS.replace(text, "")
        text = text.lines().joinToString("\n") { SPACES.replace(it, " ").trim().trim(',').trim() }
        return text.replace(Regex("""\n{2,}"""), "\n").trim()
    }

    /**
     * Returns the next chunk to speak from [cleaned] starting at [offset], and the new offset.
     * Without [final], only complete sentences are returned; with it, everything left.
     */
    fun nextChunk(cleaned: String, offset: Int, final: Boolean): Pair<String, Int> {
        if (offset >= cleaned.length) return "" to offset
        if (final) return cleaned.substring(offset).trim() to cleaned.length
        var end = -1
        SENTENCE_END.findAll(cleaned, offset).forEach { end = it.range.last + 1 }
        if (end <= offset) return "" to offset
        return cleaned.substring(offset, end).trim() to end
    }
}

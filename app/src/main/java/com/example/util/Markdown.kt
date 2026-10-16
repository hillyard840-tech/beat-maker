package com.example.util

/**
 * Small Markdown parser for chat replies. It covers what Gemini actually produces: headings,
 * paragraphs, bullet / numbered / task lists, block quotes, fenced code, tables, rules, and inline
 * bold / italic / strikethrough / code / links. It is pure Kotlin so it can be unit-tested; the
 * Compose renderer lives in ui/MarkdownText.kt.
 *
 * It is forgiving by design: an unclosed code fence (common while a reply is still streaming) runs
 * to the end of the text, and unmatched emphasis markers are shown as plain characters.
 */
object Markdown {

    // ---------------- Model ----------------

    data class Run(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false,
        val code: Boolean = false,
        val url: String? = null
    )

    sealed class Block {
        data class Heading(val level: Int, val runs: List<Run>) : Block()
        data class Paragraph(val runs: List<Run>) : Block()
        data class ListBlock(val items: List<ListItem>) : Block()
        data class Quote(val blocks: List<Block>) : Block()
        data class Code(val language: String, val code: String) : Block()
        data class Table(val header: List<List<Run>>, val rows: List<List<List<Run>>>) : Block()
        object Rule : Block()
    }

    /** [marker] is "•", "1.", "2." … or null for task items, which use [checked]. */
    data class ListItem(val depth: Int, val marker: String?, val checked: Boolean?, val runs: List<Run>)

    // ---------------- Blocks ----------------

    private val FENCE = Regex("""^\s{0,3}(```|~~~)\s*([\w+#.\-]*)\s*$""")
    private val HEADING = Regex("""^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$""")
    private val RULE = Regex("""^\s{0,3}([-*_])(\s*\1){2,}\s*$""")
    private val LIST_ITEM = Regex("""^(\s*)([-*+•]|\d{1,3}[.)])\s+(.*)$""")
    private val TASK = Regex("""^\[([ xX])]\s+(.*)$""")
    private val QUOTE = Regex("""^\s{0,3}>\s?(.*)$""")
    private val TABLE_SEPARATOR = Regex("""^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")

    fun parse(text: String): List<Block> = parseLines(text.replace("\r\n", "\n").split("\n"))

    private fun parseLines(lines: List<String>): List<Block> {
        val blocks = mutableListOf<Block>()
        val paragraph = mutableListOf<String>()
        var i = 0

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += Block.Paragraph(parseInline(paragraph.joinToString("\n")))
                paragraph.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]

            if (line.isBlank()) {
                flushParagraph()
                i++
                continue
            }

            val fence = FENCE.matchEntire(line)
            if (fence != null) {
                flushParagraph()
                val marker = fence.groupValues[1]
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(marker)) {
                    code += lines[i]
                    i++
                }
                i++ // skip the closing fence (or run past the end if it's still streaming)
                blocks += Block.Code(fence.groupValues[2], code.joinToString("\n").trimEnd())
                continue
            }

            val heading = HEADING.matchEntire(line)
            if (heading != null) {
                flushParagraph()
                blocks += Block.Heading(heading.groupValues[1].length, parseInline(heading.groupValues[2]))
                i++
                continue
            }

            if (RULE.matches(line)) {
                flushParagraph()
                blocks += Block.Rule
                i++
                continue
            }

            if (line.contains('|') && i + 1 < lines.size && TABLE_SEPARATOR.matches(lines[i + 1])) {
                flushParagraph()
                val header = splitRow(line)
                i += 2
                val rows = mutableListOf<List<List<Run>>>()
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    val cells = splitRow(lines[i])
                    // Pad / trim to the header width so the grid stays rectangular.
                    rows += List(header.size) { idx -> parseInline(cells.getOrElse(idx) { "" }) }
                    i++
                }
                blocks += Block.Table(header.map { parseInline(it) }, rows)
                continue
            }

            if (QUOTE.matches(line)) {
                flushParagraph()
                val quoted = mutableListOf<String>()
                while (i < lines.size) {
                    val q = QUOTE.matchEntire(lines[i]) ?: break
                    quoted += q.groupValues[1]
                    i++
                }
                blocks += Block.Quote(parseLines(quoted))
                continue
            }

            if (LIST_ITEM.matches(line)) {
                flushParagraph()
                val items = mutableListOf<ListItem>()
                var pendingText: StringBuilder? = null
                var pendingDepth = 0
                var pendingMarker: String? = null
                var pendingChecked: Boolean? = null

                fun flushItem() {
                    pendingText?.let {
                        items += ListItem(pendingDepth, pendingMarker, pendingChecked, parseInline(it.toString()))
                    }
                    pendingText = null
                }

                while (i < lines.size) {
                    val current = lines[i]
                    val m = LIST_ITEM.matchEntire(current)
                    if (m != null) {
                        flushItem()
                        val indent = m.groupValues[1].replace("\t", "    ").length
                        val rawMarker = m.groupValues[2]
                        var body = m.groupValues[3]
                        val task = TASK.matchEntire(body)
                        pendingChecked = task?.let { it.groupValues[1].isNotBlank() }
                        if (task != null) body = task.groupValues[2]
                        pendingDepth = (indent / 2).coerceAtMost(4)
                        pendingMarker = when {
                            task != null -> null
                            rawMarker.first().isDigit() -> rawMarker.dropLast(1) + "."
                            else -> "•"
                        }
                        pendingText = StringBuilder(body)
                        i++
                    } else if (current.isNotBlank() && current.startsWith(" ") && pendingText != null) {
                        // Indented continuation of the previous item.
                        pendingText!!.append('\n').append(current.trim())
                        i++
                    } else if (current.isBlank() && i + 1 < lines.size && LIST_ITEM.matches(lines[i + 1])) {
                        i++ // blank line between items of the same list
                    } else {
                        break
                    }
                }
                flushItem()
                blocks += Block.ListBlock(items)
                continue
            }

            paragraph += line
            i++
        }
        flushParagraph()
        return blocks
    }

    private fun splitRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.drop(1)
        if (trimmed.endsWith("|") && !trimmed.endsWith("\\|")) trimmed = trimmed.dropLast(1)
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var k = 0
        while (k < trimmed.length) {
            val c = trimmed[k]
            if (c == '\\' && k + 1 < trimmed.length && trimmed[k + 1] == '|') {
                current.append('|')
                k += 2
                continue
            }
            if (c == '|') {
                cells += current.toString().trim()
                current.clear()
            } else {
                current.append(c)
            }
            k++
        }
        cells += current.toString().trim()
        return cells
    }

    // ---------------- Inline ----------------

    private data class Style(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false,
        val url: String? = null
    )

    private const val ESCAPABLE = "\\`*_{}[]()#+-.!|~>"

    fun parseInline(text: String): List<Run> {
        val out = mutableListOf<Run>()
        parseInto(text, Style(), out)
        return merge(out)
    }

    private fun parseInto(s: String, style: Style, out: MutableList<Run>) {
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotEmpty()) {
                out += Run(buffer.toString(), style.bold, style.italic, style.strike, false, style.url)
                buffer.clear()
            }
        }

        var i = 0
        while (i < s.length) {
            val c = s[i]

            // Backslash escape
            if (c == '\\' && i + 1 < s.length && s[i + 1] in ESCAPABLE) {
                buffer.append(s[i + 1])
                i += 2
                continue
            }

            // Inline code
            if (c == '`') {
                var ticks = 0
                while (i + ticks < s.length && s[i + ticks] == '`') ticks++
                val fence = "`".repeat(ticks)
                val end = s.indexOf(fence, i + ticks)
                if (end > i + ticks - 1 && end != -1) {
                    flush()
                    val code = s.substring(i + ticks, end).trim()
                    out += Run(code, style.bold, style.italic, style.strike, code = true, url = style.url)
                    i = end + ticks
                    continue
                }
                buffer.append(fence)
                i += ticks
                continue
            }

            // Bold (** or __) and strikethrough (~~)
            if ((s.startsWith("**", i) || s.startsWith("__", i) || s.startsWith("~~", i))) {
                val delim = s.substring(i, i + 2)
                val isUnderscore = delim == "__"
                val opensOk = i + 2 < s.length && !s[i + 2].isWhitespace() &&
                    (!isUnderscore || i == 0 || !s[i - 1].isLetterOrDigit())
                val end = findClosing(s, delim, i + 2)
                if (opensOk && end != -1 && (!isUnderscore || end + 2 >= s.length || !s[end + 2].isLetterOrDigit())) {
                    flush()
                    val inner = s.substring(i + 2, end)
                    val next = if (delim == "~~") style.copy(strike = true) else style.copy(bold = true)
                    parseInto(inner, next, out)
                    i = end + 2
                    continue
                }
            }

            // Italic (* or _)
            if ((c == '*' || c == '_') && !(i + 1 < s.length && s[i + 1] == c)) {
                val opensOk = i + 1 < s.length && !s[i + 1].isWhitespace() &&
                    (c == '*' || i == 0 || !s[i - 1].isLetterOrDigit())
                val end = if (opensOk) findClosing(s, c.toString(), i + 1) else -1
                val closesOk = end != -1 && !s[end - 1].isWhitespace() &&
                    (c == '*' || end + 1 >= s.length || !s[end + 1].isLetterOrDigit())
                if (closesOk) {
                    flush()
                    parseInto(s.substring(i + 1, end), style.copy(italic = true), out)
                    i = end + 1
                    continue
                }
            }

            // Link [text](url)
            if (c == '[') {
                val closeBracket = findMatchingBracket(s, i)
                if (closeBracket != -1 && closeBracket + 1 < s.length && s[closeBracket + 1] == '(') {
                    val closeParen = s.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val label = s.substring(i + 1, closeBracket)
                        val url = s.substring(closeBracket + 2, closeParen).trim().substringBefore(' ')
                        flush()
                        if (isSafeUrl(url)) {
                            parseInto(label, style.copy(url = url), out)
                        } else {
                            parseInto(label, style, out) // keep the text, drop an unsafe link
                        }
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Autolink <https://...> and bare URLs
            if (c == '<') {
                val close = s.indexOf('>', i + 1)
                if (close != -1) {
                    val candidate = s.substring(i + 1, close)
                    if (isSafeUrl(candidate) && candidate.none { it.isWhitespace() }) {
                        flush()
                        out += Run(candidate, style.bold, style.italic, style.strike, false, candidate)
                        i = close + 1
                        continue
                    }
                }
            }
            if (style.url == null && (s.startsWith("https://", i) || s.startsWith("http://", i)) &&
                (i == 0 || !s[i - 1].isLetterOrDigit())
            ) {
                var end = i
                while (end < s.length && !s[end].isWhitespace() && s[end] != '<' && s[end] != '>') end++
                var url = s.substring(i, end)
                url = url.trimEnd('.', ',', ';', ':', '!', '?', '\'', '"')
                if (url.endsWith(")") && url.count { it == '(' } < url.count { it == ')' }) url = url.dropLast(1)
                flush()
                out += Run(url, style.bold, style.italic, style.strike, false, url)
                i += url.length
                continue
            }

            buffer.append(c)
            i++
        }
        flush()
    }

    /** Finds a closing delimiter that isn't escaped and isn't preceded by whitespace. */
    private fun findClosing(s: String, delim: String, from: Int): Int {
        var idx = s.indexOf(delim, from)
        while (idx != -1) {
            val escaped = idx > 0 && s[idx - 1] == '\\'
            val afterSpace = idx > 0 && s[idx - 1].isWhitespace()
            // For single-char delimiters, skip occurrences that are part of a double marker.
            val partOfDouble = delim.length == 1 && idx + 1 < s.length && s[idx + 1] == delim[0]
            if (!escaped && !afterSpace && !partOfDouble && idx > from) {
                // In "***x***" the bold closer is the last two stars, leaving "*x*" for italic.
                var end = idx
                while (delim.length == 2 && end + 2 < s.length && s[end + 2] == delim[0]) end++
                return end
            }
            idx = s.indexOf(delim, if (partOfDouble) idx + 2 else idx + 1)
        }
        return -1
    }

    private fun findMatchingBracket(s: String, open: Int): Int {
        var depth = 0
        for (k in open until s.length) {
            when (s[k]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return k
                }
            }
        }
        return -1
    }

    /** Only web and mail links become tappable; anything else (javascript:, intent:, file:) stays text. */
    fun isSafeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") || lower.startsWith("http://") || lower.startsWith("mailto:")
    }

    private fun merge(runs: List<Run>): List<Run> {
        val merged = mutableListOf<Run>()
        for (run in runs) {
            if (run.text.isEmpty()) continue
            val last = merged.lastOrNull()
            if (last != null && last.copy(text = "") == run.copy(text = "")) {
                merged[merged.size - 1] = last.copy(text = last.text + run.text)
            } else {
                merged += run
            }
        }
        return merged
    }

    /** Plain text of a run list (used for copy, speech, and tests). */
    fun plainText(runs: List<Run>): String = runs.joinToString("") { it.text }
}

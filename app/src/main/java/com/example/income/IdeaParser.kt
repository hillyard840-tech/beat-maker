package com.example.income

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One AI suggestion: a way to earn, or a program worth building. */
data class Idea(
    val title: String,
    val summary: String,
    val steps: List<String> = emptyList(),
    val startupCost: String = "",
    val timeToFirstIncome: String = "",
    val earnings: String = "",
    val whoPays: String = "",
    val stack: String = "",
    val risks: String = "",
    /** Links the model cited; only links that were in the material Kora gave it are kept. */
    val sources: List<String> = emptyList()
) {
    /** Markdown version for Canvas, saving, or asking Kora to plan it. */
    fun toMarkdown(): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine(summary)
        appendLine()
        if (whoPays.isNotEmpty()) appendLine("**Who pays:** $whoPays  ")
        if (startupCost.isNotEmpty()) appendLine("**Startup cost:** $startupCost  ")
        if (timeToFirstIncome.isNotEmpty()) appendLine("**Time to first income:** $timeToFirstIncome  ")
        if (earnings.isNotEmpty()) appendLine("**Realistic earnings:** $earnings  ")
        if (stack.isNotEmpty()) appendLine("**Suggested stack:** $stack  ")
        if (steps.isNotEmpty()) {
            appendLine()
            appendLine("## First steps")
            steps.forEachIndexed { i, s -> appendLine("${i + 1}. $s") }
        }
        if (risks.isNotEmpty()) {
            appendLine()
            appendLine("**Watch out for:** $risks")
        }
        if (sources.isNotEmpty()) {
            appendLine()
            appendLine("## Sources")
            sources.forEach { appendLine("- $it") }
        }
    }
}

object IdeaParser {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    /**
     * Reads the JSON array the model was asked for, tolerating code fences and chatter around
     * it. [allowedUrls] keeps only links that really came from the search material.
     */
    fun parse(text: String, allowedUrls: Set<String> = emptySet()): List<Idea> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val root: JsonElement = try {
            json.parseToJsonElement(text.substring(start, end + 1))
        } catch (_: Exception) {
            return emptyList()
        }
        return (root as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            fun s(vararg keys: String) = keys.firstNotNullOfOrNull { k -> (o[k] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() } }.orEmpty()
            fun list(vararg keys: String) = keys.firstNotNullOfOrNull { k -> o[k] as? JsonArray }
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { v -> v.isNotEmpty() } }
                .orEmpty()
            val title = s("title", "name")
            val summary = s("summary", "description")
            if (title.isEmpty() || summary.isEmpty()) return@mapNotNull null
            Idea(
                title = title.take(100),
                summary = summary.take(600),
                steps = list("steps", "firstSteps", "first_steps").take(6),
                startupCost = s("startupCost", "startup_cost", "cost"),
                timeToFirstIncome = s("timeToFirstIncome", "time_to_first_income"),
                earnings = s("earnings", "realisticEarnings"),
                whoPays = s("whoPays", "who_pays", "customers"),
                stack = s("stack", "techStack"),
                risks = s("risks", "watchOutFor"),
                sources = list("sources", "evidence").filter { url -> allowedUrls.isEmpty() || url in allowedUrls }.take(4)
            )
        }
    }
}

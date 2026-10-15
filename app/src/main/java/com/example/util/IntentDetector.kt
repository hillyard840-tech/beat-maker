package com.example.util

/**
 * Intelligent Intent Classifier for Kora.Ai
 * 
 * Automatically routes and orchestrates capabilities:
 * - Live Internet: current events, weather, stock prices, scores, news, URLs, "who won", "latest"
 * - Deep Research: comprehensive inquiries, market analyses, literature reviews, comparisons, "deep research", "in-depth investigation"
 * - Image Generation: creative visual prompts ("draw", "generate an image", "create a picture", "visualize")
 * - Canvas / Code Workspace: long code refactoring, full script authoring, long document drafts
 * - Data Analysis: tabular queries, statistics on datasets
 * - Persistent Memory: "remember that", "keep in mind", "don't forget"
 */
object IntentDetector {

    enum class RoutedIntent {
        DEEP_RESEARCH,
        IMAGE_GEN,
        CANVAS,
        DATA_ANALYSIS,
        PERSISTENT_MEMORY,
        SEEKCLAW,
        MOLTBOOK,
        STANDARD_WITH_LIVE_WEB,
        STANDARD
    }

    data class DetectionResult(
        val intent: RoutedIntent,
        val extractedParam: String? = null,
        val autoTriggerLiveWeb: Boolean = false,
        val reasoning: String = ""
    )

    private val DEEP_RESEARCH_PATTERNS = listOf(
        Regex("""\b(?:deep\s+research|in-depth\s+research|comprehensive\s+report|exhaustive\s+analysis|thorough\s+investigation|literature\s+review|state\s+of\s+the\s+art\s+survey)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:conduct|do|perform|produce|generate)\s+(?:a\s+)?(?:deep|detailed|exhaustive|rigorous)\s+(?:research|study|report|breakdown|investigation)\s+(?:on|into|about)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:compare\s+in\s+detail|multi-source\s+analysis|uncover\s+all\s+recent\s+developments\s+in)\b""", RegexOption.IGNORE_CASE)
    )

    private val LIVE_WEB_PATTERNS = listOf(
        // Explicit time / temporal anchors
        Regex("""\b(?:today|yesterday|tomorrow|this\s+week|this\s+month|latest|current|recent|recently|breaking|live|now|right\s+now|at\s+present|real-time|updated)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:2025|2026|2027)\b"""),
        // Specific live domains
        Regex("""\b(?:weather|forecast|temperature|stock\s+price|market\s+cap|crypto\s+price|bitcoin|score|match\s+result|who\s+won|standings|election|schedule|flight\s+status|news|headline)\b""", RegexOption.IGNORE_CASE),
        // Search request verbs
        Regex("""\b(?:search\s+(?:the\s+web|online|internet|google|bing)|look\s+up\s+online|browse\s+the\s+web|check\s+the\s+news|find\s+latest\s+info)\b""", RegexOption.IGNORE_CASE),
        // URLs
        Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
    )

    private val IMAGE_GEN_PATTERNS = listOf(
        Regex("""^(?:generate\s+image(?:\s+of)?|create\s+image(?:\s+of)?|draw(?:\s+a|\s+an)?|make\s+a\s+picture\s+of|illustrate(?:\s+a|\s+an)?|paint(?:\s+a|\s+an)?|render(?:\s+a|\s+an)?)\s+(.+)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:generate\s+an?\s+image|create\s+an?\s+illustration|produce\s+a\s+drawing|make\s+a\s+digital\s+art|render\s+a\s+3d\s+scene)\s+(?:of|showing|depicting)\s+(.+)""", RegexOption.IGNORE_CASE)
    )

    private val MEMORY_PATTERNS = listOf(
        Regex("""^(?:remember\s+(?:that\s+)?|keep\s+in\s+mind\s+(?:that\s+)?|never\s+forget\s+(?:that\s+)?|save\s+to\s+memory(?:\s+that)?)\s*(.+)""", RegexOption.IGNORE_CASE)
    )

    private val CANVAS_PATTERNS = listOf(
        Regex("""^(?:open\s+(?:a\s+)?canvas|write\s+(?:a\s+)?full\s+(?:script|program|essay|contract|article)\s+in\s+canvas)\b""", RegexOption.IGNORE_CASE)
    )

    private val SEEKCLAW_PATTERNS = listOf(
        Regex("""\b(?:seekclaw|claw\s+credits|agent\s+bount(?:y|ies)|agent\s+marketplace|seek\s+claw)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:find\s+(?:agent\s+)?jobs|apply\s+(?:to|for)\s+job|open\s+marketplace)\b""", RegexOption.IGNORE_CASE)
    )

    private val MOLTBOOK_PATTERNS = listOf(
        Regex("""\b(?:moltbook|submolt|moltys?|lobster\s+social|agent\s+social\s+network|post\s+on\s+moltbook|post\s+to\s+moltbook|moltbook\s+feed)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:check\s+moltbook|open\s+moltbook|browse\s+submolts?)\b""", RegexOption.IGNORE_CASE)
    )

    fun detect(prompt: String, hasAttachedDataset: Boolean = false): DetectionResult {
        val trimmed = prompt.trim()

        // 0. Check for Moltbook intent
        if (MOLTBOOK_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return DetectionResult(
                intent = RoutedIntent.MOLTBOOK,
                reasoning = "Moltbook AI agent social network requested"
            )
        }

        // 0.1 Check for SeekClaw intent
        if (SEEKCLAW_PATTERNS.any { it.containsMatchIn(trimmed) }) {
            return DetectionResult(
                intent = RoutedIntent.SEEKCLAW,
                reasoning = "SeekClaw agent marketplace requested"
            )
        }

        // 1. Check for Memory instructions
        for (pattern in MEMORY_PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val fact = match.groupValues[1].trim()
                return DetectionResult(
                    intent = RoutedIntent.PERSISTENT_MEMORY,
                    extractedParam = fact,
                    reasoning = "Detected persistent memory directive"
                )
            }
        }

        // 2. Check for Image Generation intent
        for (pattern in IMAGE_GEN_PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                val imagePrompt = match.groupValues.lastOrNull { it.isNotBlank() }?.trim() ?: trimmed
                return DetectionResult(
                    intent = RoutedIntent.IMAGE_GEN,
                    extractedParam = imagePrompt,
                    reasoning = "Detected visual generation prompt"
                )
            }
        }

        // 3. Check for Deep Research intent (complex multi-step investigation)
        for (pattern in DEEP_RESEARCH_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                // Extract clean query topic
                var topic = trimmed
                    .replace(Regex("""^(?:please\s+)?(?:do|conduct|run|perform|produce|generate)?\s*(?:a\s+)?(?:deep\s+research|in-depth\s+research|comprehensive\s+report|exhaustive\s+analysis)\s*(?:on|about|into)?\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (topic.isBlank()) topic = trimmed
                return DetectionResult(
                    intent = RoutedIntent.DEEP_RESEARCH,
                    extractedParam = topic,
                    reasoning = "Detected requirement for comprehensive multi-source investigation"
                )
            }
        }

        // 4. Check for Canvas intent
        for (pattern in CANVAS_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return DetectionResult(
                    intent = RoutedIntent.CANVAS,
                    reasoning = "Explicit canvas requested"
                )
            }
        }

        // 5. Check if tabular dataset attached and user is asking for stats/analysis
        if (hasAttachedDataset) {
            val analysisKeywords = listOf("analyze", "chart", "graph", "trend", "mean", "average", "median", "summary", "stats", "distribution", "correlation")
            if (analysisKeywords.any { trimmed.contains(it, ignoreCase = true) }) {
                return DetectionResult(
                    intent = RoutedIntent.DATA_ANALYSIS,
                    reasoning = "Tabular data present and data analysis requested"
                )
            }
        }

        // 6. Check for Live Internet requirement
        val needsLiveWeb = LIVE_WEB_PATTERNS.any { it.containsMatchIn(trimmed) }
        if (needsLiveWeb) {
            return DetectionResult(
                intent = RoutedIntent.STANDARD_WITH_LIVE_WEB,
                autoTriggerLiveWeb = true,
                reasoning = "Detected temporal queries, live data, or URLs requiring real-time web browsing"
            )
        }

        return DetectionResult(
            intent = RoutedIntent.STANDARD,
            autoTriggerLiveWeb = false,
            reasoning = "Standard conversation or reasoning task"
        )
    }
}

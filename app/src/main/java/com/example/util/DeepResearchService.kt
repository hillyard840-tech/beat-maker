package com.example.util

import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class DeepResearchState(
    val phase: String = "Initializing",
    val step: Int = 1,
    val totalSteps: Int = 4,
    val logs: List<String> = emptyList(),
    val sources: List<LiveSearchResult> = emptyList(),
    val report: String? = null,
    val isFinished: Boolean = false,
    val error: String? = null
)

object DeepResearchService {

    suspend fun executeDeepResearch(
        query: String,
        siteRestrictions: String = "",
        generate: suspend (GenerateContentRequest) -> GenerateContentResponse,
        onProgress: (DeepResearchState) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val allSources = mutableListOf<LiveSearchResult>()
        val retrievedPages = mutableListOf<LiveWebpage>()

        fun update(phase: String, step: Int, newLog: String? = null, isDone: Boolean = false, report: String? = null) {
            if (newLog != null) logs.add(newLog)
            onProgress(
                DeepResearchState(
                    phase = phase,
                    step = step,
                    totalSteps = 4,
                    logs = logs.toList(),
                    sources = allSources.toList(),
                    report = report,
                    isFinished = isDone
                )
            )
        }

        try {
            // STEP 1: Formulate Deep Research Plan
            update(
                phase = "1/4: Formulating Research Plan & Sub-queries",
                step = 1,
                newLog = "📋 Formulating multi-angle research strategy for: \"$query\""
            )

            val restrictedClause = if (siteRestrictions.isNotBlank()) " restricting/prioritizing sites: $siteRestrictions" else ""
            logs.add("🎯 Objectives defined$restrictedClause")

            // Create sub-queries
            val subQueries = mutableListOf(query)
            if (siteRestrictions.isNotBlank()) {
                subQueries.add("$query site:$siteRestrictions")
            }
            subQueries.add("$query comparison requirements details")
            subQueries.add("$query official sources guide")

            // STEP 2: Multi-Source Web Exploration
            update(
                phase = "2/4: Crawling Multi-Source Index",
                step = 2,
                newLog = "🔍 Executing multi-source crawler across ${subQueries.size} angles..."
            )

            for (sq in subQueries.take(3)) {
                val results = LiveInternetService.searchLiveInternet(sq, maxResults = 4)
                for (r in results) {
                    if (allSources.none { it.url == r.url }) {
                        allSources.add(r)
                        logs.add("🌐 Discovered: ${r.title.take(45)}... (${r.url.substringAfter("://").take(30)})")
                    }
                }
                delay(300) // be polite to the search engine between queries
            }

            if (allSources.isEmpty()) {
                logs.add("⚠️ The web search returned no results; the report will rely on the model's own knowledge and say so.")
            }

            // STEP 3: Deep Document Analysis & Synthesis
            update(
                phase = "3/4: Reading & Cross-referencing Web Sources",
                step = 3,
                newLog = "📖 Deep-reading top ${minOf(allSources.size, 5)} retrieved web sources..."
            )

            val topSources = allSources.take(5)
            for (source in topSources) {
                logs.add("⚡ Reading page: ${source.title.take(40)}...")
                val page = LiveInternetService.fetchLiveWebpage(source.url, maxChars = 3500)
                retrievedPages.add(page)
            }

            // STEP 4: Synthesizing Structured Deep Research Report
            update(
                phase = "4/4: Synthesizing Comprehensive Deep Research Report",
                step = 4,
                newLog = "✍️ Formulating rigorous cross-source comparative report with citations..."
            )

            val researchCorpus = buildString {
                appendLine("=== DEEP RESEARCH INVESTIGATION DOSSIER ===")
                appendLine("PRIMARY INQUIRY: $query")
                if (siteRestrictions.isNotBlank()) {
                    appendLine("TARGET DOMAIN RESTRICTIONS: $siteRestrictions")
                }
                appendLine()
                appendLine("=== PRIMARY RETRIEVED SOURCES & EVIDENCE (${retrievedPages.size} sources) ===")
                retrievedPages.forEachIndexed { i, page ->
                    appendLine("--- SOURCE [${i + 1}]: ${page.title} ---")
                    appendLine("URL: ${page.url}")
                    if (page.metaDescription.isNotBlank()) appendLine("Summary: ${page.metaDescription}")
                    appendLine("Extracted Text Content:\n${page.content}")
                    appendLine()
                }
            }

            val prompt = """
                You are Kora.Ai's Deep Research Engine. 
                Using the comprehensive research evidence dossier provided below, produce an exhaustive, authoritative, and structured Deep Research Report answering the user's primary inquiry: "$query".
                
                FORMAT REQUIREMENTS:
                # 🔬 Deep Research Report: [Concise Title]
                
                ## 📌 Executive Summary
                - 2-3 paragraph distilled synthesis of the most critical findings.
                
                ## 📊 Comparative Analysis & Findings
                - Detailed comparative breakdown (use Markdown tables where comparing options, requirements, prices, lenders, specs, or criteria).
                
                ## 🔍 In-Depth Evidence Breakdown
                - Point-by-point deep dive addressing all dimensions of the user's inquiry.
                - Address edge cases, eligibility criteria, pitfalls, and verification nuances.
                
                ## 💡 Actionable Strategic Recommendations
                - Concrete, step-by-step guidance for the user based on the evidence.
                
                ## 📚 Sources
                - Bulleted list of the sources in the dossier below with markdown links [Title](URL) and the key takeaway from each.
                - Only cite URLs that appear in the dossier. Never invent sources, statistics, or quotes. If the dossier has no usable sources, say so clearly at the top of the report.
                
                $researchCorpus
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = prompt))
                    )
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "You are an elite, objective research analyst. Produce thoroughly cited, structured, and deep multi-source research reports."
                        )
                    )
                )
            )

            val response = generate(request)
            val reportText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.thought != true && !it.text.isNullOrBlank() }?.text
                ?: "Deep research synthesis could not be completed."

            update(
                phase = "Deep Research Completed",
                step = 4,
                newLog = "✅ Report generated from ${retrievedPages.count { !it.title.startsWith("Error loading") && it.title != "Blocked URL" }} pages read (${allSources.size} search results).",
                isDone = true,
                report = reportText
            )

            reportText
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "Deep research encountered an error: ${e.message}"
            onProgress(
                DeepResearchState(
                    phase = "Error",
                    step = 4,
                    totalSteps = 4,
                    logs = logs + listOf("❌ $errorMsg"),
                    sources = allSources,
                    report = null,
                    isFinished = true,
                    error = errorMsg
                )
            )
            errorMsg
        }
    }
}

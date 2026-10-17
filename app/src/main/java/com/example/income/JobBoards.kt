package com.example.income

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Live listings from free public job boards (no keys, no accounts). Results are cached for a
 * while because the boards ask apps not to poll them constantly.
 */
object JobBoards {
    data class Result(val jobs: List<JobListing>, val failedSources: List<String>, val fetchedAt: Long)
    data class Demand(val signals: List<DemandSignal>, val failed: Boolean)

    private const val MAX_BODY_BYTES = 3L * 1024 * 1024
    private const val CACHE_MILLIS = 30 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = mutableMapOf<String, Result>()

    suspend fun search(query: String, remoteOnly: Boolean = true, now: Long = System.currentTimeMillis()): Result {
        val key = query.trim().lowercase() + "|" + remoteOnly
        synchronized(cache) { cache[key]?.takeIf { now - it.fetchedAt < CACHE_MILLIS }?.let { return it } }

        val result = coroutineScope {
            val remotive = async { fetch(JobParsers.REMOTIVE) { remotiveUrl(query) }?.let(JobParsers::parseRemotive) }
            val jobicy = async { fetch(JobParsers.JOBICY) { jobicyUrl(query) }?.let(JobParsers::parseJobicy) }
            val arbeitnow = async {
                fetch(JobParsers.ARBEITNOW) { "https://www.arbeitnow.com/api/job-board-api" }
                    ?.let { JobParsers.parseArbeitnow(it, query, remoteOnly) }
            }
            val bySource = listOf(JobParsers.REMOTIVE to remotive.await(), JobParsers.JOBICY to jobicy.await(), JobParsers.ARBEITNOW to arbeitnow.await())
            Result(
                jobs = JobParsers.merge(bySource.mapNotNull { it.second }),
                failedSources = bySource.filter { it.second == null }.map { it.first },
                fetchedAt = now
            )
        }
        if (result.failedSources.size < 3) synchronized(cache) { cache[key] = result }
        return result
    }

    /** Recent Hacker News posts where people ask for a tool or describe a problem around [topic]. */
    suspend fun demandSignals(topic: String): Demand {
        val queries = listOf("is there a tool $topic", "$topic I would pay for", "Ask HN $topic")
        var failures = 0
        val signals = mutableListOf<DemandSignal>()
        for (q in queries) {
            val root = fetch("Hacker News") {
                "https://hn.algolia.com/api/v1/search".toHttpUrl().newBuilder()
                    .addQueryParameter("query", q.trim())
                    .addQueryParameter("tags", "story")
                    .addQueryParameter("hitsPerPage", "8")
                    .build().toString()
            }
            if (root == null) failures++ else signals += JobParsers.parseHackerNews(root)
        }
        return Demand(
            signals = signals.distinctBy { it.url }.sortedByDescending { it.points + it.comments }.take(12),
            failed = failures == queries.size
        )
    }

    fun remotiveUrl(query: String): String =
        "https://remotive.com/api/remote-jobs".toHttpUrl().newBuilder()
            .apply { if (query.isNotBlank()) addQueryParameter("search", query.trim()) }
            .addQueryParameter("limit", "30")
            .build().toString()

    fun jobicyUrl(query: String): String =
        "https://jobicy.com/api/v2/remote-jobs".toHttpUrl().newBuilder()
            .addQueryParameter("count", "30")
            .apply { JobParsers.jobicyTag(query)?.let { addQueryParameter("tag", it) } }
            .build().toString()

    /** GETs JSON, or null if the source is down, blocked or returns something unexpected. */
    private suspend fun fetch(source: String, url: () -> String): JsonElement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url())
                .header("User-Agent", "Kora.Ai/1.0 (Android; personal job search)")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val length = body.contentLength()
                if (length > MAX_BODY_BYTES) return@withContext null
                val text = body.source().let { src ->
                    src.request(MAX_BODY_BYTES)
                    src.buffer.readUtf8(minOf(src.buffer.size, MAX_BODY_BYTES))
                }
                json.parseToJsonElement(text)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            android.util.Log.w("JobBoards", "$source unavailable")
            null
        }
    }

    /** Text block for the model: real listings with their links. */
    fun describeForModel(result: Result, max: Int = 12): String = buildString {
        if (result.jobs.isEmpty()) {
            append("No matching listings found right now.")
        } else {
            appendLine("Live listings from public job boards (${result.jobs.size} found, showing ${minOf(max, result.jobs.size)}):")
            result.jobs.take(max).forEachIndexed { i, j ->
                appendLine("[${i + 1}] ${j.title} — ${j.company.ifEmpty { "company not listed" }}")
                appendLine("    ${listOf(j.location, j.jobType, j.salary).filter { it.isNotEmpty() }.joinToString(" · ")}")
                appendLine("    Source: ${j.source} · ${j.url}")
            }
        }
        if (result.failedSources.isNotEmpty()) {
            append("\n(Unavailable right now: ${result.failedSources.joinToString()}.)")
        }
    }

    fun describeDemand(demand: Demand): String = buildString {
        if (demand.failed) return "Hacker News couldn't be reached right now."
        if (demand.signals.isEmpty()) return "No recent posts found asking for this."
        appendLine("Real Hacker News posts about this problem (points / comments):")
        demand.signals.forEach { s ->
            appendLine("• ${s.title} (${s.points} / ${s.comments}) ${s.url}")
            if (s.excerpt.isNotEmpty()) appendLine("  ${s.excerpt}")
        }
    }
}

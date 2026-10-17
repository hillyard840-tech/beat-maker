package com.example.income

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One real listing from a public job board. Every field comes from the board itself. */
data class JobListing(
    val source: String,
    val title: String,
    val company: String,
    val url: String,
    val location: String = "",
    val jobType: String = "",
    val salary: String = "",
    val tags: List<String> = emptyList(),
    val postedAt: Long? = null,
    val excerpt: String = ""
) {
    /** Same job posted on two boards counts once. */
    val dedupeKey: String get() = (title.lowercase().filter { it.isLetterOrDigit() } + "|" + company.lowercase().filter { it.isLetterOrDigit() })
}

/** A real post where people describe a problem or ask for a tool — evidence that a build is wanted. */
data class DemandSignal(
    val title: String,
    val url: String,
    val points: Int,
    val comments: Int,
    val createdAt: Long?,
    val excerpt: String
)

/**
 * Lenient parsers for the boards' documented JSON. Unknown or missing fields are skipped rather
 * than failing the whole list, so a small format change never shows the user an error page.
 */
object JobParsers {
    const val REMOTIVE = "Remotive"
    const val JOBICY = "Jobicy"
    const val ARBEITNOW = "Arbeitnow"

    private fun JsonObject.str(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun JsonObject.int(key: String): Int =
        (this[key] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toInt() ?: 0

    private fun JsonObject.strings(key: String): List<String> = when (val v = this[key]) {
        is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { s -> s.isNotEmpty() } }
        is JsonPrimitive -> listOfNotNull(v.contentOrNull?.trim()?.takeIf { it.isNotEmpty() })
        else -> emptyList()
    }

    private fun array(root: JsonElement, key: String): List<JsonObject> =
        ((root as? JsonObject)?.get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

    /** https://remotive.com/api/remote-jobs */
    fun parseRemotive(root: JsonElement): List<JobListing> = array(root, "jobs").mapNotNull { j ->
        val title = j.str("title")
        val url = j.str("url")
        if (title.isEmpty() || !isHttp(url)) return@mapNotNull null
        JobListing(
            source = REMOTIVE,
            title = title,
            company = j.str("company_name"),
            url = url,
            location = j.str("candidate_required_location"),
            jobType = j.str("job_type").replace('_', ' '),
            salary = j.str("salary"),
            tags = j.strings("tags").take(6),
            postedAt = parseDate(j.str("publication_date")),
            excerpt = excerpt(j.str("description"))
        )
    }

    /** https://jobicy.com/api/v2/remote-jobs */
    fun parseJobicy(root: JsonElement): List<JobListing> = array(root, "jobs").mapNotNull { j ->
        val title = cleanText(j.str("jobTitle"))
        val url = j.str("url")
        if (title.isEmpty() || !isHttp(url)) return@mapNotNull null
        val min = j.str("annualSalaryMin")
        val max = j.str("annualSalaryMax")
        val currency = j.str("salaryCurrency")
        val salary = when {
            min.isNotEmpty() && max.isNotEmpty() -> "$min–$max $currency".trim()
            min.isNotEmpty() -> "from $min $currency".trim()
            else -> ""
        }
        JobListing(
            source = JOBICY,
            title = title,
            company = cleanText(j.str("companyName")),
            url = url,
            location = j.str("jobGeo"),
            jobType = j.strings("jobType").joinToString(", "),
            salary = salary,
            tags = (j.strings("jobIndustry") + j.strings("jobLevel")).map { cleanText(it) }.take(6),
            postedAt = parseDate(j.str("pubDate")),
            excerpt = excerpt(j.str("jobExcerpt").ifEmpty { j.str("jobDescription") })
        )
    }

    /** https://www.arbeitnow.com/api/job-board-api — no server-side search, so filter here. */
    fun parseArbeitnow(root: JsonElement, query: String, remoteOnly: Boolean): List<JobListing> {
        val words = keywords(query)
        return array(root, "data").mapNotNull { j ->
            val title = j.str("title")
            val url = j.str("url")
            if (title.isEmpty() || !isHttp(url)) return@mapNotNull null
            val remote = (j["remote"] as? JsonPrimitive)?.contentOrNull == "true"
            if (remoteOnly && !remote) return@mapNotNull null
            val tags = j.strings("tags")
            val haystack = (title + " " + tags.joinToString(" ") + " " + j.str("description").take(2000)).lowercase()
            if (words.isNotEmpty() && words.none { it in haystack }) return@mapNotNull null
            JobListing(
                source = ARBEITNOW,
                title = title,
                company = j.str("company_name"),
                url = url,
                location = listOf(j.str("location"), if (remote) "Remote" else "").filter { it.isNotEmpty() }.joinToString(" · "),
                jobType = j.strings("job_types").joinToString(", "),
                tags = tags.take(6),
                postedAt = j.str("created_at").toLongOrNull()?.let { it * 1000 },
                excerpt = excerpt(j.str("description"))
            )
        }
    }

    /** https://hn.algolia.com/api/v1/search */
    fun parseHackerNews(root: JsonElement): List<DemandSignal> = array(root, "hits").mapNotNull { h ->
        val title = h.str("title").ifEmpty { h.str("story_title") }
        val id = h.str("objectID")
        if (title.isEmpty() || id.isEmpty()) return@mapNotNull null
        DemandSignal(
            title = title,
            url = "https://news.ycombinator.com/item?id=$id",
            points = h.int("points"),
            comments = h.int("num_comments"),
            createdAt = parseDate(h.str("created_at")),
            excerpt = excerpt(h.str("story_text").ifEmpty { h.str("comment_text") }, 280)
        )
    }

    /** Merges boards: one copy of each job, newest first. */
    fun merge(lists: List<List<JobListing>>, limit: Int = 40): List<JobListing> =
        lists.flatten()
            .distinctBy { it.dedupeKey }
            .sortedWith(compareByDescending<JobListing> { it.postedAt ?: 0L })
            .take(limit)

    /** Lower-case search words, dropping filler like "remote" or "jobs". */
    fun keywords(query: String): List<String> {
        val stop = setOf("remote", "job", "jobs", "work", "for", "and", "the", "a", "an", "in", "of", "part", "time", "gig", "gigs", "side")
        return query.lowercase().split(Regex("[^a-z0-9+#.]+")).filter { it.length >= 2 && it !in stop }.distinct()
    }

    /** Jobicy's tag filter takes one slug, e.g. "python" or "data-science". */
    fun jobicyTag(query: String): String? = keywords(query).firstOrNull()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')?.ifEmpty { null }

    fun isHttp(url: String): Boolean = url.startsWith("https://") || url.startsWith("http://")

    private val ISO_FORMATS = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")

    /** ISO dates as the boards send them, in UTC; fractions and zones are tolerated. */
    fun parseDate(raw: String): Long? {
        if (raw.isEmpty()) return null
        val core = raw.replace(Regex("""(\.\d+)?(Z|[+-]\d{2}:?\d{2})?$"""), "")
        for (pattern in ISO_FORMATS) {
            try {
                val f = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
                val pos = java.text.ParsePosition(0)
                val d = f.parse(core, pos)
                if (d != null && pos.index == core.length) return d.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /** Plain text from HTML-ish board descriptions, shortened for a card. */
    fun excerpt(html: String, max: Int = 220): String {
        val text = cleanText(html.replace(Regex("<[^>]*>"), " "))
        return if (text.length <= max) text else text.take(max).substringBeforeLast(' ') + "…"
    }

    fun cleanText(s: String): String = s
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#039;", "'").replace("&#8217;", "’").replace("&#8211;", "–")
        .replace(Regex("\\s+"), " ")
        .trim()
}

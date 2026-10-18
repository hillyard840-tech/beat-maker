package com.example.markets

import com.example.data.remote.arrayOf
import com.example.data.remote.bool
import com.example.data.remote.num
import com.example.data.remote.str
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One Polymarket market (a question with outcomes). Prices are probabilities, 0..1. */
data class Market(
    val id: String,
    val question: String,
    val slug: String,
    val eventSlug: String?,
    val outcomes: List<String>,
    val prices: List<Double>,
    val tokenIds: List<String>,
    val volume: Double?,
    val volume24h: Double?,
    val liquidity: Double?,
    val oneDayChange: Double?,
    val endDate: Long?,
    val closed: Boolean,
    val description: String = ""
) {
    val url: String get() = Polymarket.marketUrl(eventSlug, slug)

    fun priceOf(outcome: String): Double? = outcomes.indexOfFirst { it.equals(outcome, ignoreCase = true) }.takeIf { it >= 0 }?.let { prices.getOrNull(it) }

    /** For a resolved market: the outcome that settled at (nearly) 1.0, if any. */
    val winner: String? get() = if (!closed) null else prices.indexOfFirst { it >= 0.99 }.takeIf { it >= 0 }?.let { outcomes.getOrNull(it) }
}

/** One of the user's open positions, read by public wallet address. */
data class Position(
    val title: String,
    val outcome: String,
    val size: Double,
    val avgPrice: Double?,
    val curPrice: Double?,
    val currentValue: Double?,
    val cashPnl: Double?,
    val percentPnl: Double?,
    val slug: String?,
    val eventSlug: String?,
    val endDate: Long?,
    val redeemable: Boolean
) {
    val url: String? get() = slug?.let { Polymarket.marketUrl(eventSlug, it) }
}

data class PricePoint(val t: Long, val p: Double)

object Polymarket {
    const val GAMMA = "https://gamma-api.polymarket.com"
    const val CLOB = "https://clob.polymarket.com"
    const val DATA = "https://data-api.polymarket.com"
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    fun marketUrl(eventSlug: String?, slug: String): String =
        if (!eventSlug.isNullOrBlank() && eventSlug != slug) "https://polymarket.com/event/$eventSlug/$slug" else "https://polymarket.com/event/$slug"

    /** Only real Polymarket pages may be opened from a trade ticket. */
    fun isPolymarketUrl(url: String): Boolean =
        Regex("""^https://(www\.)?polymarket\.com/(event|market)/[A-Za-z0-9._~%-]+(/[A-Za-z0-9._~%-]+)?/?$""").matches(url)

    /** A Polygon wallet address: 0x and 40 hex characters. */
    fun isWalletAddress(text: String): Boolean = Regex("^0x[0-9a-fA-F]{40}$").matches(text.trim())

    /** Gamma sends outcomes, prices and token IDs as JSON arrays inside strings. */
    private fun stringList(o: JsonObject, key: String): List<String> = when (val v = o[key]) {
        is JsonArray -> v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> v.contentOrNull?.let { raw ->
            try {
                (json.parseToJsonElement(raw) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            } catch (_: Exception) {
                null
            }
        }.orEmpty()
        else -> emptyList()
    }

    fun parseMarket(o: JsonObject, eventSlugHint: String? = null): Market? {
        val question = o.str("question", "title") ?: return null
        val slug = o.str("slug") ?: return null
        val outcomes = stringList(o, "outcomes")
        val prices = stringList(o, "outcomePrices").mapNotNull { it.toDoubleOrNull() }
        if (outcomes.isEmpty() || outcomes.size != prices.size) return null
        val eventSlug = eventSlugHint ?: (o["events"] as? JsonArray)?.firstOrNull()?.let { (it as? JsonObject)?.str("slug") }
        return Market(
            id = o.str("id", "conditionId") ?: slug,
            question = question,
            slug = slug,
            eventSlug = eventSlug,
            outcomes = outcomes,
            prices = prices,
            tokenIds = stringList(o, "clobTokenIds"),
            volume = o.num("volumeNum", "volume"),
            volume24h = o.num("volume24hr", "volume24hrClob"),
            liquidity = o.num("liquidityNum", "liquidity"),
            oneDayChange = o.num("oneDayPriceChange"),
            endDate = parseDate(o.str("endDate", "endDateIso")),
            closed = o.bool("closed") ?: false,
            description = o.str("description").orEmpty().take(1500)
        )
    }

    /** /markets returns an array of markets. */
    fun parseMarkets(root: JsonElement): List<Market> = root.arrayOf("markets", "data").mapNotNull { parseMarket(it) }

    /** /public-search returns events, each with nested markets. Closed markets are dropped. */
    fun parseSearch(root: JsonElement): List<Market> =
        root.arrayOf("events").flatMap { e ->
            val slug = e.str("slug")
            e.arrayOf("markets").mapNotNull { parseMarket(it, slug) }
        }.filter { !it.closed }.distinctBy { it.slug }

    fun parsePositions(root: JsonElement): List<Position> = root.arrayOf("positions", "data").mapNotNull { p ->
        val title = p.str("title") ?: return@mapNotNull null
        val size = p.num("size") ?: return@mapNotNull null
        Position(
            title = title,
            outcome = p.str("outcome").orEmpty(),
            size = size,
            avgPrice = p.num("avgPrice", "avg_price"),
            curPrice = p.num("curPrice", "cur_price"),
            currentValue = p.num("currentValue", "current_value"),
            cashPnl = p.num("cashPnl", "cash_pnl"),
            percentPnl = p.num("percentPnl", "percent_pnl"),
            slug = p.str("slug"),
            eventSlug = p.str("eventSlug", "event_slug"),
            endDate = parseDate(p.str("endDate", "end_date")),
            redeemable = p.bool("redeemable") ?: false
        )
    }.filter { it.size > 0 }

    fun parseHistory(root: JsonElement): List<PricePoint> =
        root.arrayOf("history").mapNotNull { h ->
            val t = h.num("t") ?: return@mapNotNull null
            val p = h.num("p") ?: return@mapNotNull null
            PricePoint((t * 1000).toLong(), p)
        }.sortedBy { it.t }

    fun parseDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val core = raw.replace(Regex("""(\.\d+)?(Z|[+-]\d{2}:?\d{2})$"""), "")
        for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")) {
            val f = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
            val pos = java.text.ParsePosition(0)
            val d = f.parse(core, pos)
            if (d != null && pos.index == core.length) return d.time
        }
        return null
    }

    fun percent(p: Double): String = "${"%.0f".format(Locale.US, p * 100)}%"

    fun money(v: Double): String = when {
        v >= 1_000_000 -> "$" + "%.1f".format(Locale.US, v / 1_000_000) + "M"
        v >= 1_000 -> "$" + "%.1f".format(Locale.US, v / 1_000) + "k"
        else -> "$" + "%.2f".format(Locale.US, v)
    }

    /** Text block the model reads. */
    fun describe(markets: List<Market>, max: Int = 8): String = if (markets.isEmpty()) "No open markets found." else buildString {
        appendLine("Live Polymarket markets (prices are the market's implied probabilities):")
        markets.take(max).forEachIndexed { i, m ->
            appendLine("[${i + 1}] ${m.question}")
            appendLine("    " + m.outcomes.zip(m.prices).joinToString(" · ") { (o, p) -> "$o ${percent(p)}" } +
                (m.oneDayChange?.let { " · 24h change ${"%+.1f".format(Locale.US, it * 100)} pts" } ?: "") +
                (m.volume24h?.let { " · 24h volume ${money(it)}" } ?: "") +
                (m.endDate?.let { " · ends ${com.example.util.AgentTime.label(it).substringBeforeLast(',')}" } ?: ""))
            appendLine("    slug: ${m.slug} · ${m.url}")
        }
    }

    fun describePositions(positions: List<Position>): String = if (positions.isEmpty()) "No open positions for this wallet." else buildString {
        val value = positions.sumOf { it.currentValue ?: 0.0 }
        val pnl = positions.sumOf { it.cashPnl ?: 0.0 }
        appendLine("Open positions: ${positions.size} · value ${money(value)} · unrealised P&L ${if (pnl >= 0) "+" else "-"}${money(kotlin.math.abs(pnl))}")
        positions.sortedByDescending { it.currentValue ?: 0.0 }.take(20).forEach { p ->
            appendLine("• ${p.title} — ${p.outcome}: ${"%.1f".format(Locale.US, p.size)} shares, avg ${p.avgPrice?.let { percent(it) } ?: "?"}, now ${p.curPrice?.let { percent(it) } ?: "?"}" +
                (p.cashPnl?.let { " · P&L ${if (it >= 0) "+" else "-"}${money(kotlin.math.abs(it))}" } ?: "") + (if (p.redeemable) " · resolved, can be redeemed" else ""))
        }
    }
}

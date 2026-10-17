package com.example.income

import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import java.util.TimeZone

/** Totals for the earnings tracker. Amounts are in minor units (cents) to avoid rounding drift. */
object EarningsMath {
    data class Entry(val source: String, val amountCents: Long, val minutes: Int?, val at: Long)

    data class SourceStats(val source: String, val totalCents: Long, val minutes: Int, val entries: Int) {
        /** Earned per hour, only when time was logged for this source. */
        val hourlyCents: Long? get() = if (minutes > 0) totalCents * 60 / minutes else null
    }

    data class Summary(
        val monthCents: Long,
        val lastMonthCents: Long,
        val allTimeCents: Long,
        val goalCents: Long,
        val bySource: List<SourceStats>
    ) {
        /** 0..1, or null when there's no goal. */
        val goalProgress: Float? get() = if (goalCents > 0) (monthCents.toFloat() / goalCents).coerceIn(0f, 1f) else null
    }

    fun monthStart(now: Long, zone: TimeZone = TimeZone.getDefault(), monthsBack: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -monthsBack)
        }.timeInMillis

    fun summarize(entries: List<Entry>, goalCents: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): Summary {
        val thisMonth = monthStart(now, zone)
        val lastMonth = monthStart(now, zone, 1)
        val bySource = entries.groupBy { it.source.trim().ifEmpty { "Other" }.lowercase() }
            .map { (_, list) ->
                SourceStats(
                    source = list.first().source.trim().ifEmpty { "Other" },
                    totalCents = list.sumOf { it.amountCents },
                    minutes = list.sumOf { it.minutes ?: 0 },
                    entries = list.size
                )
            }
            .sortedByDescending { it.totalCents }
        return Summary(
            monthCents = entries.filter { it.at >= thisMonth }.sumOf { it.amountCents },
            lastMonthCents = entries.filter { it.at in lastMonth until thisMonth }.sumOf { it.amountCents },
            allTimeCents = entries.sumOf { it.amountCents },
            goalCents = goalCents,
            bySource = bySource
        )
    }

    /** "12.50" or "12,50" → 1250. Returns null for anything that isn't a positive amount. */
    fun parseAmount(text: String): Long? {
        if (Regex("""-\s*\d""").containsMatchIn(text)) return null // refunds and losses aren't earnings
        val t = text.trim().replace(Regex("[^0-9.,]"), "")
        if (t.isEmpty()) return null
        val lastSep = maxOf(t.lastIndexOf('.'), t.lastIndexOf(','))
        val normalized = if (lastSep >= 0 && t.length - lastSep - 1 in 1..2) {
            t.substring(0, lastSep).replace(".", "").replace(",", "") + "." + t.substring(lastSep + 1)
        } else {
            t.replace(".", "").replace(",", "")
        }
        val value = normalized.toBigDecimalOrNull() ?: return null
        val cents = value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        return cents.takeIf { it > 0 }
    }

    fun format(cents: Long, currencyCode: String): String = try {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { currency = Currency.getInstance(currencyCode) }.format(cents / 100.0)
    } catch (_: Exception) {
        "%.2f %s".format(Locale.US, cents / 100.0, currencyCode)
    }

    fun defaultCurrency(): String = try {
        Currency.getInstance(Locale.getDefault()).currencyCode
    } catch (_: Exception) {
        "USD"
    }

    /** Plain text for the model (weekly check-in, chat questions). */
    fun describe(summary: Summary, currency: String, recent: List<Pair<Entry, String>>): String = buildString {
        appendLine("Earnings tracker (currency $currency):")
        appendLine("• This month: ${format(summary.monthCents, currency)}" + (if (summary.goalCents > 0) " of a ${format(summary.goalCents, currency)} goal" else " (no monthly goal set)"))
        appendLine("• Last month: ${format(summary.lastMonthCents, currency)} · All time: ${format(summary.allTimeCents, currency)}")
        if (summary.bySource.isEmpty()) appendLine("No earnings logged yet.")
        summary.bySource.take(10).forEach { s ->
            appendLine("• ${s.source}: ${format(s.totalCents, currency)} from ${s.entries} entr${if (s.entries == 1) "y" else "ies"}" +
                (s.hourlyCents?.let { " · ${format(it, currency)}/hour" } ?: " · no time logged"))
        }
        if (recent.isNotEmpty()) {
            appendLine("Recent entries:")
            recent.take(10).forEach { (e, date) -> appendLine("• $date ${e.source}: ${format(e.amountCents, currency)}" + (e.minutes?.let { " in ${it / 60}h${it % 60}m" } ?: "")) }
        }
    }
}

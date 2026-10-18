package com.example.markets

import kotlin.math.abs

/** Trade-ticket figures and the prediction track record. */
object MarketMath {
    data class Ticket(
        val outcome: String,
        val price: Double,
        val amount: Double,
        val shares: Double,
        /** What the shares pay if the outcome wins (each pays $1). */
        val payoutIfWin: Double,
        val profitIfWin: Double,
        /** The whole stake is lost if it doesn't. */
        val maxLoss: Double,
        /** Kora's estimate minus the price, if Kora made one; positive means Kora thinks it's underpriced. */
        val edge: Double?
    )

    /** Null when the inputs can't make a sensible ticket. Fees and slippage aren't included. */
    fun ticket(outcome: String, price: Double, amount: Double, koraProbability: Double? = null): Ticket? {
        if (price <= 0.0 || price >= 1.0 || amount <= 0.0 || amount > 1_000_000) return null
        val shares = amount / price
        return Ticket(outcome, price, amount, shares, shares, shares - amount, amount, koraProbability?.let { it - price })
    }

    /** "Buy 37.7 Yes shares at 53¢ for $20.00 · pays $37.74 if Yes (+$17.74) · you can lose all $20.00" */
    fun describeTicket(t: Ticket): String {
        fun d(v: Double) = "$" + "%.2f".format(java.util.Locale.US, v)
        return "Buy ${"%.1f".format(java.util.Locale.US, t.shares)} ${t.outcome} shares at ${"%.0f".format(java.util.Locale.US, t.price * 100)}¢ for ${d(t.amount)} · " +
            "pays ${d(t.payoutIfWin)} if ${t.outcome} (+${d(t.profitIfWin)}) · you can lose all ${d(t.maxLoss)}. Fees and price moves not included."
    }

    data class Record(val count: Int, val koraBrier: Double, val marketBrier: Double, val koraBetter: Int) {
        /** Lower Brier is better; Kora "beats the market" when its score is lower. */
        val beatsMarket: Boolean get() = count > 0 && koraBrier < marketBrier
    }

    data class Scored(val koraProbability: Double, val marketProbability: Double, val happened: Boolean)

    /** Brier score: mean squared error of the probability given to what happened (0 perfect, 0.25 coin flip). */
    fun record(resolved: List<Scored>): Record {
        if (resolved.isEmpty()) return Record(0, 0.0, 0.0, 0)
        fun brier(p: Double, hit: Boolean) = (p - if (hit) 1.0 else 0.0).let { it * it }
        val kora = resolved.map { brier(it.koraProbability, it.happened) }
        val market = resolved.map { brier(it.marketProbability, it.happened) }
        return Record(
            count = resolved.size,
            koraBrier = kora.average(),
            marketBrier = market.average(),
            koraBetter = kora.zip(market).count { (k, m) -> k < m }
        )
    }

    /** A big move worth flagging in alerts. */
    fun isBigMove(change: Double?, threshold: Double = 0.1): Boolean = change != null && abs(change) >= threshold

    /** Price change over a history (last minus first), or null if too short. */
    fun change(history: List<PricePoint>): Double? = if (history.size < 2) null else history.last().p - history.first().p
}

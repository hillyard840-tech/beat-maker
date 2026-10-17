package com.example.income

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.Earning
import com.example.util.AgentTime

/** Tracker settings (currency, monthly goal) and the text Kora reads. */
object EarningsStore {
    private const val K_CURRENCY = "earnings_currency"
    private const val K_GOAL = "earnings_goal_cents"

    private fun prefs(context: Context) = context.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)

    fun currency(context: Context): String = prefs(context).getString(K_CURRENCY, null) ?: EarningsMath.defaultCurrency()
    fun goalCents(context: Context): Long = prefs(context).getLong(K_GOAL, 0L)

    fun saveSettings(context: Context, currency: String, goalCents: Long) {
        prefs(context).edit().putString(K_CURRENCY, currency.uppercase().take(3)).putLong(K_GOAL, goalCents.coerceAtLeast(0)).apply()
    }

    fun toEntry(e: Earning) = EarningsMath.Entry(e.source, e.amountCents, e.minutes, e.earnedAt)

    suspend fun describe(context: Context): String {
        val all = AppDatabase.getInstance(context).earningDao().allOnce()
        val summary = EarningsMath.summarize(all.map(::toEntry), goalCents(context), System.currentTimeMillis())
        return EarningsMath.describe(summary, currency(context), all.take(10).map { toEntry(it) to AgentTime.label(it.earnedAt).substringBeforeLast(',') })
    }
}

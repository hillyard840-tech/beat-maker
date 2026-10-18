package com.example.markets

import android.content.Context
import com.example.agent.AgentRunner
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.LlmSettings
import com.example.api.Part
import com.example.data.AppDatabase
import com.example.data.Prediction
import com.example.util.LiveInternetService

/** Wallet setting, Kora's predictions and their scoring. */
object MarketsStore {
    private const val K_WALLET = "polymarket_wallet"

    private fun prefs(context: Context) = context.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)

    /** A public address only. Kora never asks for or stores a private key or seed phrase. */
    fun wallet(context: Context): String = prefs(context).getString(K_WALLET, "").orEmpty()

    fun setWallet(context: Context, address: String) {
        prefs(context).edit().putString(K_WALLET, address.trim()).apply()
    }

    /** Asks the AI for an estimate grounded in fresh web results, and saves it. */
    suspend fun predict(context: Context, market: Market, outcome: String): Pair<Prediction, KoraEstimate> {
        val price = market.priceOf(outcome) ?: throw IllegalArgumentException("Unknown outcome")
        val prefs = prefs(context)
        val settings = LlmSettings.fromPrefs(prefs)
        if (!settings.isReady) throw IllegalStateException("Add your Gemini API key or server in Settings first.")
        val results = if (prefs.getBoolean("live_internet_enabled", true)) LiveInternetService.searchLiveInternet(market.question, 6) else emptyList()
        val news = results.joinToString("\n") { "- ${it.title} (${it.url}): ${it.snippet}" }
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = PredictionParser.prompt(market, outcome, news))))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Kora.Ai, a calibrated, honest forecaster. You answer with valid JSON only.")))
        )
        val response = AgentRunner.generateWithFallback(settings, AgentRunner.modelsFor(settings, prefs.getString("selected_model", null).orEmpty()), request)
        val text = response.candidates.firstOrNull()?.content?.parts?.filter { it.thought != true }?.mapNotNull { it.text }?.joinToString("\n").orEmpty()
        val estimate = PredictionParser.parse(text, results.map { it.url }.toSet())
            ?: throw IllegalStateException("The AI's answer couldn't be read. Try again.")
        val prediction = Prediction(
            marketSlug = market.slug,
            eventSlug = market.eventSlug,
            question = market.question,
            outcome = outcome,
            marketProbability = price,
            koraProbability = estimate.probability,
            confidence = estimate.confidence,
            reasoning = estimate.reasoning,
            endDate = market.endDate
        )
        val id = AppDatabase.getInstance(context).predictionDao().insert(prediction).toInt()
        return prediction.copy(id = id) to estimate
    }

    /** Checks unresolved predictions against Polymarket and records the outcome. Returns how many resolved. */
    suspend fun resolvePending(context: Context): Int {
        val dao = AppDatabase.getInstance(context).predictionDao()
        var resolved = 0
        dao.unresolved()
            .filter { it.endDate == null || it.endDate <= System.currentTimeMillis() }
            .take(25)
            .forEach { p ->
                val market = try {
                    PolymarketClient.market(p.marketSlug)
                } catch (_: Exception) {
                    null
                } ?: return@forEach
                val winner = market.winner ?: return@forEach
                dao.resolve(p.id, winner.equals(p.outcome, ignoreCase = true), System.currentTimeMillis())
                resolved++
            }
        return resolved
    }

    fun record(predictions: List<Prediction>): MarketMath.Record =
        MarketMath.record(predictions.mapNotNull { p -> p.happened?.let { MarketMath.Scored(p.koraProbability, p.marketProbability, it) } })

    suspend fun describeRecord(context: Context): String {
        val all = AppDatabase.getInstance(context).predictionDao().allOnce()
        val r = record(all)
        return if (r.count == 0) "Kora's track record: ${all.size} prediction(s) made, none resolved yet."
        else "Kora's track record on ${r.count} resolved prediction(s): Brier score ${"%.3f".format(r.koraBrier)} vs the market's ${"%.3f".format(r.marketBrier)} " +
            "(lower is better; Kora was closer on ${r.koraBetter} of ${r.count}). " + if (r.beatsMarket) "Kora is ahead of the market so far." else "The market has been more accurate than Kora so far."
    }
}

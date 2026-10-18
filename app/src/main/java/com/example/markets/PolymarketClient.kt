package com.example.markets

import com.example.data.remote.JsonHttp
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Read-only Polymarket client using its public APIs (no account, key or wallet). Kora never signs
 * or places orders.
 */
object PolymarketClient {
    suspend fun trending(limit: Int = 20): List<Market> = Polymarket.parseMarkets(
        JsonHttp.get(
            "${Polymarket.GAMMA}/markets".toHttpUrl().newBuilder()
                .addQueryParameter("active", "true")
                .addQueryParameter("closed", "false")
                .addQueryParameter("order", "volume24hr")
                .addQueryParameter("ascending", "false")
                .addQueryParameter("limit", limit.toString())
                .build().toString()
        )
    )

    suspend fun search(query: String): List<Market> = Polymarket.parseSearch(
        JsonHttp.get(
            "${Polymarket.GAMMA}/public-search".toHttpUrl().newBuilder()
                .addQueryParameter("q", query.trim().take(100))
                .addQueryParameter("limit_per_type", "10")
                .build().toString()
        )
    )

    suspend fun market(slug: String): Market? = Polymarket.parseMarkets(
        JsonHttp.get("${Polymarket.GAMMA}/markets".toHttpUrl().newBuilder().addQueryParameter("slug", slug.trim()).build().toString())
    ).firstOrNull()

    /** A week of hourly prices for one outcome token. Empty for very new markets. */
    suspend fun history(tokenId: String, interval: String = "1w", fidelityMinutes: Int = 60): List<PricePoint> = Polymarket.parseHistory(
        JsonHttp.get(
            "${Polymarket.CLOB}/prices-history".toHttpUrl().newBuilder()
                .addQueryParameter("market", tokenId)
                .addQueryParameter("interval", interval)
                .addQueryParameter("fidelity", fidelityMinutes.toString())
                .build().toString()
        )
    )

    suspend fun positions(wallet: String): List<Position> {
        require(Polymarket.isWalletAddress(wallet)) { "That isn't a wallet address (0x followed by 40 characters)." }
        return Polymarket.parsePositions(
            JsonHttp.get(
                "${Polymarket.DATA}/positions".toHttpUrl().newBuilder()
                    .addQueryParameter("user", wallet.trim())
                    .addQueryParameter("sizeThreshold", "0.1")
                    .addQueryParameter("limit", "200")
                    .build().toString()
            )
        )
    }
}

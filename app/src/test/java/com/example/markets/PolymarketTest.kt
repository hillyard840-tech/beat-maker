package com.example.markets

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolymarketTest {
    private fun json(s: String) = Json.parseToJsonElement(s)

    private val gammaMarket = """{"id":"123","question":"Will it rain in NYC tomorrow?","slug":"rain-nyc","outcomes":"[\"Yes\", \"No\"]",
        "outcomePrices":"[\"0.62\", \"0.38\"]","clobTokenIds":"[\"111\", \"222\"]","volumeNum":152340.5,"volume24hr":"8200.1",
        "liquidityNum":12000,"oneDayPriceChange":-0.04,"endDate":"2026-10-19T12:00:00Z","closed":false,
        "events":[{"slug":"nyc-weather"}],"description":"Resolves Yes if..."}"""

    @Test
    fun `gamma markets with stringified arrays are parsed`() {
        val m = Polymarket.parseMarkets(json("[$gammaMarket]")).single()
        assertEquals(listOf("Yes", "No"), m.outcomes)
        assertEquals(listOf(0.62, 0.38), m.prices)
        assertEquals(listOf("111", "222"), m.tokenIds)
        assertEquals(8200.1, m.volume24h!!, 0.001)
        assertEquals("https://polymarket.com/event/nyc-weather/rain-nyc", m.url)
        assertEquals(0.62, m.priceOf("yes")!!, 0.0)
        assertTrue(m.endDate != null)
        assertNull(m.winner)
    }

    @Test
    fun `mismatched outcomes and prices are dropped`() {
        val bad = """[{"question":"Q","slug":"q","outcomes":"[\"Yes\",\"No\"]","outcomePrices":"[\"0.5\"]"}]"""
        assertTrue(Polymarket.parseMarkets(json(bad)).isEmpty())
        assertTrue(Polymarket.parseMarkets(json("""{"error":"x"}""")).isEmpty())
    }

    @Test
    fun `search results keep the event slug and skip closed markets`() {
        val root = json(
            """{"events":[{"slug":"fed-dec","markets":[
                {"question":"Fed cuts in December?","slug":"fed-cut-dec","outcomes":"[\"Yes\",\"No\"]","outcomePrices":"[\"0.3\",\"0.7\"]"},
                {"question":"Old one","slug":"old","outcomes":"[\"Yes\",\"No\"]","outcomePrices":"[\"1\",\"0\"]","closed":true}]}]}"""
        )
        val markets = Polymarket.parseSearch(root)
        assertEquals(listOf("fed-cut-dec"), markets.map { it.slug })
        assertEquals("https://polymarket.com/event/fed-dec/fed-cut-dec", markets[0].url)
    }

    @Test
    fun `resolved market reports its winner`() {
        val m = Polymarket.parseMarkets(json("""[{"question":"Q","slug":"q","outcomes":["Yes","No"],"outcomePrices":["0.0005","0.9995"],"closed":true}]""")).single()
        assertEquals("No", m.winner)
    }

    @Test
    fun `positions and history parse from the data and clob apis`() {
        val positions = Polymarket.parsePositions(
            json("""[{"title":"Will it rain?","outcome":"Yes","size":40,"avgPrice":0.5,"curPrice":0.62,"currentValue":24.8,"cashPnl":4.8,"percentPnl":24,"slug":"rain-nyc","eventSlug":"nyc-weather","redeemable":false},
                     {"title":"Dust","outcome":"No","size":0}]""")
        )
        assertEquals(1, positions.size)
        assertEquals(4.8, positions[0].cashPnl!!, 0.0)
        assertEquals("https://polymarket.com/event/nyc-weather/rain-nyc", positions[0].url)
        val history = Polymarket.parseHistory(json("""{"history":[{"t":1760000060,"p":0.6},{"t":1760000000,"p":0.55}]}"""))
        assertEquals(listOf(0.55, 0.6), history.map { it.p })
        assertEquals(1760000000000L, history[0].t)
        assertEquals(0.05, MarketMath.change(history)!!, 1e-9)
    }

    @Test
    fun `only real polymarket links and public addresses are accepted`() {
        assertTrue(Polymarket.isPolymarketUrl("https://polymarket.com/event/nyc-weather/rain-nyc"))
        assertTrue(Polymarket.isPolymarketUrl("https://polymarket.com/event/rain-nyc"))
        assertFalse(Polymarket.isPolymarketUrl("https://polymarket.com.evil.io/event/x"))
        assertFalse(Polymarket.isPolymarketUrl("http://polymarket.com/event/x"))
        assertFalse(Polymarket.isPolymarketUrl("https://polymarket.com/event/x?redirect=https://evil"))
        assertTrue(Polymarket.isWalletAddress("0x" + "a".repeat(40)))
        assertFalse(Polymarket.isWalletAddress("0x" + "a".repeat(64))) // a private key, not an address
    }

    @Test
    fun `trade ticket maths`() {
        val t = MarketMath.ticket("Yes", 0.53, 20.0, koraProbability = 0.6)!!
        assertEquals(37.735, t.shares, 0.001)
        assertEquals(17.735, t.profitIfWin, 0.001)
        assertEquals(20.0, t.maxLoss, 0.0)
        assertEquals(0.07, t.edge!!, 1e-9)
        assertTrue(MarketMath.describeTicket(t).contains("you can lose all $20.00"))
        assertNull(MarketMath.ticket("Yes", 1.0, 20.0))
        assertNull(MarketMath.ticket("Yes", 0.5, 0.0))
    }

    @Test
    fun `brier track record compares kora with the market`() {
        val r = MarketMath.record(
            listOf(
                MarketMath.Scored(0.8, 0.6, true),  // kora .04, market .16
                MarketMath.Scored(0.3, 0.2, false)  // kora .09, market .04
            )
        )
        assertEquals(2, r.count)
        assertEquals(0.065, r.koraBrier, 1e-9)
        assertEquals(0.10, r.marketBrier, 1e-9)
        assertEquals(1, r.koraBetter)
        assertTrue(r.beatsMarket)
        assertFalse(MarketMath.record(emptyList()).beatsMarket)
    }

    @Test
    fun `prediction json is parsed with sources limited to real results`() {
        val e = PredictionParser.parse(
            """Sure: {"probability":"58%","confidence":"Medium","reasoning":"Polls tightened.","factors":["polls"],"sources":["https://a.example","https://made-up.example"]}""",
            setOf("https://a.example")
        )!!
        assertEquals(0.58, e.probability, 1e-9)
        assertEquals("medium", e.confidence)
        assertEquals(listOf("https://a.example"), e.sources)
        assertNull(PredictionParser.parse("""{"probability":1.0,"reasoning":"certain"}""", emptySet()))
        assertNull(PredictionParser.parse("no json", emptySet()))
        assertEquals(0.62, PredictionParser.probability("62")!!, 1e-9)
    }
}

package com.example.markets

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.TaskScheduler
import com.example.data.AppDatabase
import com.example.data.Prediction
import com.example.data.ScheduledTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Live Polymarket data, the user's positions by public address, and Kora's predictions. */
class MarketsViewModel(application: Application) : AndroidViewModel(application) {
    sealed class Load<out T> {
        data object Idle : Load<Nothing>()
        data object Loading : Load<Nothing>()
        data class Done<T>(val value: T) : Load<T>()
        data class Failed(val message: String) : Load<Nothing>()
    }

    private val prefs = application.getSharedPreferences("omni_agent_prefs", Context.MODE_PRIVATE)
    private val predictionDao = AppDatabase.getInstance(application).predictionDao()

    val query = MutableStateFlow("")
    val markets = MutableStateFlow<Load<List<Market>>>(Load.Idle)
    val selected = MutableStateFlow<Market?>(null)
    val history = MutableStateFlow<Load<List<PricePoint>>>(Load.Idle)
    val estimate = MutableStateFlow<Load<Pair<Prediction, KoraEstimate>>>(Load.Idle)

    val wallet = MutableStateFlow(MarketsStore.wallet(application))
    val positions = MutableStateFlow<Load<List<Position>>>(Load.Idle)

    val predictions: StateFlow<List<Prediction>> = predictionDao.all().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val notice = MutableStateFlow<String?>(null)

    private var searchJob: Job? = null

    private fun webAllowed(): Boolean = prefs.getBoolean("live_internet_enabled", true)

    private fun <T> run(target: MutableStateFlow<Load<T>>, block: suspend () -> T): Job? {
        if (!webAllowed()) {
            target.value = Load.Failed("Live web is off. Polymarket data comes from the internet, so turn it on in the chat's Tools.")
            return null
        }
        return viewModelScope.launch {
            target.value = Load.Loading
            target.value = try {
                Load.Done(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Failed(e.message?.takeIf { it.length < 200 } ?: "Polymarket couldn't be reached. Try again.")
            }
        }
    }

    fun loadTrending() {
        searchJob?.cancel()
        searchJob = run(markets) { PolymarketClient.trending(25) }
    }

    fun search() {
        val q = query.value.trim()
        if (q.isEmpty()) return loadTrending()
        searchJob?.cancel()
        searchJob = run(markets) { PolymarketClient.search(q) }
    }

    fun open(market: Market) {
        selected.value = market
        estimate.value = Load.Idle
        val token = market.tokenIds.firstOrNull()
        if (token == null) history.value = Load.Done(emptyList()) else run(history) { PolymarketClient.history(token) }
    }

    fun close() {
        selected.value = null
    }

    fun askKora(market: Market, outcome: String) {
        if (estimate.value is Load.Loading) return
        viewModelScope.launch {
            estimate.value = Load.Loading
            estimate.value = try {
                Load.Done(MarketsStore.predict(getApplication(), market, outcome))
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                Load.Failed(if (e.code() == 429) "Your AI quota is used up for now. Try again later." else "The AI request failed (${e.code()}).")
            } catch (e: Exception) {
                Load.Failed(e.message ?: "Something went wrong. Try again.")
            }
        }
    }

    /** Saves a public address. Anything that looks like a private key is refused outright. */
    fun saveWallet(address: String): Boolean {
        val clean = address.trim()
        if (Regex("^(0x)?[0-9a-fA-F]{64}$").matches(clean) || clean.split(Regex("\\s+")).size >= 12) {
            notice.value = "That looks like a private key or seed phrase. Never share it with any app. Use your public 0x… address."
            return false
        }
        if (clean.isNotEmpty() && !Polymarket.isWalletAddress(clean)) {
            notice.value = "That isn't a wallet address (0x followed by 40 characters)."
            return false
        }
        MarketsStore.setWallet(getApplication(), clean)
        wallet.value = clean
        if (clean.isEmpty()) positions.value = Load.Idle else loadPositions()
        return true
    }

    fun loadPositions() {
        val w = wallet.value
        if (!Polymarket.isWalletAddress(w)) return
        run(positions) { PolymarketClient.positions(w) }
    }

    fun checkResults() {
        if (!webAllowed()) {
            notice.value = "Turn on Live web to check results"
            return
        }
        viewModelScope.launch {
            val n = try {
                MarketsStore.resolvePending(getApplication())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                -1
            }
            notice.value = when {
                n < 0 -> "Couldn't reach Polymarket"
                n == 0 -> "No new results yet"
                else -> "$n prediction${if (n == 1) "" else "s"} resolved"
            }
        }
    }

    fun deletePrediction(id: Int) {
        viewModelScope.launch { predictionDao.delete(id) }
    }

    /** A daily morning task: Kora checks the portfolio, flags big moves and related news. */
    fun createWatch() {
        if (!Polymarket.isWalletAddress(wallet.value)) {
            notice.value = "Add your wallet address first"
            return
        }
        viewModelScope.launch {
            TaskScheduler.save(
                getApplication(),
                ScheduledTask(
                    title = "Polymarket watch",
                    prompt = "Call polymarket_positions. For each position, flag big price moves and positions that resolved or can be redeemed. " +
                        "Search the web for news on my 3 largest positions and say whether anything changes the picture. Be brief and don't tell me to trade; just inform me.",
                    hour = 8,
                    minute = 15,
                    repeat = ScheduledTask.REPEAT_DAILY,
                    useWeb = true
                )
            )
            notice.value = "Kora will check your positions every morning at 8:15"
        }
    }
}

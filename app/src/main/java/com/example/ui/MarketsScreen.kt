package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Prediction
import com.example.markets.Market
import com.example.markets.MarketMath
import com.example.markets.MarketsStore
import com.example.markets.MarketsViewModel
import com.example.markets.MarketsViewModel.Load
import com.example.markets.Polymarket
import com.example.markets.Position
import com.example.markets.PricePoint
import com.example.ui.theme.status
import com.example.util.AgentTime
import java.util.Locale

private enum class MarketsTab(val label: String) { EXPLORE("Explore"), PORTFOLIO("Portfolio"), PREDICTIONS("Kora's record") }

/**
 * Polymarket analysis: live odds, the user's positions (by public address), Kora's predictions
 * with a public track record, and trade tickets that open Polymarket. Kora never places trades.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(onBack: () -> Unit, onAskKora: (String) -> Unit, vm: MarketsViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(MarketsTab.EXPLORE) }
    val markets by vm.markets.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val wallet by vm.wallet.collectAsStateWithLifecycle()
    val positions by vm.positions.collectAsStateWithLifecycle()
    val predictions by vm.predictions.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (vm.markets.value is Load.Idle) vm.loadTrending()
        if (vm.positions.value is Load.Idle) vm.loadPositions()
    }
    LaunchedEffect(notice) {
        notice?.let {
            snackbar.showSnackbar(it)
            vm.notice.value = null
        }
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markets") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab.ordinal) {
                MarketsTab.entries.forEach { t -> Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) }) }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { RiskBanner() }
                when (tab) {
                    MarketsTab.EXPLORE -> exploreTab(markets, query, vm)
                    MarketsTab.PORTFOLIO -> portfolioTab(wallet, positions, vm, onAskKora)
                    MarketsTab.PREDICTIONS -> predictionsTab(predictions, vm)
                }
            }
        }
    }

    selected?.let { market ->
        MarketSheet(market, vm, onAskKora = onAskKora, onDismiss = { vm.close() })
    }
}

@Composable
private fun RiskBanner() {
    Text(
        "Prediction markets are risky: you can lose the whole stake. Kora's analysis isn't financial advice, and Kora never places trades. Check that Polymarket is legal where you live.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ---------------------------------------------------------------- explore

private fun LazyListScope.exploreTab(state: Load<List<Market>>, query: String, vm: MarketsViewModel) {
    item {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it.take(100) },
            label = { Text("Search markets") },
            placeholder = { Text("e.g. election, fed, bitcoin, world cup") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            trailingIcon = { IconButton(onClick = { vm.search() }) { Icon(Icons.Default.Search, contentDescription = "Search") } },
            modifier = Modifier.fillMaxWidth()
        )
    }
    item {
        when (state) {
            Load.Idle, Load.Loading -> MarketsLoading("Loading live markets…")
            is Load.Failed -> MarketsError(state.message) { vm.search() }
            is Load.Done -> Text(
                if (state.value.isEmpty()) "No open markets match." else if (query.isBlank()) "Top markets by 24h volume" else "${state.value.size} open markets",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
    if (state is Load.Done) {
        items(state.value, key = { "m_" + it.slug }) { m -> MarketRow(m) { vm.open(m) } }
    }
}

@Composable
private fun MarketRow(m: Market, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(m.question, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.outcomes.zip(m.prices).take(3).joinToString("  ·  ") { (o, p) -> "$o ${Polymarket.percent(p)}" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                m.oneDayChange?.let { ChangeText(it) }
            }
            Text(
                listOfNotNull(
                    m.volume24h?.let { "24h vol ${Polymarket.money(it)}" },
                    m.liquidity?.let { "liquidity ${Polymarket.money(it)}" },
                    m.endDate?.let { "ends ${AgentTime.label(it).substringBeforeLast(',')}" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChangeText(change: Double) {
    val up = change >= 0
    Text(
        "${if (up) "▲" else "▼"} ${"%.1f".format(Locale.US, kotlin.math.abs(change) * 100)} pts",
        style = MaterialTheme.typography.labelMedium,
        color = if (up) MaterialTheme.status.success else MaterialTheme.colorScheme.error
    )
}

// ---------------------------------------------------------------- market detail

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MarketSheet(market: Market, vm: MarketsViewModel, onAskKora: (String) -> Unit, onDismiss: () -> Unit) {
    val history by vm.history.collectAsStateWithLifecycle()
    val estimate by vm.estimate.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    var outcome by remember(market.slug) { mutableStateOf(market.outcomes.first()) }
    var showTicket by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(market.question, style = MaterialTheme.typography.titleLarge)
            market.outcomes.zip(market.prices).forEach { (o, p) ->
                Column {
                    Row {
                        Text(o, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(Polymarket.percent(p), style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(progress = { p.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
            Text(
                listOfNotNull(
                    market.volume?.let { "Volume ${Polymarket.money(it)}" },
                    market.volume24h?.let { "24h ${Polymarket.money(it)}" },
                    market.liquidity?.let { "Liquidity ${Polymarket.money(it)}" },
                    market.endDate?.let { "Ends ${AgentTime.label(it)}" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("${market.outcomes.first()} price, last 7 days", style = MaterialTheme.typography.labelLarge)
            when (val h = history) {
                Load.Idle, Load.Loading -> MarketsLoading("Loading price history…")
                is Load.Failed -> Text(h.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                is Load.Done -> if (h.value.size < 2) {
                    Text("Not enough trading history yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Sparkline(h.value)
                    MarketMath.change(h.value)?.let { ChangeText(it) }
                }
            }

            if (market.description.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                Text(
                    "Resolution rules: " + market.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            HorizontalDivider()
            Text("Outcome", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                market.outcomes.forEach { o -> FilterChip(selected = outcome == o, onClick = { outcome = o }, label = { Text(o) }) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.askKora(market, outcome) }, enabled = estimate !is Load.Loading) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kora's estimate")
                }
                OutlinedButton(onClick = { showTicket = true }) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Trade ticket")
                }
            }
            when (val e = estimate) {
                Load.Idle -> {}
                Load.Loading -> MarketsLoading("Reading the news and thinking…")
                is Load.Failed -> MarketsError(e.message) { vm.askKora(market, outcome) }
                is Load.Done -> EstimateCard(e.value.first, e.value.second)
            }
            FlowRow {
                TextButton(onClick = { uri.openSafely(market.url) }) { Text("Open in Polymarket") }
                TextButton(onClick = {
                    onDismiss()
                    onAskKora(
                        "Let's discuss this Polymarket market: \"${market.question}\" (${market.url}). Current prices: " +
                            market.outcomes.zip(market.prices).joinToString { (o, p) -> "$o ${Polymarket.percent(p)}" } +
                            ". Search the web for the latest news, tell me the key things that would move it, and where the market could be wrong."
                    )
                }) { Text("Discuss in chat") }
            }
        }
    }

    if (showTicket) {
        val kora = (estimate as? Load.Done)?.value?.first?.takeIf { it.marketSlug == market.slug && it.outcome == outcome }?.koraProbability
        TradeTicketDialog(market, outcome, kora, onDismiss = { showTicket = false }) {
            showTicket = false
            uri.openSafely(market.url)
        }
    }
}

@Composable
private fun EstimateCard(p: Prediction, estimate: com.example.markets.KoraEstimate) {
    val uri = LocalUriHandler.current
    val diff = p.koraProbability - p.marketProbability
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Market", style = MaterialTheme.typography.labelMedium)
                    Text(Polymarket.percent(p.marketProbability), style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Kora", style = MaterialTheme.typography.labelMedium)
                    Text(Polymarket.percent(p.koraProbability), style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Confidence", style = MaterialTheme.typography.labelMedium)
                    Text(p.confidence.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                when {
                    kotlin.math.abs(diff) < 0.03 -> "Kora roughly agrees with the market on \"${p.outcome}\"."
                    diff > 0 -> "Kora thinks \"${p.outcome}\" is more likely than the market by ${"%.0f".format(Locale.US, diff * 100)} points."
                    else -> "Kora thinks \"${p.outcome}\" is less likely than the market by ${"%.0f".format(Locale.US, -diff * 100)} points."
                },
                style = MaterialTheme.typography.titleSmall
            )
            Text(estimate.reasoning, style = MaterialTheme.typography.bodySmall)
            estimate.factors.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            estimate.sources.forEach { url ->
                Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { uri.openSafely(url) })
            }
            Text(
                "Saved to Kora's record. It's scored when the market resolves, so you can see whether Kora actually beats the market.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun TradeTicketDialog(market: Market, outcome: String, koraProbability: Double?, onDismiss: () -> Unit, onOpen: () -> Unit) {
    var amountText by remember { mutableStateOf("10") }
    val price = market.priceOf(outcome) ?: 0.0
    val ticket = amountText.replace(',', '.').toDoubleOrNull()?.let { MarketMath.ticket(outcome, price, it, koraProbability) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
        title = { Text("Trade ticket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Buy \"$outcome\" on: ${market.question}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.take(8) },
                    label = { Text("Amount (USD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (ticket == null) {
                    Text("Enter an amount between \$1 and \$10,000. Prices of 0% or 100% can't be traded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(MarketMath.describeTicket(ticket), style = MaterialTheme.typography.bodyMedium)
                    ticket.edge?.let {
                        Text(
                            "Kora's estimate is ${"%+.0f".format(Locale.US, it * 100)} points vs the price. That's an opinion, not an edge you can count on.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    "Kora doesn't place trades. \"Open in Polymarket\" takes you to the market; you choose whether to place the order there. Only risk money you can afford to lose.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onOpen, enabled = ticket != null) { Text("Open in Polymarket") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Sparkline(points: List<PricePoint>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val minP = points.minOf { it.p }
        val maxP = points.maxOf { it.p }
        val span = (maxP - minP).takeIf { it > 0.0001 } ?: 0.01
        val t0 = points.first().t
        val tSpan = (points.last().t - t0).coerceAtLeast(1)
        drawLine(grid, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
        val path = Path()
        points.forEachIndexed { i, pt ->
            val x = ((pt.t - t0).toFloat() / tSpan) * size.width
            val y = size.height - ((pt.p - minP) / span).toFloat() * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = 4f))
    }
    Text(
        "Range ${Polymarket.percent(points.minOf { it.p })}–${Polymarket.percent(points.maxOf { it.p })}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ---------------------------------------------------------------- portfolio

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.portfolioTab(wallet: String, state: Load<List<Position>>, vm: MarketsViewModel, onAskKora: (String) -> Unit) {
    item {
        var text by remember(wallet) { mutableStateOf(wallet) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your Polymarket wallet address", style = MaterialTheme.typography.titleSmall)
            Text(
                "Paste the public 0x… address from your Polymarket profile. It's read-only: positions are public by address. Never paste a private key or seed phrase into any app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.trim().take(80) },
                placeholder = { Text("0x…") },
                singleLine = true,
                trailingIcon = { TextButton(onClick = { vm.saveWallet(text) }) { Text("Save") } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    if (!Polymarket.isWalletAddress(wallet)) return
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { vm.loadPositions() }, leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) }, label = { Text("Refresh") })
            AssistChip(onClick = { vm.createWatch() }, leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) }, label = { Text("Daily watch") })
            AssistChip(
                onClick = { onAskKora("Call polymarket_positions and review my Polymarket portfolio: concentration, positions near resolution, big moves, and any news that changes the picture. Don't tell me to trade; help me think.") },
                leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Review with Kora") }
            )
        }
    }
    item {
        when (state) {
            Load.Idle, Load.Loading -> MarketsLoading("Loading your positions…")
            is Load.Failed -> MarketsError(state.message) { vm.loadPositions() }
            is Load.Done -> {
                val value = state.value.sumOf { it.currentValue ?: 0.0 }
                val pnl = state.value.sumOf { it.cashPnl ?: 0.0 }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Open value", style = MaterialTheme.typography.labelMedium)
                            Text(Polymarket.money(value), style = MaterialTheme.typography.headlineSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Unrealised P&L", style = MaterialTheme.typography.labelMedium)
                            Text(
                                (if (pnl >= 0) "+" else "-") + Polymarket.money(kotlin.math.abs(pnl)),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (pnl >= 0) MaterialTheme.status.success else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
    if (state is Load.Done) {
        if (state.value.isEmpty()) item { Text("No open positions for this address.", style = MaterialTheme.typography.bodyMedium) }
        items(state.value, key = { "p_" + it.title + it.outcome }) { PositionRow(it) }
    }
}

@Composable
private fun PositionRow(p: Position) {
    val uri = LocalUriHandler.current
    ListItem(
        modifier = Modifier.clickable(enabled = p.url != null) { p.url?.let { uri.openSafely(it) } },
        headlineContent = { Text(p.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${p.outcome} · ${"%.1f".format(Locale.US, p.size)} shares · avg ${p.avgPrice?.let { Polymarket.percent(it) } ?: "?"} → now ${p.curPrice?.let { Polymarket.percent(it) } ?: "?"}" +
                    if (p.redeemable) " · resolved, redeem in Polymarket" else ""
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                p.currentValue?.let { Text(Polymarket.money(it), style = MaterialTheme.typography.titleSmall) }
                p.cashPnl?.let {
                    Text(
                        (if (it >= 0) "+" else "-") + Polymarket.money(kotlin.math.abs(it)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it >= 0) MaterialTheme.status.success else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

// ---------------------------------------------------------------- Kora's record

private fun LazyListScope.predictionsTab(predictions: List<Prediction>, vm: MarketsViewModel) {
    item {
        val r = MarketsStore.record(predictions)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Does Kora beat the market?", style = MaterialTheme.typography.titleMedium)
                if (r.count == 0) {
                    Text(
                        "${predictions.size} prediction${if (predictions.size == 1) "" else "s"} made, none resolved yet. Ask for Kora's estimate on a market; it's scored automatically when the market resolves.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        if (r.beatsMarket) "So far, yes: Kora's forecasts were more accurate than the market's." else "Not so far: the market's prices were more accurate than Kora.",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (r.beatsMarket) MaterialTheme.status.success else MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Brier score on ${r.count} resolved: Kora ${"%.3f".format(Locale.US, r.koraBrier)} vs market ${"%.3f".format(Locale.US, r.marketBrier)} (lower is better, 0.25 is a coin flip). Kora was closer on ${r.koraBetter} of ${r.count}.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (r.count < 20) Text("Too few results to mean much yet.", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = { vm.checkResults() }, contentPadding = PaddingValues(0.dp)) { Text("Check results now") }
            }
        }
    }
    items(predictions, key = { "pr_" + it.id }) { p ->
        ListItem(
            headlineContent = { Text(p.question, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    "\"${p.outcome}\": Kora ${Polymarket.percent(p.koraProbability)} vs market ${Polymarket.percent(p.marketProbability)} · ${AgentTime.label(p.createdAt).substringBeforeLast(',')}" +
                        when (p.happened) {
                            null -> " · open"
                            true -> " · ✓ happened"
                            false -> " · ✗ didn't happen"
                        }
                )
            },
            trailingContent = { IconButton(onClick = { vm.deletePrediction(p.id) }) { Icon(Icons.Default.Close, contentDescription = "Delete") } }
        )
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun MarketsLoading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MarketsError(message: String, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

/** Opens only Polymarket or https links; never crashes without a browser. */
private fun androidx.compose.ui.platform.UriHandler.openSafely(url: String) {
    if (!url.startsWith("https://")) return
    try {
        openUri(url)
    } catch (_: Exception) {
    }
}

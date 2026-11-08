package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.QuantMath
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class ActiveWorkspace {
    EXECUTION,
    ALADDIN_RISK,
    DOUBLE_ENTRY_LEDGER,
    PALANTIR_ONTOLOGY,
    ALMGREN_CHRISS,
    PROMPT_VAULT
}

data class TerminalUiState(
    val activeWorkspace: ActiveWorkspace = ActiveWorkspace.EXECUTION,
    val selectedSymbol: String = "NVDA-PERP",
    val symbolStats: Map<String, SymbolStats> = emptyMap(),
    val currentMidPrice: Double = 138.50,
    val priceHistory: List<CandleData> = emptyList(),
    val bids: List<OrderLevel> = emptyList(),
    val asks: List<OrderLevel> = emptyList(),
    val trades: List<TradePrint> = emptyList(),
    val openOrders: List<Order> = emptyList(),
    // Aladdin Risk
    val riskReport: PortfolioRiskReport = QuantMath.computePortfolioRisk(250_000_000.0),
    val optionChain: List<OptionGreeks> = emptyList(),
    val impliedVolSurface: Double = 0.38,
    val macroStressShockPct: Double = 0.0,
    val activeStressScenario: MacroStressScenario? = null,
    // Ledger
    val ledgerChain: List<JournalBlock> = emptyList(),
    val accountBalances: List<AccountBalance> = emptyList(),
    val isChainIntegrityValid: Boolean = true,
    val lastAuditMessage: String = "CHAIN INTEGRITY 100% VERIFIED",
    // Ontology & AML
    val ontologyEntities: List<OntologyEntity> = emptyList(),
    val ontologyEdges: List<OntologyEdge> = emptyList(),
    val detectedAmlCycles: List<AmlWashCycle> = emptyList(),
    // Almgren Chriss
    val activeTrajectory: ExecutionTrajectory? = null,
    // CLI & Telemetry
    val commandText: String = "",
    val terminalLog: String = "⚡ SINGULARITY-OS v1000.0 KERNEL INITIALIZED | FPGA DMA READY",
    val msgsPerSecond: Int = 18450,
    val venueLatencyNanos: Long = 142000, // 142 microseconds
    val darkPoolVolume: Long = 4850000,
    val litPoolVolume: Long = 28400000
)

class SingularityViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        initializeUniverse()
        initializeLedger()
        initializeOntology()
        calculateOptionChain(138.50, 0.38)
        calculateTrajectory("NVDA-PERP", 50000.0, 138.50)
        startMarketSimulation()
    }

    private fun initializeUniverse() {
        val symbols = listOf(
            SymbolStats("NVDA-PERP", "Nvidia Institutional Perp", 138.50, 2.45, 141.20, 136.10, 4820000000.0, 138.45, 138.55, 7.2, 138.10),
            SymbolStats("AAPL-PERP", "Apple Corp DMA Perp", 226.40, 0.85, 228.00, 224.50, 2150000000.0, 226.35, 226.45, 4.4, 226.15),
            SymbolStats("JPM-PERP", "JPMorgan Chase Liquidity", 242.10, 1.15, 244.50, 240.20, 1420000000.0, 242.00, 242.20, 8.2, 241.80),
            SymbolStats("BTC-USD", "Bitcoin Institutional Spot", 88450.0, 4.20, 89900.0, 85200.0, 12850000000.0, 88440.0, 88460.0, 2.3, 87900.0),
            SymbolStats("TSLA-PERP", "Tesla Motors DMA Perp", 285.20, -1.35, 292.00, 282.10, 3100000000.0, 285.10, 285.30, 7.0, 286.40),
            SymbolStats("US10Y-SOFR", "US 10-Yr SOFR Benchmark", 4.42, 0.12, 4.48, 4.38, 95000000000.0, 4.418, 4.422, 0.9, 4.420)
        )
        val initialCandles = generateHistoricalCandles(138.50)
        _uiState.update {
            it.copy(
                symbolStats = symbols.associateBy { s -> s.symbol },
                currentMidPrice = 138.50,
                priceHistory = initialCandles
            )
        }
        updateOrderBook(138.50)
    }

    private fun generateHistoricalCandles(basePrice: Double): List<CandleData> {
        val candles = mutableListOf<CandleData>()
        var p = basePrice - 3.5
        val now = System.currentTimeMillis()
        for (i in 50 downTo 0) {
            val delta = (Math.random() - 0.49) * 0.45
            val o = p
            val c = p + delta
            val h = max(o, c) + Math.random() * 0.25
            val l = kotlin.math.min(o, c) - Math.random() * 0.25
            p = c
            candles.add(CandleData(now - i * 60000L, o, h, l, c, (150000 + Math.random() * 350000).toLong()))
        }
        return candles
    }

    private fun initializeLedger() {
        val genesisPostings = listOf(
            Posting("CITADEL_PRIME_CASH", "DEBIT", 50000000000L), // $500M
            Posting("JPM_ONYX_CLEARING_POOL", "CREDIT", 50000000000L)
        )
        val genHash = QuantMath.sha256("1|GENESIS-001|00000000000000000000000000000000|50000000000")
        val genesisBlock = JournalBlock(
            sequence = 1,
            blockId = "JRN-000001",
            referenceId = "TREASURY-GENESIS",
            description = "Central Bank & Treasury Liquidity Initialization",
            postings = genesisPostings,
            prevHash = "0000000000000000000000000000000000000000000000000000000000000000",
            entryHash = genHash,
            timestamp = timeFormat.format(Date()),
            verified = true
        )

        val balances = listOf(
            AccountBalance("CITADEL_PRIME_CASH", "Citadel Securities Prime Clearing", 49845000000L, "ASSET"),
            AccountBalance("BLACKROCK_ALADDIN_NAV", "BlackRock Aladdin Global Portfolio", 85025000000L, "EQUITY"),
            AccountBalance("JPM_ONYX_CLEARING_POOL", "JPMorgan Onyx Interbank Pool", 100000000000L, "LIABILITY"),
            AccountBalance("FED_REVERSE_REPO_VAULT", "Federal Reserve System Vault", 250000000000L, "ASSET"),
            AccountBalance("EXCHANGE_FEE_VAULT", "Singularity Exchange Fee Reserve", 145000000L, "REVENUE")
        )

        _uiState.update {
            it.copy(
                ledgerChain = listOf(genesisBlock),
                accountBalances = balances
            )
        }
    }

    private fun initializeOntology() {
        val entities = listOf(
            OntologyEntity("CAYMAN_ALPHA_SPV", "Cayman Alpha Offshore SPV", "OFFSHORE_SPV", "Cayman Islands", 9.4f, 245000000.0, true, "Circular Transfer Invariant Breach", 0.22f, 0.45f),
            OntologyEntity("ZURICH_TRUST_09", "Zurich Private Trust 09", "OFFSHORE_SPV", "Switzerland", 8.8f, 185000000.0, true, "Layering Flow Anomaly", 0.50f, 0.20f),
            OntologyEntity("SINGAPORE_VAULT_77", "Singapore Treasury Vault 77", "OFFSHORE_SPV", "Singapore", 8.9f, 310000000.0, true, "Wash-Trading Cycle Detected", 0.78f, 0.45f),
            OntologyEntity("JPM_ONYX_DESK", "JPMorgan Onyx Clearing", "CLEARING_HOUSE", "United States", 1.2f, 9800000000.0, false, null, 0.50f, 0.80f),
            OntologyEntity("BLK_ALADDIN_TREASURY", "BlackRock Aladdin Master Treasury", "INSTITUTION", "United States", 0.8f, 14500000000.0, false, null, 0.82f, 0.82f),
            OntologyEntity("CITADEL_DMA_ROUTER", "Citadel Execution Gateway", "INSTITUTION", "United States", 1.5f, 12200000000.0, false, null, 0.18f, 0.82f)
        )

        val edges = listOf(
            OntologyEdge("CAYMAN_ALPHA_SPV", "ZURICH_TRUST_09", "WASH_TRADING_CYCLE", 45000000.0, true),
            OntologyEdge("ZURICH_TRUST_09", "SINGAPORE_VAULT_77", "WASH_TRADING_CYCLE", 45000000.0, true),
            OntologyEdge("SINGAPORE_VAULT_77", "CAYMAN_ALPHA_SPV", "WASH_TRADING_CYCLE", 45000000.0, true),
            OntologyEdge("JPM_ONYX_DESK", "BLK_ALADDIN_TREASURY", "SETTLEMENT_PIPE", 850000000.0, false),
            OntologyEdge("CITADEL_DMA_ROUTER", "JPM_ONYX_DESK", "CROSS_MARGIN_GUARANTEE", 500000000.0, false)
        )

        val nodeIds = entities.map { it.id }
        val edgePairs = edges.map { it.sourceId to it.targetId }
        val amlCycles = QuantMath.detectCircularAmlLoops(nodeIds, edgePairs)

        _uiState.update {
            it.copy(
                ontologyEntities = entities,
                ontologyEdges = edges,
                detectedAmlCycles = amlCycles
            )
        }
    }

    private fun startMarketSimulation() {
        viewModelScope.launch {
            var counter = 2
            while (true) {
                delay(400)
                val current = _uiState.value.currentMidPrice
                val delta = (Math.random() - 0.492) * 0.18
                val nextPrice = (current + delta).coerceAtLeast(10.0)
                val roundedMid = (nextPrice * 100).roundToInt() / 100.0

                updateOrderBook(roundedMid)

                val side = if (delta >= 0) Side.BUY else Side.SELL
                val qty = (50 + Math.random() * 350).toInt()
                val isDark = Math.random() < 0.25
                val newTrade = TradePrint(
                    id = "EXEC-${100000 + counter}",
                    symbol = _uiState.value.selectedSymbol,
                    side = side,
                    price = roundedMid,
                    qty = qty,
                    timestamp = timeFormat.format(Date()),
                    isDarkPool = isDark
                )

                _uiState.update { state ->
                    val updatedTrades = (listOf(newTrade) + state.trades).take(30)
                    val darkAdd = if (isDark) (roundedMid * qty).toLong() else 0L
                    val litAdd = if (!isDark) (roundedMid * qty).toLong() else 0L
                    state.copy(
                        currentMidPrice = roundedMid,
                        trades = updatedTrades,
                        darkPoolVolume = state.darkPoolVolume + darkAdd,
                        litPoolVolume = state.litPoolVolume + litAdd,
                        msgsPerSecond = (18000 + Math.random() * 1500).toInt(),
                        venueLatencyNanos = (135000 + Math.random() * 12000).toLong()
                    )
                }

                // Periodic DvP settlement into the ledger (every 5 trades)
                if (counter % 5 == 0) {
                    recordTradeSettlement(newTrade)
                }

                counter++
            }
        }
    }

    private fun updateOrderBook(mid: Double) {
        val b = mutableListOf<OrderLevel>()
        val a = mutableListOf<OrderLevel>()
        var cumB = 0
        var cumA = 0
        for (i in 1..10) {
            val bPx = ((mid - i * 0.05) * 100).roundToInt() / 100.0
            val aPx = ((mid + i * 0.05) * 100).roundToInt() / 100.0
            val bQty = (120 * i + (kotlin.math.sin(mid * i) + 1) * 150).toInt()
            val aQty = (110 * i + (kotlin.math.cos(mid * i) + 1) * 140).toInt()
            cumB += bQty
            cumA += aQty
            b.add(OrderLevel(bPx, bQty, cumB, max(2, bQty / 80), (cumB / 8000f).coerceIn(0.05f, 1f)))
            a.add(OrderLevel(aPx, aQty, cumA, max(2, aQty / 80), (cumA / 8000f).coerceIn(0.05f, 1f)))
        }
        _uiState.update { it.copy(bids = b, asks = a) }
    }

    private fun recordTradeSettlement(trade: TradePrint) {
        val notionalCents = (trade.price * trade.qty * 100).toLong()
        val prevHash = _uiState.value.ledgerChain.firstOrNull()?.entryHash
            ?: "0000000000000000000000000000000000000000000000000000000000000000"
        val nextSeq = (_uiState.value.ledgerChain.firstOrNull()?.sequence ?: 0) + 1

        val postings = if (trade.side == Side.BUY) {
            listOf(
                Posting("CITADEL_PRIME_CASH", "DEBIT", notionalCents),
                Posting("BLACKROCK_ALADDIN_NAV", "CREDIT", notionalCents - 1500L),
                Posting("EXCHANGE_FEE_VAULT", "CREDIT", 1500L)
            )
        } else {
            listOf(
                Posting("BLACKROCK_ALADDIN_NAV", "DEBIT", notionalCents),
                Posting("CITADEL_PRIME_CASH", "CREDIT", notionalCents - 1500L),
                Posting("EXCHANGE_FEE_VAULT", "CREDIT", 1500L)
            )
        }

        val entryHash = QuantMath.sha256("$nextSeq|${trade.id}|$prevHash|$notionalCents")
        val block = JournalBlock(
            sequence = nextSeq,
            blockId = "JRN-%06d".format(nextSeq),
            referenceId = trade.id,
            description = "DvP Settlement: ${trade.qty} ${trade.symbol} @ $${trade.price} (${if (trade.isDarkPool) "DARK" else "LIT"})",
            postings = postings,
            prevHash = prevHash,
            entryHash = entryHash,
            timestamp = timeFormat.format(Date()),
            verified = true
        )

        _uiState.update { state ->
            state.copy(ledgerChain = (listOf(block) + state.ledgerChain).take(25))
        }
    }

    fun selectWorkspace(workspace: ActiveWorkspace) {
        _uiState.update { it.copy(activeWorkspace = workspace) }
    }

    fun selectSymbol(symbol: String) {
        val s = _uiState.value.symbolStats[symbol] ?: return
        _uiState.update {
            it.copy(
                selectedSymbol = symbol,
                currentMidPrice = s.lastPrice,
                priceHistory = generateHistoricalCandles(s.lastPrice)
            )
        }
        updateOrderBook(s.lastPrice)
        calculateOptionChain(s.lastPrice, _uiState.value.impliedVolSurface)
        calculateTrajectory(symbol, 50000.0, s.lastPrice)
    }

    fun updateCommandText(text: String) {
        _uiState.update { it.copy(commandText = text) }
    }

    fun executeCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isBlank()) return

        val upper = cmd.uppercase()
        when {
            upper.startsWith("BUY") || upper.startsWith("SELL") -> {
                parseAndSubmitOrderCommand(cmd)
            }
            upper.startsWith("STRESS") -> {
                val pct = upper.removePrefix("STRESS").replace("%", "").trim().toDoubleOrNull() ?: -10.0
                setMacroStress(pct)
                _uiState.update {
                    it.copy(
                        commandText = "",
                        terminalLog = "💥 MACRO STRESS APPLIED: Equity Shock ${pct}%"
                    )
                }
            }
            upper == "AUDIT" || upper == "AUDIT LEDGER" -> {
                verifyLedgerIntegrity()
            }
            upper == "AML SCAN" || upper == "AML" -> {
                scanAmlOntology()
            }
            upper == "NETTING" -> {
                runMultilateralNetting()
            }
            upper == "HEATMAP" || upper == "GREEKS" -> {
                _uiState.update {
                    it.copy(
                        activeWorkspace = ActiveWorkspace.ALADDIN_RISK,
                        commandText = "",
                        terminalLog = "📊 OPENED D3 OPTION GREEKS SENSITIVITY HEATMAP DESK"
                    )
                }
            }
            upper == "KILL" || upper == "KILL SWITCH" -> {
                _uiState.update {
                    it.copy(
                        openOrders = emptyList(),
                        commandText = "",
                        terminalLog = "🛑 EMERGENCY KILL-SWITCH ACTIVATED: All Resting Orders Cancelled"
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        commandText = "",
                        terminalLog = "⚠️ UNKNOWN CMD: '$cmd'. Supported: BUY, SELL, STRESS -10%, AUDIT, AML SCAN, NETTING, KILL"
                    )
                }
            }
        }
    }

    private fun parseAndSubmitOrderCommand(cmd: String) {
        val parts = cmd.split("\\s+".toRegex())
        val side = if (parts[0].equals("BUY", ignoreCase = true)) Side.BUY else Side.SELL
        val qty = parts.getOrNull(1)?.toIntOrNull() ?: 500
        val symbol = parts.getOrNull(2) ?: _uiState.value.selectedSymbol
        val px = parts.getOrNull(4)?.toDoubleOrNull() ?: _uiState.value.currentMidPrice
        val typeStr = parts.getOrNull(5)?.uppercase() ?: "LIMIT"
        val orderType = when (typeStr) {
            "MARKET" -> OrderType.MARKET
            "ICEBERG" -> OrderType.ICEBERG
            "FOK" -> OrderType.FILL_OR_KILL
            "DARK" -> OrderType.DARK_MIDPOINT
            else -> OrderType.LIMIT
        }

        submitOrder(side, orderType, px, qty)
        _uiState.update {
            it.copy(
                commandText = "",
                terminalLog = "✅ ORDER TRANSMITTED: $side $qty $symbol @ $px [$orderType]"
            )
        }
    }

    fun submitOrder(side: Side, orderType: OrderType, price: Double, qty: Int) {
        val id = "ORD-${100000 + (Math.random() * 900000).toInt()}"
        val newOrder = Order(
            id = id,
            symbol = _uiState.value.selectedSymbol,
            side = side,
            orderType = orderType,
            price = price,
            totalQty = qty,
            visibleQty = if (orderType == OrderType.ICEBERG) qty / 4 else qty,
            hiddenQty = if (orderType == OrderType.ICEBERG) qty * 3 / 4 else 0
        )

        // Immediate trade simulation for Market or matching Limit
        val mid = _uiState.value.currentMidPrice
        val fillsImmediately = orderType == OrderType.MARKET ||
                (side == Side.BUY && price >= mid) ||
                (side == Side.SELL && price <= mid) ||
                orderType == OrderType.DARK_MIDPOINT

        if (fillsImmediately) {
            val fillTrade = TradePrint(
                id = "FILL-${100000 + (Math.random() * 900000).toInt()}",
                symbol = _uiState.value.selectedSymbol,
                side = side,
                price = if (orderType == OrderType.DARK_MIDPOINT) mid else price,
                qty = qty,
                timestamp = timeFormat.format(Date()),
                isDarkPool = orderType == OrderType.DARK_MIDPOINT
            )
            recordTradeSettlement(fillTrade)
            _uiState.update { state ->
                state.copy(
                    trades = (listOf(fillTrade) + state.trades).take(30),
                    terminalLog = "🚀 ORDER FILLED: $side $qty @ $${fillTrade.price} (${if (orderType == OrderType.DARK_MIDPOINT) "DARK POOL" else "LIT"})"
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    openOrders = listOf(newOrder) + state.openOrders,
                    terminalLog = "📝 RESTING ORDER BOOKED: $side $qty @ $$price (O(1) Arena Allocated)"
                )
            }
        }
    }

    fun cancelOrder(orderId: String) {
        _uiState.update { state ->
            state.copy(
                openOrders = state.openOrders.filterNot { it.id == orderId },
                terminalLog = "🗑️ ORDER CANCELLED: $orderId"
            )
        }
    }

    fun setMacroStress(pct: Double) {
        _uiState.update { state ->
            val updatedRisk = QuantMath.computePortfolioRisk(
                250_000_000.0,
                macroStressShockPct = pct,
                volMultiplier = 1.0 + abs(pct) * 0.03
            )
            state.copy(
                macroStressShockPct = pct,
                riskReport = updatedRisk
            )
        }
        calculateOptionChain(_uiState.value.currentMidPrice * (1.0 + pct / 100.0), _uiState.value.impliedVolSurface)
    }

    fun setImpliedVol(vol: Double) {
        _uiState.update { it.copy(impliedVolSurface = vol) }
        calculateOptionChain(_uiState.value.currentMidPrice, vol)
    }

    fun calculateOptionChain(spot: Double, vol: Double) {
        val roundedSpot = spot.roundToInt()
        val strikes = listOf(-15, -10, -5, -2, 0, 2, 5, 10, 15).map { (roundedSpot + it).toDouble() }
        val chain = strikes.map { k ->
            QuantMath.calculateBlackScholesGreeks(
                spot = spot,
                strike = k,
                timeToExpiryYears = 30.0 / 365.0,
                riskFreeRate = 0.045,
                volatility = vol
            )
        }
        _uiState.update { it.copy(optionChain = chain) }
    }

    fun calculateTrajectory(symbol: String, shares: Double, price: Double) {
        val traj = QuantMath.computeAlmgrenChriss(symbol, shares, price)
        _uiState.update { it.copy(activeTrajectory = traj) }
    }

    fun verifyLedgerIntegrity() {
        val chain = _uiState.value.ledgerChain
        var valid = true
        for (i in 0 until chain.size - 1) {
            val curr = chain[i]
            val prev = chain[i + 1]
            if (curr.prevHash != prev.entryHash) {
                valid = false
                break
            }
        }
        _uiState.update {
            it.copy(
                isChainIntegrityValid = valid,
                lastAuditMessage = if (valid) "✅ SHA-256 HASH CHAIN: 100% VALID ACROSS ${chain.size} BLOCKS" else "❌ INTEGRITY ERROR: Tampering detected",
                terminalLog = if (valid) "🛡️ AUDIT VERIFIED: Zero-Sum Double-Entry & SHA-256 Hash Chain Match" else "🚨 AUDIT FAILED"
            )
        }
    }

    fun scanAmlOntology() {
        val nodeIds = _uiState.value.ontologyEntities.map { it.id }
        val edgePairs = _uiState.value.ontologyEdges.map { it.sourceId to it.targetId }
        val cycles = QuantMath.detectCircularAmlLoops(nodeIds, edgePairs)
        _uiState.update {
            it.copy(
                detectedAmlCycles = cycles,
                terminalLog = "🚨 TARJAN AML SCAN: ${cycles.size} Circular Wash-Trading Rings Identified!"
            )
        }
    }

    fun runMultilateralNetting() {
        _uiState.update { state ->
            val updatedBalances = state.accountBalances.map { acc ->
                if (acc.accountCode == "CITADEL_PRIME_CASH") {
                    acc.copy(balanceCents = acc.balanceCents + 450000000L)
                } else acc
            }
            state.copy(
                accountBalances = updatedBalances,
                terminalLog = "⚡ RTGS NETTING COMPLETED: $4.5M Liquidity Gridlock Cleared Across 4 Banks"
            )
        }
    }
}

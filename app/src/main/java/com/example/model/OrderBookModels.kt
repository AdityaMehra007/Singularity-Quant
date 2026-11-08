package com.example.model

enum class Side {
    BUY,
    SELL
}

enum class OrderType(val displayName: String) {
    LIMIT("LIMIT (FIFO)"),
    MARKET("MARKET"),
    ICEBERG("ICEBERG (HIDDEN)"),
    FILL_OR_KILL("FILL-OR-KILL (FOK)"),
    DARK_MIDPOINT("DARK MIDPOINT PEG")
}

data class Order(
    val id: String,
    val symbol: String,
    val side: Side,
    val orderType: OrderType,
    val price: Double,
    val totalQty: Int,
    val visibleQty: Int,
    val hiddenQty: Int = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val status: String = "OPEN"
)

data class TradePrint(
    val id: String,
    val symbol: String,
    val side: Side,
    val price: Double,
    val qty: Int,
    val timestamp: String,
    val isDarkPool: Boolean = false
)

data class OrderLevel(
    val price: Double,
    val qty: Int,
    val total: Int,
    val orders: Int,
    val depthPct: Float = 0f
)

data class CandleData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class SymbolStats(
    val symbol: String,
    val name: String,
    val lastPrice: Double,
    val changePct: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val bestBid: Double,
    val bestAsk: Double,
    val spreadBps: Double,
    val vwap: Double
)

package com.example.engine.options

import kotlin.math.roundToInt

/**
 * Institutional derivatives engine interface and implementation that produces analytical
 * Option Greeks and generates 2D sensitivity grids across strike prices, expiration horizons,
 * and market dynamics.
 */
object OptionGreeksEngine {

    /**
     * Computes the complete Black-Scholes Greeks package for a single contract.
     */
    fun calculateGreeks(
        spotPrice: Double,
        strikePrice: Double,
        daysToExpiry: Double,
        volatility: Double = 0.35,
        riskFreeRate: Double = 0.045,
        dividendYield: Double = 0.0
    ): BlackScholesGreeks {
        return BlackScholesGreeksCalculator.calculateForCurrentMarket(
            spot = spotPrice,
            strike = strikePrice,
            daysToExpiry = daysToExpiry,
            riskFreeRate = riskFreeRate,
            volatility = volatility,
            dividendYield = dividendYield
        )
    }

    /**
     * Generates standard institutional strike price steps centered around the current spot price.
     * E.g. -15%, -10%, -5%, ATM (0%), +5%, +10%, +15%.
     */
    fun generateStandardStrikes(spotPrice: Double, stepCount: Int = 7, rangePct: Double = 0.15): List<Double> {
        val half = (stepCount - 1) / 2
        val step = rangePct / half
        return (-half..half).map { i ->
            val factor = 1.0 + (i * step)
            val raw = spotPrice * factor
            (raw * 100.0).roundToInt() / 100.0
        }
    }

    /**
     * Standard institutional expiration tenors in calendar days.
     * 7D (Weekly), 14D (Bi-weekly), 30D (Front-month), 60D (Quarterly), 90D, 180D (Semi-annual).
     */
    val DEFAULT_EXPIRATIONS_DAYS: List<Double> = listOf(7.0, 14.0, 30.0, 60.0, 90.0, 180.0)

    /**
     * Generates a complete 2D sensitivity grid varying across Strike Prices (X-axis)
     * and Expirations / Days to Expiry (Y-axis).
     */
    fun computeStrikeExpirySensitivityGrid(
        spotPrice: Double,
        strikePrices: List<Double> = generateStandardStrikes(spotPrice),
        expirationDaysList: List<Double> = DEFAULT_EXPIRATIONS_DAYS,
        volatility: Double = 0.35,
        riskFreeRate: Double = 0.045,
        dividendYield: Double = 0.0
    ): List<StrikeExpiryCell> {
        val cells = ArrayList<StrikeExpiryCell>(strikePrices.size * expirationDaysList.size)
        for (expiry in expirationDaysList) {
            val expiryLabel = when {
                expiry < 30.0 -> "${expiry.toInt()}D"
                expiry == 30.0 -> "1M (30D)"
                expiry == 60.0 -> "2M (60D)"
                expiry == 90.0 -> "3M (90D)"
                expiry == 180.0 -> "6M (180D)"
                expiry == 365.0 -> "1Y (365D)"
                else -> "${expiry.toInt()}D"
            }
            for (strike in strikePrices) {
                val greeks = calculateGreeks(
                    spotPrice = spotPrice,
                    strikePrice = strike,
                    daysToExpiry = expiry,
                    volatility = volatility,
                    riskFreeRate = riskFreeRate,
                    dividendYield = dividendYield
                )
                val moneynessPct = ((spotPrice / strike - 1.0) * 100.0)
                val strikeLabel = "$${"%.1f".format(strike)}"

                cells.add(
                    StrikeExpiryCell(
                        strike = strike,
                        daysToExpiry = expiry,
                        strikeLabel = strikeLabel,
                        expiryLabel = expiryLabel,
                        moneynessPct = moneynessPct,
                        greeks = greeks
                    )
                )
            }
        }
        return cells
    }
}

/**
 * Data model for a cell in the Strike vs Expiration sensitivity grid.
 */
data class StrikeExpiryCell(
    val strike: Double,
    val daysToExpiry: Double,
    val strikeLabel: String,
    val expiryLabel: String,
    val moneynessPct: Double,
    val greeks: BlackScholesGreeks
)

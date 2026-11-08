package com.example.engine.options

import kotlin.math.max

/**
 * Encapsulates the complete set of analytical Greeks (Delta, Gamma, Theta, Vega, Rho)
 * and theoretical pricing for both Call and Put contracts derived from the Black-Scholes model.
 */
data class BlackScholesGreeks(
    val inputs: OptionPricingInputs,

    // Core Black-Scholes intermediate variables
    val d1: Double,
    val d2: Double,
    val nd1: Double,
    val nCdfD1: Double,
    val nCdfD2: Double,

    // Theoretical Option Prices
    val callPrice: Double,
    val putPrice: Double,

    // Intrinsic and Extrinsic (Time) Values
    val callIntrinsicValue: Double,
    val callTimeValue: Double,
    val putIntrinsicValue: Double,
    val putTimeValue: Double,

    // ==========================================
    // 1st & 2nd ORDER GREEKS (CALL)
    // ==========================================
    /** Call Delta (Δ): Rate of change of Call price with respect to underlying spot price (0.0 to 1.0) */
    val callDelta: Double,

    /** Call Gamma (Γ): Rate of change of Delta with respect to underlying spot price (identical for Put) */
    val callGamma: Double,

    /** Call Theta Annual (Θ): Annual decay of Call price with respect to passage of time */
    val callThetaAnnual: Double,

    /** Call Theta Daily (Θ/365): 1-day time decay of Call price */
    val callThetaDaily: Double,

    /** Call Vega (ν): Sensitivity of Call price to a 1% (0.01) change in volatility (identical for Put) */
    val callVega: Double,

    /** Call Rho (ρ): Sensitivity of Call price to a 1% (0.01) change in risk-free interest rate */
    val callRho: Double,

    // ==========================================
    // 1st & 2nd ORDER GREEKS (PUT)
    // ==========================================
    /** Put Delta (Δ): Rate of change of Put price with respect to underlying spot price (-1.0 to 0.0) */
    val putDelta: Double,

    /** Put Gamma (Γ): Identical to Call Gamma */
    val putGamma: Double,

    /** Put Theta Annual (Θ): Annual decay of Put price with respect to passage of time */
    val putThetaAnnual: Double,

    /** Put Theta Daily (Θ/365): 1-day time decay of Put price */
    val putThetaDaily: Double,

    /** Put Vega (ν): Identical to Call Vega */
    val putVega: Double,

    /** Put Rho (ρ): Sensitivity of Put price to a 1% (0.01) change in risk-free interest rate */
    val putRho: Double,

    // ==========================================
    // INSTITUTIONAL HIGHER-ORDER CROSS-GREEKS
    // ==========================================
    /** Vanna: dDelta/dVol = dVega/dSpot (sensitivity of Delta to volatility changes) */
    val vanna: Double,

    /** Volga (Vomma): dVega/dVol (convexity of Vega with respect to volatility) */
    val volga: Double,

    /** Charm: -dDelta/dTime (Delta bleed / rate of change of Delta over time) */
    val charm: Double,

    /** Speed: dGamma/dSpot (rate of change of Gamma with respect to spot price) */
    val speed: Double
) {
    /** Moneyness label: ITM, ATM, OTM */
    val callMoneyness: String
        get() = when {
            inputs.isAtm -> "ATM"
            inputs.isCallItm -> "ITM"
            else -> "OTM"
        }

    val putMoneyness: String
        get() = when {
            inputs.isAtm -> "ATM"
            inputs.isPutItm -> "ITM"
            else -> "OTM"
        }

    /**
     * Taylor-series second-order P&L attribution estimation for a Call contract.
     *
     * @param deltaSpot Absolute change in underlying spot price ($)
     * @param deltaVolPct Change in implied volatility in percentage points (e.g. +2.0 for +2% vol)
     * @param daysElapsed Days forward elapsed (time decay)
     * @param deltaRatePct Change in interest rate in percentage points (e.g. +0.50 for +50 bps)
     */
    fun estimateCallPnl(
        deltaSpot: Double,
        deltaVolPct: Double = 0.0,
        daysElapsed: Double = 0.0,
        deltaRatePct: Double = 0.0
    ): GreekPnlAttribution {
        val deltaPnl = callDelta * deltaSpot
        val gammaPnl = 0.5 * callGamma * deltaSpot * deltaSpot
        val vegaPnl = callVega * deltaVolPct
        val thetaPnl = callThetaDaily * daysElapsed
        val rhoPnl = callRho * deltaRatePct
        val total = deltaPnl + gammaPnl + vegaPnl + thetaPnl + rhoPnl

        return GreekPnlAttribution(
            deltaPnl = deltaPnl,
            gammaPnl = gammaPnl,
            vegaPnl = vegaPnl,
            thetaPnl = thetaPnl,
            rhoPnl = rhoPnl,
            totalPnl = total,
            projectedOptionPrice = max(0.0, callPrice + total)
        )
    }

    /**
     * Taylor-series second-order P&L attribution estimation for a Put contract.
     */
    fun estimatePutPnl(
        deltaSpot: Double,
        deltaVolPct: Double = 0.0,
        daysElapsed: Double = 0.0,
        deltaRatePct: Double = 0.0
    ): GreekPnlAttribution {
        val deltaPnl = putDelta * deltaSpot
        val gammaPnl = 0.5 * putGamma * deltaSpot * deltaSpot
        val vegaPnl = putVega * deltaVolPct
        val thetaPnl = putThetaDaily * daysElapsed
        val rhoPnl = putRho * deltaRatePct
        val total = deltaPnl + gammaPnl + vegaPnl + thetaPnl + rhoPnl

        return GreekPnlAttribution(
            deltaPnl = deltaPnl,
            gammaPnl = gammaPnl,
            vegaPnl = vegaPnl,
            thetaPnl = thetaPnl,
            rhoPnl = rhoPnl,
            totalPnl = total,
            projectedOptionPrice = max(0.0, putPrice + total)
        )
    }
}

/**
 * Breakdown of estimated option position P&L by Greek sensitivity.
 */
data class GreekPnlAttribution(
    val deltaPnl: Double,
    val gammaPnl: Double,
    val vegaPnl: Double,
    val thetaPnl: Double,
    val rhoPnl: Double,
    val totalPnl: Double,
    val projectedOptionPrice: Double
)

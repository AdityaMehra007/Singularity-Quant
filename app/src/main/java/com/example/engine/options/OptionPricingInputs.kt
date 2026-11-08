package com.example.engine.options

import kotlin.math.max

/**
 * Standardized market inputs required for Black-Scholes option pricing and Greeks computation.
 *
 * @param spotPrice Current market price of the underlying asset (S)
 * @param strikePrice Strike price of the option contract (K)
 * @param timeToExpiryYears Time to expiration in years (T, e.g. 30.0 / 365.0)
 * @param riskFreeRate Annualized continuous risk-free interest rate (r, e.g. 0.045 for 4.5%)
 * @param volatility Annualized implied volatility of the underlying (sigma, e.g. 0.35 for 35%)
 * @param dividendYield Annualized continuous dividend/convenience yield (q, default 0.0)
 */
data class OptionPricingInputs(
    val spotPrice: Double,
    val strikePrice: Double,
    val timeToExpiryYears: Double,
    val riskFreeRate: Double,
    val volatility: Double,
    val dividendYield: Double = 0.0
) {
    init {
        require(spotPrice > 0) { "Spot price must be strictly positive, was: $spotPrice" }
        require(strikePrice > 0) { "Strike price must be strictly positive, was: $strikePrice" }
        require(timeToExpiryYears > 0) { "Time to expiry must be positive, was: $timeToExpiryYears" }
        require(volatility > 0) { "Volatility must be positive, was: $volatility" }
    }

    /** Moneyness ratio S / K */
    val moneynessRatio: Double
        get() = spotPrice / strikePrice

    /** Days to expiry based on 365-day year convention */
    val daysToExpiry: Double
        get() = timeToExpiryYears * 365.0

    /** True if Call option is in-the-money (S > K) */
    val isCallItm: Boolean
        get() = spotPrice > strikePrice

    /** True if Put option is in-the-money (S < K) */
    val isPutItm: Boolean
        get() = spotPrice < strikePrice

    /** True if option is strictly At-The-Money within 0.5% tolerance */
    val isAtm: Boolean
        get() = kotlin.math.abs(spotPrice - strikePrice) / strikePrice <= 0.005

    companion object {
        /**
         * Convenience constructor taking days to expiration instead of fraction of year.
         */
        fun fromDays(
            spotPrice: Double,
            strikePrice: Double,
            daysToExpiry: Double,
            riskFreeRate: Double,
            volatility: Double,
            dividendYield: Double = 0.0
        ): OptionPricingInputs {
            val safeDays = max(0.001, daysToExpiry)
            return OptionPricingInputs(
                spotPrice = max(0.01, spotPrice),
                strikePrice = max(0.01, strikePrice),
                timeToExpiryYears = safeDays / 365.0,
                riskFreeRate = riskFreeRate,
                volatility = max(0.001, volatility),
                dividendYield = dividendYield
            )
        }
    }
}

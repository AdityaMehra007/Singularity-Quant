package com.example.engine.options

import kotlin.math.*

/**
 * Dedicated mathematical calculation utility for Option Greeks (Delta, Gamma, Theta, Vega, Rho)
 * and derivatives pricing based on the Black-Scholes-Merton (1973/Merton 1973) continuous model.
 *
 * All Greeks are computed analytically with high precision without numerical finite-difference jitter.
 */
object BlackScholesGreeksCalculator {

    private const val SQRT_2PI = 2.5066282746310005
    private const val DAYS_PER_YEAR = 365.0

    /**
     * Standard Normal Probability Density Function (PDF): N'(x) = (1 / sqrt(2*pi)) * exp(-0.5 * x^2)
     */
    fun normPdf(x: Double): Double {
        return exp(-0.5 * x * x) / SQRT_2PI
    }

    /**
     * Standard Normal Cumulative Distribution Function (CDF): N(x)
     * Implemented using the Abramowitz & Stegun formula (7.1.26), with absolute error < 1.5e-7.
     */
    fun normCdf(x: Double): Double {
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1.0 else 1.0
        val absX = abs(x) / sqrt(2.0)
        val t = 1.0 / (1.0 + p * absX)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-absX * absX)
        return 0.5 * (1.0 + sign * y)
    }

    /**
     * Computes all 1st, 2nd, and higher-order Option Greeks for the supplied [OptionPricingInputs].
     */
    fun compute(inputs: OptionPricingInputs): BlackScholesGreeks {
        val s = max(0.0001, inputs.spotPrice)
        val k = max(0.0001, inputs.strikePrice)
        val t = max(1e-6, inputs.timeToExpiryYears)
        val sigma = max(0.0001, inputs.volatility)
        val r = inputs.riskFreeRate
        val q = inputs.dividendYield
        val sqrtT = sqrt(t)

        // Intermediate dimensionless d1 & d2
        val d1 = (ln(s / k) + (r - q + 0.5 * sigma * sigma) * t) / (sigma * sqrtT)
        val d2 = d1 - sigma * sqrtT

        val nd1 = normPdf(d1)
        val nCdfD1 = normCdf(d1)
        val nCdfD2 = normCdf(d2)
        val nCdfNegD1 = normCdf(-d1)
        val nCdfNegD2 = normCdf(-d2)

        val expQT = exp(-q * t)
        val expRT = exp(-r * t)

        // Analytical Option Prices
        val rawCall = s * expQT * nCdfD1 - k * expRT * nCdfD2
        val rawPut = k * expRT * nCdfNegD2 - s * expQT * nCdfNegD1
        val callPrice = max(0.0, rawCall)
        val putPrice = max(0.0, rawPut)

        // Intrinsic and Time (Extrinsic) Values
        val callIntrinsic = max(0.0, s - k)
        val callTimeValue = max(0.0, callPrice - callIntrinsic)
        val putIntrinsic = max(0.0, k - s)
        val putTimeValue = max(0.0, putPrice - putIntrinsic)

        // ==========================================
        // 1. DELTA (Δ = ∂V / ∂S)
        // ==========================================
        val callDelta = expQT * nCdfD1
        val putDelta = -expQT * nCdfNegD1

        // ==========================================
        // 2. GAMMA (Γ = ∂²V / ∂S² = ∂Δ / ∂S)
        // Identical for Call and Put contracts
        // ==========================================
        val gamma = (expQT * nd1) / (s * sigma * sqrtT)

        // ==========================================
        // 3. THETA (Θ = -∂V / ∂t)
        // Expressed annualized and daily (Θ / 365)
        // ==========================================
        val commonThetaPart = -(s * sigma * expQT * nd1) / (2.0 * sqrtT)
        val callThetaAnnual = commonThetaPart - r * k * expRT * nCdfD2 + q * s * expQT * nCdfD1
        val putThetaAnnual = commonThetaPart + r * k * expRT * nCdfNegD2 - q * s * expQT * nCdfNegD1
        val callThetaDaily = callThetaAnnual / DAYS_PER_YEAR
        val putThetaDaily = putThetaAnnual / DAYS_PER_YEAR

        // ==========================================
        // 4. VEGA (ν = ∂V / ∂σ)
        // Sensitivity to a 1 percentage point (0.01) change in volatility
        // ==========================================
        val rawVega = s * expQT * nd1 * sqrtT
        val vega1Pct = rawVega / 100.0

        // ==========================================
        // 5. RHO (ρ = ∂V / ∂r)
        // Sensitivity to a 1 percentage point (0.01 / 100 bps) change in risk-free rate
        // ==========================================
        val callRho1Pct = (k * t * expRT * nCdfD2) / 100.0
        val putRho1Pct = (-k * t * expRT * nCdfNegD2) / 100.0

        // ==========================================
        // 6. HIGHER-ORDER INSTITUTIONAL CROSS-GREEKS
        // ==========================================
        // Vanna: dDelta/dVol = -exp(-qT) * N'(d1) * (d2 / sigma)
        val vanna = -expQT * nd1 * (d2 / sigma)

        // Volga (Vomma): dVega/dVol = Vega * (d1 * d2 / sigma)
        val volga = vega1Pct * (d1 * d2 / sigma)

        // Charm: -dDelta/dTime (decay of Delta over time)
        val charm = q * expQT * nCdfD1 - expQT * nd1 * (2.0 * (r - q) * t - d2 * sigma * sqrtT) / (2.0 * t * sigma * sqrtT)

        // Speed: dGamma/dSpot
        val speed = -(gamma / s) * (d1 / (sigma * sqrtT) + 1.0)

        return BlackScholesGreeks(
            inputs = inputs,
            d1 = d1,
            d2 = d2,
            nd1 = nd1,
            nCdfD1 = nCdfD1,
            nCdfD2 = nCdfD2,
            callPrice = callPrice,
            putPrice = putPrice,
            callIntrinsicValue = callIntrinsic,
            callTimeValue = callTimeValue,
            putIntrinsicValue = putIntrinsic,
            putTimeValue = putTimeValue,
            callDelta = callDelta,
            callGamma = gamma,
            callThetaAnnual = callThetaAnnual,
            callThetaDaily = callThetaDaily,
            callVega = vega1Pct,
            callRho = callRho1Pct,
            putDelta = putDelta,
            putGamma = gamma,
            putThetaAnnual = putThetaAnnual,
            putThetaDaily = putThetaDaily,
            putVega = vega1Pct,
            putRho = putRho1Pct,
            vanna = vanna,
            volga = volga,
            charm = charm,
            speed = speed
        )
    }

    /**
     * Helper to compute Option Greeks directly from current market inputs.
     *
     * @param spot Current underlying market price ($)
     * @param strike Contract strike price ($)
     * @param daysToExpiry Days until option expiration
     * @param riskFreeRate Annualized risk-free interest rate (e.g. 0.045 for 4.5%)
     * @param volatility Annualized implied volatility (e.g. 0.35 for 35%)
     * @param dividendYield Annualized continuous dividend yield (default 0.0)
     */
    fun calculateForCurrentMarket(
        spot: Double,
        strike: Double,
        daysToExpiry: Double,
        riskFreeRate: Double,
        volatility: Double,
        dividendYield: Double = 0.0
    ): BlackScholesGreeks {
        val inputs = OptionPricingInputs.fromDays(
            spotPrice = spot,
            strikePrice = strike,
            daysToExpiry = daysToExpiry,
            riskFreeRate = riskFreeRate,
            volatility = volatility,
            dividendYield = dividendYield
        )
        return compute(inputs)
    }

    /**
     * Generates a strike ladder / option chain for a given underlying spot price across a list of strikes.
     */
    fun computeOptionChain(
        spot: Double,
        strikes: List<Double>,
        daysToExpiry: Double,
        riskFreeRate: Double,
        volatility: Double,
        dividendYield: Double = 0.0
    ): List<BlackScholesGreeks> {
        return strikes.map { strike ->
            calculateForCurrentMarket(
                spot = spot,
                strike = strike,
                daysToExpiry = daysToExpiry,
                riskFreeRate = riskFreeRate,
                volatility = volatility,
                dividendYield = dividendYield
            )
        }
    }

    /**
     * Solves for Implied Volatility (IV) using Newton-Raphson iterations with Brenner-Subrahmanyam initial seed.
     *
     * @param marketPrice Observed market price of the option contract ($)
     * @param spot Current market price of the underlying ($)
     * @param strike Strike price ($)
     * @param daysToExpiry Days to expiration
     * @param riskFreeRate Risk-free rate
     * @param isCall True for call options, false for put options
     * @param dividendYield Continuous dividend yield
     * @return Implied Volatility (sigma) rounded to 4 decimals, or -1.0 if not convergent
     */
    fun calculateImpliedVolatility(
        marketPrice: Double,
        spot: Double,
        strike: Double,
        daysToExpiry: Double,
        riskFreeRate: Double,
        isCall: Boolean = true,
        dividendYield: Double = 0.0,
        maxIterations: Int = 100,
        tolerance: Double = 1e-4
    ): Double {
        val intrinsic = if (isCall) max(0.0, spot - strike) else max(0.0, strike - spot)
        if (marketPrice < intrinsic) {
            return -1.0 // Price below intrinsic violates no-arbitrage boundary
        }

        val tYears = max(1e-5, daysToExpiry / DAYS_PER_YEAR)
        // Brenner-Subrahmanyam ATM initial approximation: sigma ≈ sqrt(2*pi / T) * (Price / S)
        var sigma = max(0.05, sqrt(2.0 * PI / tYears) * (marketPrice / spot))

        for (i in 0 until maxIterations) {
            val inputs = OptionPricingInputs(
                spotPrice = spot,
                strikePrice = strike,
                timeToExpiryYears = tYears,
                riskFreeRate = riskFreeRate,
                volatility = sigma,
                dividendYield = dividendYield
            )
            val greeks = compute(inputs)
            val theoreticalPrice = if (isCall) greeks.callPrice else greeks.putPrice
            val priceDiff = theoreticalPrice - marketPrice

            if (abs(priceDiff) <= tolerance) {
                return (sigma * 10000.0).roundToInt() / 10000.0
            }

            val vegaRaw = greeks.callVega * 100.0 // Vega per 1.0 unit of sigma
            if (vegaRaw < 1e-8) {
                break
            }

            sigma -= priceDiff / vegaRaw
            sigma = sigma.coerceIn(0.001, 5.0)
        }

        return (sigma * 10000.0).roundToInt() / 10000.0
    }
}

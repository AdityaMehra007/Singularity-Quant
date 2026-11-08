package com.example.engine.options

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class BlackScholesGreeksCalculatorTest {

    @Test
    fun testBlackScholesAnalyticalGreeksStandardBenchmark() {
        // Standard textbook benchmark: S = 100, K = 100, T = 1.0 year, r = 0.05 (5%), sigma = 0.20 (20%)
        val inputs = OptionPricingInputs(
            spotPrice = 100.0,
            strikePrice = 100.0,
            timeToExpiryYears = 1.0,
            riskFreeRate = 0.05,
            volatility = 0.20
        )
        val greeks = BlackScholesGreeksCalculator.compute(inputs)

        // 1. Analytical Option Prices
        // Known benchmark: Call ≈ 10.4506, Put ≈ 5.5735
        assertEquals(10.4506, greeks.callPrice, 1e-2)
        assertEquals(5.5735, greeks.putPrice, 1e-2)

        // 2. Put-Call Parity: C - P = S - K * exp(-r * T)
        val expectedParityDiff = 100.0 - 100.0 * exp(-0.05 * 1.0)
        val actualParityDiff = greeks.callPrice - greeks.putPrice
        assertEquals(expectedParityDiff, actualParityDiff, 1e-4)

        // 3. Delta: Call Delta ≈ 0.6368, Put Delta ≈ -0.3632
        // Invariant: Call Delta - Put Delta = exp(-q*T) = 1.0 (when q = 0)
        assertEquals(0.6368, greeks.callDelta, 1e-2)
        assertEquals(-0.3632, greeks.putDelta, 1e-2)
        assertEquals(1.0, greeks.callDelta - greeks.putDelta, 1e-5)

        // 4. Gamma: Identical for Call and Put, positive
        assertTrue(greeks.callGamma > 0.0)
        assertEquals(greeks.callGamma, greeks.putGamma, 1e-9)
        assertEquals(0.01876, greeks.callGamma, 1e-3)

        // 5. Vega: Identical for Call and Put, positive
        assertTrue(greeks.callVega > 0.0)
        assertEquals(greeks.callVega, greeks.putVega, 1e-9)
        assertEquals(0.3752, greeks.callVega, 1e-2)

        // 6. Theta: Negative for long unhedged vanilla calls/puts
        assertTrue(greeks.callThetaDaily < 0.0)
        assertTrue(greeks.putThetaDaily < 0.0)
        assertEquals(greeks.callThetaDaily * 365.0, greeks.callThetaAnnual, 1e-5)

        // 7. Rho: Positive for Calls, Negative for Puts
        assertTrue(greeks.callRho > 0.0)
        assertTrue(greeks.putRho < 0.0)
        assertEquals(0.5323, greeks.callRho, 1e-2)
        assertEquals(-0.4189, greeks.putRho, 1e-2)
    }

    @Test
    fun testDeepInTheMoneyAndOutOfTheMoneyBoundaries() {
        // Deep ITM Call (S = 250, K = 100) -> Delta should approach 1.0
        val deepItm = BlackScholesGreeksCalculator.calculateForCurrentMarket(
            spot = 250.0,
            strike = 100.0,
            daysToExpiry = 30.0,
            riskFreeRate = 0.05,
            volatility = 0.20
        )
        assertTrue(deepItm.callDelta > 0.999)
        assertEquals(0.0, deepItm.putDelta, 1e-2)
        assertTrue(deepItm.callPrice >= 150.0) // S - K = 150

        // Deep OTM Call (S = 40, K = 100) -> Delta should approach 0.0
        val deepOtm = BlackScholesGreeksCalculator.calculateForCurrentMarket(
            spot = 40.0,
            strike = 100.0,
            daysToExpiry = 30.0,
            riskFreeRate = 0.05,
            volatility = 0.20
        )
        assertTrue(deepOtm.callDelta < 0.001)
        assertTrue(deepOtm.putDelta < -0.999)
        assertEquals(0.0, deepOtm.callPrice, 1e-2)
    }

    @Test
    fun testOptionChainCalculation() {
        val strikes = listOf(130.0, 135.0, 140.0, 145.0, 150.0)
        val chain = BlackScholesGreeksCalculator.computeOptionChain(
            spot = 138.50,
            strikes = strikes,
            daysToExpiry = 30.0,
            riskFreeRate = 0.045,
            volatility = 0.35
        )

        assertEquals(5, chain.size)
        // Delta must be monotonically decreasing with increasing strike for Calls
        for (i in 0 until chain.size - 1) {
            assertTrue(chain[i].callDelta > chain[i + 1].callDelta)
            assertTrue(chain[i].callPrice > chain[i + 1].callPrice)
            assertTrue(chain[i].putPrice < chain[i + 1].putPrice)
        }
    }

    @Test
    fun testNewtonRaphsonImpliedVolatilitySolverRoundTrip() {
        val spot = 138.50
        val strike = 140.00
        val days = 30.0
        val r = 0.045
        val targetVol = 0.38

        val knownGreeks = BlackScholesGreeksCalculator.calculateForCurrentMarket(
            spot = spot,
            strike = strike,
            daysToExpiry = days,
            riskFreeRate = r,
            volatility = targetVol
        )

        val solvedVol = BlackScholesGreeksCalculator.calculateImpliedVolatility(
            marketPrice = knownGreeks.callPrice,
            spot = spot,
            strike = strike,
            daysToExpiry = days,
            riskFreeRate = r,
            isCall = true
        )

        assertEquals(targetVol, solvedVol, 1e-3)
    }

    @Test
    fun testTaylorSeriesGreekPnlAttribution() {
        val greeks = BlackScholesGreeksCalculator.calculateForCurrentMarket(
            spot = 100.0,
            strike = 100.0,
            daysToExpiry = 45.0,
            riskFreeRate = 0.05,
            volatility = 0.25
        )

        // Shock spot by +2.0
        val pnl = greeks.estimateCallPnl(deltaSpot = 2.0, deltaVolPct = 0.0, daysElapsed = 0.0)

        // Linear + Curvature P&L check
        val expectedDeltaPnl = greeks.callDelta * 2.0
        val expectedGammaPnl = 0.5 * greeks.callGamma * 4.0
        assertEquals(expectedDeltaPnl, pnl.deltaPnl, 1e-6)
        assertEquals(expectedGammaPnl, pnl.gammaPnl, 1e-6)
        assertEquals(expectedDeltaPnl + expectedGammaPnl, pnl.totalPnl, 1e-5)
        assertEquals(greeks.callPrice + pnl.totalPnl, pnl.projectedOptionPrice, 1e-5)
    }

    @Test
    fun testD3SensitivityMatrixDeltaAndGammaBehavior() {
        val spotSteps = listOf(85.0, 90.0, 95.0, 100.0, 105.0, 110.0, 115.0)
        val volSteps = listOf(0.15, 0.25, 0.35, 0.45, 0.55)
        val strike = 100.0
        val days = 30.0
        val r = 0.05

        for (vol in volSteps) {
            var prevCallDelta = -1.0
            var prevCallPrice = -1.0
            var atmGamma = 0.0

            spotSteps.forEach { s ->
                val g = BlackScholesGreeksCalculator.calculateForCurrentMarket(
                    spot = s,
                    strike = strike,
                    daysToExpiry = days,
                    riskFreeRate = r,
                    volatility = vol
                )
                // Delta must increase monotonically with spot
                assertTrue(g.callDelta > prevCallDelta)
                prevCallDelta = g.callDelta

                // Call price must increase monotonically with spot
                assertTrue(g.callPrice > prevCallPrice)
                prevCallPrice = g.callPrice

                if (abs(s - strike) < 1.0) {
                    atmGamma = g.callGamma
                }
            }

            // Verify Gamma peaks near ATM compared to deep wings (85 or 115)
            val otmGreeks = BlackScholesGreeksCalculator.calculateForCurrentMarket(
                spot = 85.0,
                strike = strike,
                daysToExpiry = days,
                riskFreeRate = r,
                volatility = vol
            )
            assertTrue(atmGamma > otmGreeks.callGamma)
        }
    }

    @Test
    fun testD3ColorScaleInterpolators() {
        val viridis0 = com.example.ui.components.interpolateD3Viridis(0f)
        val viridis1 = com.example.ui.components.interpolateD3Viridis(1f)
        // Viridis start is dark purple/indigo (low red, low green)
        assertTrue(viridis0.red < 0.5f)
        // Viridis end is bright yellow (high red, high green)
        assertTrue(viridis1.red > 0.8f && viridis1.green > 0.8f)

        val plasma0 = com.example.ui.components.interpolateD3Plasma(0f)
        val plasma1 = com.example.ui.components.interpolateD3Plasma(1f)
        // Plasma start is deep blue/purple (low red, low green)
        assertTrue(plasma0.red < 0.2f && plasma0.green < 0.2f)
        // Plasma end is bright yellow (high red, high green)
        assertTrue(plasma1.red > 0.8f && plasma1.green > 0.8f)
    }

    @Test
    fun testD3ScaleMathematicalInterpolation() {
        // Test scaleLinear
        val scale = com.example.ui.components.D3Scale.scaleLinear(
            domainMin = 100.0,
            domainMax = 200.0,
            rangeMin = 0f,
            rangeMax = 500f
        )
        assertEquals(0f, scale(100.0), 1e-4f)
        assertEquals(250f, scale(150.0), 1e-4f)
        assertEquals(500f, scale(200.0), 1e-4f)
        // Clamping check
        assertEquals(0f, scale(50.0), 1e-4f)
        assertEquals(500f, scale(300.0), 1e-4f)

        // Test D3Scale Viridis & Plasma endpoints
        val vir0 = com.example.ui.components.D3Scale.interpolateViridis(0f)
        val vir1 = com.example.ui.components.D3Scale.interpolateViridis(1f)
        assertTrue(vir0.red < 0.5f)
        assertTrue(vir1.red > 0.8f && vir1.green > 0.8f)

        val plas0 = com.example.ui.components.D3Scale.interpolatePlasma(0f)
        val plas1 = com.example.ui.components.D3Scale.interpolatePlasma(1f)
        assertTrue(plas0.red < 0.2f)
        assertTrue(plas1.red > 0.8f && plas1.green > 0.8f)
    }

    @Test
    fun testOptionGreeksEngineStrikeExpiryGrid() {
        val spot = 138.50
        val strikes = OptionGreeksEngine.generateStandardStrikes(spot, stepCount = 7, rangePct = 0.15)
        assertEquals(7, strikes.size)
        // Middle strike is ATM
        val atmStrike = strikes[3]
        assertEquals(spot, atmStrike, 0.5)

        val grid = OptionGreeksEngine.computeStrikeExpirySensitivityGrid(
            spotPrice = spot,
            strikePrices = strikes,
            expirationDaysList = OptionGreeksEngine.DEFAULT_EXPIRATIONS_DAYS,
            volatility = 0.35,
            riskFreeRate = 0.045
        )

        assertEquals(7 * OptionGreeksEngine.DEFAULT_EXPIRATIONS_DAYS.size, grid.size)

        // Verify Strike monotonically changes Delta: lower strike (ITM call) -> higher Delta
        val frontMonthCells = grid.filter { it.daysToExpiry == 30.0 }
        for (i in 0 until frontMonthCells.size - 1) {
            assertTrue(frontMonthCells[i].greeks.callDelta > frontMonthCells[i + 1].greeks.callDelta)
        }

        // Verify Gamma peaks near ATM (strike index 3)
        val atmCell = frontMonthCells.find { it.strike == atmStrike }!!
        val otmWingCell = frontMonthCells.last()
        assertTrue(atmCell.greeks.callGamma > otmWingCell.greeks.callGamma)
    }
}




package com.example

import com.example.engine.QuantMath
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class ExampleUnitTest {

    @Test
    fun testBlackScholesPutCallParity() {
        val spot = 138.50
        val strike = 140.00
        val t = 30.0 / 365.0
        val r = 0.045
        val sigma = 0.35

        val greeks = QuantMath.calculateBlackScholesGreeks(spot, strike, t, r, sigma)
        val call = greeks.callPrice
        val put = greeks.putPrice

        // Put-Call Parity: C - P = S - K * exp(-r * T)
        val expectedDiff = spot - strike * exp(-r * t)
        val actualDiff = call - put

        assertEquals(expectedDiff, actualDiff, 1e-2)
        assertTrue(greeks.delta in 0.0..1.0)
        assertTrue(greeks.gamma > 0.0)
        assertTrue(greeks.vega > 0.0)
    }

    @Test
    fun testAlmgrenChrissTrajectoryMonotonicity() {
        val totalShares = 50000.0
        val trajectory = QuantMath.computeAlmgrenChriss("NVDA-PERP", totalShares, 138.50, 60, 6)

        assertEquals(6, trajectory.slices.size)
        // Ensure remaining inventory strictly decreases towards zero
        var prev = totalShares
        trajectory.slices.forEach { s ->
            assertTrue(s.remainingInventory <= prev)
            prev = s.remainingInventory
        }
        assertTrue(trajectory.urgencyKappa > 0.0)
        assertTrue(trajectory.expectedImplementationShortfallUsd > 0.0)
    }

    @Test
    fun testSha256Determinism() {
        val hash1 = QuantMath.sha256("TEST_LEDGER_PAYLOAD_123")
        val hash2 = QuantMath.sha256("TEST_LEDGER_PAYLOAD_123")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testTarjanSccCycleDetection() {
        val nodes = listOf("A", "B", "C", "D")
        val edges = listOf(
            "A" to "B",
            "B" to "C",
            "C" to "A", // cycle A-B-C
            "C" to "D"
        )
        val cycles = QuantMath.detectCircularAmlLoops(nodes, edges)
        assertEquals(1, cycles.size)
        assertTrue(cycles[0].entityIds.containsAll(listOf("A", "B", "C")))
    }
}

package com.example.model

data class OptionGreeks(
    val strike: Double,
    val callPrice: Double,
    val putPrice: Double,
    val delta: Double,
    val gamma: Double,
    val vega: Double,
    val thetaDaily: Double,
    val rho: Double,
    val vanna: Double,
    val volga: Double,
    val impliedVol: Double
)

data class PortfolioRiskReport(
    val navUsd: Double,
    val parametricVar95: Double,
    val parametricVar99: Double,
    val monteCarloVar95: Double,
    val monteCarloVar99: Double,
    val expectedShortfallCvar99: Double,
    val annualizedVolatilityPct: Double,
    val sharpeRatio: Double,
    val betaToSp500: Double,
    val currentLeverage: Double
)

data class MacroStressScenario(
    val id: String,
    val title: String,
    val description: String,
    val equityShockPct: Double,
    val rateShockBps: Double,
    val volShockPct: Double,
    val estimatedLossUsd: Double,
    val capitalImpactPct: Double
)

data class FactorAttribution(
    val factorName: String,
    val exposure: Double,
    val pnlContributionUsd: Double,
    val contributionPct: Double
)

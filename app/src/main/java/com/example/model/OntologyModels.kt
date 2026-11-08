package com.example.model

data class OntologyEntity(
    val id: String,
    val name: String,
    val type: String, // "INSTITUTION", "OFFSHORE_SPV", "CLEARING_HOUSE", "SOVEREIGN_FUND"
    val jurisdiction: String,
    val riskScore: Float, // 0.0 - 10.0
    val totalVolumeUsd: Double,
    val isAlert: Boolean = false,
    val alertReason: String? = null,
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f
)

data class OntologyEdge(
    val sourceId: String,
    val targetId: String,
    val relationship: String, // "WASH_TRADING_CYCLE", "CROSS_MARGIN_GUARANTEE", "SETTLEMENT_PIPE"
    val flowAmountUsd: Double,
    val isSuspicious: Boolean = false
)

data class AmlWashCycle(
    val cycleId: String,
    val entityIds: List<String>,
    val detectedLoopDescription: String,
    val totalLaunderedUsd: Double,
    val severity: String // "CRITICAL", "HIGH", "MEDIUM"
)

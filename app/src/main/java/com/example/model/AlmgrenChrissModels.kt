package com.example.model

data class TrajectorySlice(
    val step: Int,
    val timeLabel: String,
    val remainingInventory: Double,
    val sliceShares: Double,
    val expectedPrice: Double,
    val temporaryImpactBps: Double,
    val status: String // "EXECUTED", "PENDING", "ACTIVE"
)

data class ExecutionTrajectory(
    val symbol: String,
    val totalOrderShares: Double,
    val horizonMinutes: Int,
    val riskAversionLambda: Double,
    val urgencyKappa: Double,
    val expectedImplementationShortfallUsd: Double,
    val executionVarianceStdUsd: Double,
    val slices: List<TrajectorySlice>
)

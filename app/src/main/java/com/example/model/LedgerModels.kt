package com.example.model

data class Posting(
    val accountCode: String,
    val direction: String, // "DEBIT" or "CREDIT"
    val amountCents: Long,
    val currency: String = "USD"
)

data class JournalBlock(
    val sequence: Long,
    val blockId: String,
    val referenceId: String,
    val description: String,
    val postings: List<Posting>,
    val prevHash: String,
    val entryHash: String,
    val timestamp: String,
    val verified: Boolean = true
)

data class AccountBalance(
    val accountCode: String,
    val name: String,
    val balanceCents: Long,
    val type: String, // "ASSET", "LIABILITY", "EQUITY", "REVENUE"
    val currency: String = "USD"
)

data class NettingObligation(
    val fromInstitution: String,
    val toInstitution: String,
    val grossAmountCents: Long,
    val nettedAmountCents: Long,
    val savedLiquidityPct: Double
)

package com.example.model

data class ArchitectureBlueprint(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "MASTER_PROMPT", "MICROSERVICE_RUST", "QUANT_PYTHON", "GATEWAY_GO"
    val content: String
)

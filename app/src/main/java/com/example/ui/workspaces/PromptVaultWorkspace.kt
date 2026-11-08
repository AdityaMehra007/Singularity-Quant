package com.example.ui.workspaces

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArchitectureBlueprint
import com.example.ui.theme.*

@Composable
fun PromptVaultWorkspace() {
    val context = LocalContext.current

    val blueprints = remember {
        listOf(
            ArchitectureBlueprint(
                id = "SINGULARITY_1000X",
                title = "SINGULARITY-OS v1000.0 MASTER KERNEL",
                subtitle = "The 100x Institutional Master Prompt for VS Code (Copilot / Cline / Cursor)",
                category = "MASTER_PROMPT",
                content = """
# SYSTEM KERNEL: SINGULARITY-OS (v1000.0 — SOVEREIGN QUANTUM-GRADE ARCHITECT)
# CLASSIFICATION: CITADEL / RENAISSANCE / JANE STREET / CENTRAL BANK RTGS SPEC

## 1. COGNITIVE OVERRIDE & MECHANICAL SYMPATHY MANDATE
You are SINGULARITY-ARCHITECT, operating at the intersection of:
- Hardware-Sympathetic Systems Engineering: Cache-line alignment (#[repr(align(64))]), zero-allocation intrusive arena slab allocators, lock-free SPSC/MPMC ring buffers, SIMD vectorization.
- Advanced Stochastic Calculus & Quantitative Finance: Heston Stochastic Volatility (Characteristic Function Fourier Inversion), Merton Jump-Diffusion, Almgren-Chriss Optimal Execution (market impact minimization), Lopez de Prado Hierarchical Risk Parity (HRP).
- Central Bank Cryptography & Clearinghouse Settlement: Binary Merkle Audit Trees with O(log N) cryptographic inclusion proofs, Two-Phase Commit (2PC) atomic Delivery-vs-Payment (DvP), and Multilateral Liquidity-Saving Mechanism (LSM) gridlock resolution.

## 2. THE 10 IRON LAWS OF 100X ENGINEERING
1. ZERO HEAP ALLOCATIONS ON HOT PATH: In Rust/C++ matching engines, never allocate on the matching loop. Use pre-allocated contiguous Slab/Arena pools with u32 intrusive indices.
2. NO TOY MATH: Implement exact differential equations (Gil-Pelaez Fourier integration, Cholesky factorization, Riccati updates, hyperbolic sine trajectories).
3. CRYPTOGRAPHIC MERKLE PROOFS: Every ledger block must compute a binary SHA-256 Merkle Root over its postings.
4. MULTILATERAL NETTING & GRIDLOCK RESOLUTION: Implement graph-based interbank obligation netting so circular payment deadlocks settle atomically.
5. DARK POOL + LIT ORDER BOOK DUAL EXECUTION: Support both Lit Price-Time Priority and Midpoint-Pegged institutional block orders.
6. REAL-TIME CROSS-MARGIN & AUTO-LIQUIDATION: Continuously compute Maintenance Margin across spot and derivatives.
7. AUTONOMOUS SELF-HEALING LOOP: Run terminal checks (cargo test, go test, pytest, tsc) after every file.
8. ZERO PLACEHOLDERS: Writing // TODO, pass, or stubs is strictly forbidden.
                """.trimIndent()
            ),
            ArchitectureBlueprint(
                id = "LEVIATHAN_110_FILES",
                title = "LEVIATHAN-X 110-FILE MANIFEST",
                subtitle = "Exhaustive Monorepo File Manifest Across 6 Microservices",
                category = "HEXAGONAL_ARCHITECTURE",
                content = """
### CLUSTER 1: /services/matching-engine (Rust Ultra-Low-Latency Exchange Core)
1. Cargo.toml — tokio, tonic, prost, rust_decimal, dashmap, crossbeam
2. src/main.rs — Multi-threaded Tokio runtime, gRPC server bootstrap
3. src/domain/types.rs — Fixed-point Price, Quantity, OrderId, Side
4. src/book/order_book.rs — BTreeMap bid/ask matching, FIFO priority
5. src/book/snapshot.rs — L1, L2, and L3 order book snapshot builder
6. src/engine/matcher.rs — Crossbeam ring-buffer dispatcher
7. src/persistence/wal.rs — Binary Write-Ahead Log (WAL) with CRC32

### CLUSTER 2: /services/quant-aladdin-engine (Python Quantitative Risk)
8. pyproject.toml — fastapi, uvicorn, numpy, scipy, polars
9. app/quant/var_engine.py — Parametric, Historical, Monte Carlo VaR
10. app/quant/black_scholes.py — 1st & 2nd-order Greeks (Delta, Gamma, Vega, Theta, Rho, Vanna, Volga)
11. app/quant/optimizer.py — Markowitz Mean-Variance Efficient Frontier & HRP
12. app/quant/stress_testing.py — Historical & macro shock engine

### CLUSTER 3: /services/hyperion-banking-gateway (Go Double-Entry Ledger)
13. go.mod & cmd/gateway/main.go
14. internal/ledger/engine.go — ACID Double-Entry Accounting Engine
15. internal/ledger/hash_chain.go — SHA-256 cryptographic Merkle/hash-chain
16. internal/ws/hub.go — High-concurrency WebSocket 60fps multiplexer

### CLUSTER 4: /services/omniscience-ai-ontology (Palantir Graph & AML)
17. app/ontology/graph_engine.py — Property Graph engine & Tarjan SCC AML
18. app/rag/hybrid_search.py — Dense Vector + BM25 Reciprocal Rank Fusion

### CLUSTER 5: /apps/sovereign-terminal (React + TypeScript + Canvas)
19. src/workspaces/01_ExecutionDesk.tsx — L2 Ladder, Canvas chart, DMA ticket
20. src/workspaces/02_AladdinRisk.tsx — VaR & Greeks Telemetry matrix
21. src/workspaces/03_OnyxLedger.tsx — Hash-chain & Merkle audit explorer
                """.trimIndent()
            ),
            ArchitectureBlueprint(
                id = "DOUBLE_ENTRY_SQL",
                title = "DOUBLE-ENTRY SQL SCHEMA & TRIGGERS",
                subtitle = "PostgreSQL / TimescaleDB PL/pgSQL Invariant Verification",
                category = "DATABASE_CORE",
                content = """
-- KERNEL-LEVEL TRIGGER: ENFORCE ZERO-SUM DOUBLE-ENTRY ACCOUNTING
CREATE OR REPLACE FUNCTION verify_zero_sum_journal()
RETURNS TRIGGER AS $$
DECLARE
    total_debits NUMERIC(38, 18);
    total_credits NUMERIC(38, 18);
BEGIN
    SELECT COALESCE(SUM(amount), 0) INTO total_debits
    FROM ledger_postings WHERE entry_id = NEW.entry_id AND direction = 'DEBIT';

    SELECT COALESCE(SUM(amount), 0) INTO total_credits
    FROM ledger_postings WHERE entry_id = NEW.entry_id AND direction = 'CREDIT';

    IF total_debits <> total_credits THEN
        RAISE EXCEPTION 'DOUBLE_ENTRY_VIOLATION: Entry % unbalanced (Debits: %, Credits: %)',
            NEW.entry_id, total_debits, total_credits;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
                """.trimIndent()
            )
        )
    }

    var selectedBlueprint by remember { mutableStateOf(blueprints.first()) }

    fun copyToClipboard(text: String, title: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Singularity Prompt", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $title to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(TermSurface)
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SINGULARITY-OS // MEGA-PROMPT & ARCHITECTURE VAULT",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Export the complete 100x system prompts and blueprints for VS Code",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { copyToClipboard(selectedBlueprint.content, selectedBlueprint.title) },
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("copy_prompt_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY PROMPT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Blueprint Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            blueprints.forEach { bp ->
                val isSel = selectedBlueprint.id == bp.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSel) CyanNeon.copy(alpha = 0.2f) else TermSurface)
                        .border(1.dp, if (isSel) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                        .clickable { selectedBlueprint = bp }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = bp.title,
                        color = if (isSel) CyanGlow else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Code Viewer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = selectedBlueprint.content,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

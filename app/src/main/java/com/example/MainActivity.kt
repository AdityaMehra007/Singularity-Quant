package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CommandPaletteBar
import com.example.ui.components.TopTelemetryBar
import com.example.ui.theme.*
import com.example.ui.workspaces.*
import com.example.viewmodel.ActiveWorkspace
import com.example.viewmodel.SingularityViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SingularityTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = TermBackground
                ) {
                    SingularityTerminalApp()
                }
            }
        }
    }
}

@Composable
fun SingularityTerminalApp(
    viewModel: SingularityViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TermBackground,
        topBar = {
            Column {
                TopTelemetryBar(
                    selectedSymbol = state.selectedSymbol,
                    symbolStats = state.symbolStats,
                    msgsPerSec = state.msgsPerSecond,
                    latencyNanos = state.venueLatencyNanos,
                    darkPoolVolume = state.darkPoolVolume,
                    litPoolVolume = state.litPoolVolume,
                    onSelectSymbol = { viewModel.selectSymbol(it) }
                )
                CommandPaletteBar(
                    commandText = state.commandText,
                    terminalLog = state.terminalLog,
                    onCommandTextChange = { viewModel.updateCommandText(it) },
                    onExecuteCommand = { viewModel.executeCommand(it) }
                )
            }
        },
        bottomBar = {
            InstitutionalBottomNav(
                activeWorkspace = state.activeWorkspace,
                onSelectWorkspace = { viewModel.selectWorkspace(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.activeWorkspace) {
                ActiveWorkspace.EXECUTION -> {
                    ExecutionDeskWorkspace(
                        symbol = state.selectedSymbol,
                        currentPrice = state.currentMidPrice,
                        candles = state.priceHistory,
                        bids = state.bids,
                        asks = state.asks,
                        trades = state.trades,
                        openOrders = state.openOrders,
                        onSubmitOrder = { side, type, px, q -> viewModel.submitOrder(side, type, px, q) },
                        onCancelOrder = { id -> viewModel.cancelOrder(id) }
                    )
                }
                ActiveWorkspace.ALADDIN_RISK -> {
                    AladdinRiskWorkspace(
                        riskReport = state.riskReport,
                        optionChain = state.optionChain,
                        impliedVolSurface = state.impliedVolSurface,
                        macroStressShockPct = state.macroStressShockPct,
                        currentMarketSpot = state.currentMidPrice,
                        currentSymbol = state.selectedSymbol,
                        onSetStressShock = { viewModel.setMacroStress(it) },
                        onSetImpliedVol = { viewModel.setImpliedVol(it) }
                    )
                }
                ActiveWorkspace.DOUBLE_ENTRY_LEDGER -> {
                    DoubleEntryLedgerWorkspace(
                        ledgerChain = state.ledgerChain,
                        accountBalances = state.accountBalances,
                        isChainValid = state.isChainIntegrityValid,
                        auditMessage = state.lastAuditMessage,
                        onVerifyChain = { viewModel.verifyLedgerIntegrity() },
                        onRunNetting = { viewModel.runMultilateralNetting() }
                    )
                }
                ActiveWorkspace.PALANTIR_ONTOLOGY -> {
                    PalantirAmlWorkspace(
                        entities = state.ontologyEntities,
                        edges = state.ontologyEdges,
                        detectedCycles = state.detectedAmlCycles,
                        onTriggerScan = { viewModel.scanAmlOntology() }
                    )
                }
                ActiveWorkspace.ALMGREN_CHRISS -> {
                    AlmgrenChrissWorkspace(
                        trajectory = state.activeTrajectory
                    )
                }
                ActiveWorkspace.PROMPT_VAULT -> {
                    PromptVaultWorkspace()
                }
            }
        }
    }
}

data class NavItem(
    val workspace: ActiveWorkspace,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun InstitutionalBottomNav(
    activeWorkspace: ActiveWorkspace,
    onSelectWorkspace: (ActiveWorkspace) -> Unit
) {
    val items = listOf(
        NavItem(ActiveWorkspace.EXECUTION, "[F1] EXECUTION", Icons.Default.ShowChart, "tab_execution"),
        NavItem(ActiveWorkspace.ALADDIN_RISK, "[F2] ALADDIN RISK", Icons.Default.Shield, "tab_aladdin"),
        NavItem(ActiveWorkspace.DOUBLE_ENTRY_LEDGER, "[F3] ONYX LEDGER", Icons.Default.AccountBalance, "tab_ledger"),
        NavItem(ActiveWorkspace.PALANTIR_ONTOLOGY, "[F4] PALANTIR AML", Icons.Default.Hub, "tab_ontology"),
        NavItem(ActiveWorkspace.ALMGREN_CHRISS, "[F5] ALMGREN-CHRISS", Icons.Default.TrendingUp, "tab_almgren"),
        NavItem(ActiveWorkspace.PROMPT_VAULT, "[F6] PROMPT VAULT", Icons.Default.Code, "tab_vault")
    )

    Surface(
        color = TermSurface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(1.dp, TermBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.workspace == activeWorkspace
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) CyanNeon else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectWorkspace(item.workspace) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag(item.testTag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) CyanGlow else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = item.label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

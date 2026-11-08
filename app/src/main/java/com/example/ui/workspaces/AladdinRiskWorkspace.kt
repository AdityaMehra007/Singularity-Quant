package com.example.ui.workspaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OptionGreeks
import com.example.model.PortfolioRiskReport
import com.example.ui.components.OptionGreeksInteractiveCard
import com.example.ui.theme.*

enum class AladdinSubTab {
    GREEKS_CALCULATOR,
    GREEKS_HEATMAP,
    OPTION_CHAIN,
    VaR_STRESS
}

@Composable
fun AladdinRiskWorkspace(
    riskReport: PortfolioRiskReport,
    optionChain: List<OptionGreeks>,
    impliedVolSurface: Double,
    macroStressShockPct: Double,
    currentMarketSpot: Double = 138.50,
    currentSymbol: String = "NVDA-PERP",
    onSetStressShock: (Double) -> Unit,
    onSetImpliedVol: (Double) -> Unit
) {
    var activeSubTab by remember { mutableStateOf(AladdinSubTab.GREEKS_CALCULATOR) }
    var heatmapViewMode by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper Quick Status & Sub-tab Switcher
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(TermSurface)
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BLACKROCK ALADDIN // RISK & DERIVATIVES DESK",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "UNDERLYING: $currentSymbol @ $${"%.2f".format(currentMarketSpot)}",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Sub-tab Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        AladdinSubTab.GREEKS_CALCULATOR to "CALCULATOR",
                        AladdinSubTab.GREEKS_HEATMAP to "D3 HEATMAP",
                        AladdinSubTab.OPTION_CHAIN to "CHAIN (30D)",
                        AladdinSubTab.VaR_STRESS to "VaR & STRESS"
                    ).forEach { (tab, title) ->
                        val isSelected = activeSubTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0xFF090D15))
                                .border(1.dp, if (isSelected) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                                .clickable { activeSubTab = tab }
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) CyanGlow else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Sub-tab Content
        when (activeSubTab) {
            AladdinSubTab.GREEKS_CALCULATOR -> {
                // Dedicated Black-Scholes Greeks Calculator Card
                OptionGreeksInteractiveCard(
                    currentMarketSpot = currentMarketSpot,
                    currentSymbol = currentSymbol,
                    onOpenHeatmap = { activeSubTab = AladdinSubTab.GREEKS_HEATMAP },
                    modifier = Modifier.weight(1f)
                )
            }

            AladdinSubTab.GREEKS_HEATMAP -> {
                // D3-Based Delta and Gamma Sensitivity Heatmap View
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Heatmap Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (heatmapViewMode == 0) CyanNeon.copy(alpha = 0.25f) else Color(0xFF090D15))
                                .border(1.dp, if (heatmapViewMode == 0) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                                .clickable { heatmapViewMode = 0 }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "D3 MATRIX GRID (VIRIDIS/PLASMA)",
                                color = if (heatmapViewMode == 0) CyanGlow else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (heatmapViewMode == 1) PurpleDarkPool.copy(alpha = 0.25f) else Color(0xFF090D15))
                                .border(1.dp, if (heatmapViewMode == 1) PurpleDarkPool else TermBorder, RoundedCornerShape(3.dp))
                                .clickable { heatmapViewMode = 1 }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ADVANCED EXPLORER & SVG",
                                color = if (heatmapViewMode == 1) Color(0xFFD8B4FE) else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (heatmapViewMode == 0) {
                        com.example.ui.components.GreekHeatmapVisualization(
                            modifier = Modifier.weight(1f),
                            spotPrice = currentMarketSpot,
                            impliedVolatility = impliedVolSurface,
                            riskFreeRate = 0.045
                        )
                    } else {
                        com.example.ui.components.OptionGreeksSensitivityHeatmap(
                            currentMarketSpot = currentMarketSpot,
                            currentSymbol = currentSymbol,
                            initialStrike = currentMarketSpot,
                            initialDaysToExpiry = 30.0,
                            initialVol = impliedVolSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AladdinSubTab.OPTION_CHAIN -> {
                // Option Chain Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TermSurface)
                        .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STRIKE LADDER & INSTITUTIONAL GREEKS (T=30D)",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "VOL: ${"%.1f".format(impliedVolSurface * 100)}%",
                                color = GoldWarning,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF070A10))
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("STRIKE", color = CyanGlow, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(48.dp))
                            Text("CALL", color = EmeraldBid, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                            Text("PUT", color = RoseAsk, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                            Text("DELTA", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                            Text("GAMMA", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                            Text("VEGA", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(38.dp))
                            Text("THETA/d", color = GoldWarning, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(optionChain) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0B1019))
                                        .padding(vertical = 3.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$${"%.1f".format(row.strike)}", color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(48.dp))
                                    Text("$${"%.2f".format(row.callPrice)}", color = EmeraldBid, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                                    Text("$${"%.2f".format(row.putPrice)}", color = RoseAsk, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                                    Text("${"%.3f".format(row.delta)}", color = Color.White, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                                    Text("${"%.4f".format(row.gamma)}", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                                    Text("${"%.2f".format(row.vega)}", color = TextSecondary, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(38.dp))
                                    Text("${"%.3f".format(row.thetaDaily)}", color = GoldWarning, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(44.dp))
                                }
                            }
                        }
                    }
                }
            }

            AladdinSubTab.VaR_STRESS -> {
                // VaR & Macro Stress Section
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 4 Metric Gauges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetricBox(
                            title = "INSTITUTIONAL NAV",
                            value = "$${"%.2f".format(riskReport.navUsd / 1_000_000.0)}M",
                            color = EmeraldBid,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "PARAMETRIC VaR 99%",
                            value = "$${"%.2f".format(riskReport.parametricVar99 / 1_000_000.0)}M",
                            color = GoldWarning,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "MONTE CARLO VaR 99%",
                            value = "$${"%.2f".format(riskReport.monteCarloVar99 / 1_000_000.0)}M",
                            color = RoseAsk,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            title = "EXPECTED SHORTFALL",
                            value = "$${"%.2f".format(riskReport.expectedShortfallCvar99 / 1_000_000.0)}M",
                            color = Color(0xFFFF4D4D),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Sliders & Shock Scenarios Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(TermSurface)
                            .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MACRO SHOCK SCENARIO: ${if (macroStressShockPct > 0) "+" else ""}${"%.1f".format(macroStressShockPct)}%",
                                    color = if (macroStressShockPct < 0) RoseAsk else CyanGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "VOL SIGMA: ${"%.1f".format(impliedVolSurface * 100)}%",
                                    color = GoldWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Slider(
                                    value = macroStressShockPct.toFloat(),
                                    onValueChange = { onSetStressShock(it.toDouble()) },
                                    valueRange = -30f..30f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("macro_stress_slider"),
                                    colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = TermBorder)
                                )
                                Slider(
                                    value = impliedVolSurface.toFloat(),
                                    onValueChange = { onSetImpliedVol(it.toDouble()) },
                                    valueRange = 0.10f..1.20f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("implied_vol_slider"),
                                    colors = SliderDefaults.colors(thumbColor = GoldWarning, activeTrackColor = GoldWarning, inactiveTrackColor = TermBorder)
                                )
                            }

                            // Presets
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "BASE (0%)" to 0.0,
                                    "2008 LEHMAN (-25%)" to -25.0,
                                    "2020 COVID (-15%)" to -15.0,
                                    "FED +250BPS (-8%)" to -8.0,
                                    "TECH CRASH (-20%)" to -20.0,
                                    "BULL RALLY (+15%)" to 15.0
                                ).forEach { (label, pct) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (macroStressShockPct == pct) CyanNeon.copy(alpha = 0.25f) else Color(0xFF0F1520))
                                            .border(1.dp, if (macroStressShockPct == pct) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                                            .clickable { onSetStressShock(pct) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (macroStressShockPct == pct) CyanGlow else TextSecondary,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF080C14))
            .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

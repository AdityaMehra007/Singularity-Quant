package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.options.BlackScholesGreeksCalculator
import com.example.engine.options.OptionPricingInputs
import com.example.ui.theme.*
import kotlin.math.abs

enum class OptionSide { CALL, PUT }

@Composable
fun OptionGreeksInteractiveCard(
    currentMarketSpot: Double,
    currentSymbol: String,
    onOpenHeatmap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var spotText by remember(currentMarketSpot) { mutableStateOf(currentMarketSpot.toString()) }
    var strikeText by remember(currentMarketSpot) { mutableStateOf(currentMarketSpot.toString()) }
    var daysText by remember { mutableStateOf("30") }
    var volPctText by remember { mutableStateOf("35") }
    var ratePctText by remember { mutableStateOf("4.5") }
    var divYieldPctText by remember { mutableStateOf("0.0") }
    var optionSide by remember { mutableStateOf(OptionSide.CALL) }

    // Scenario shock states
    var shockSpotPct by remember { mutableStateOf(0.0) }
    var shockVolPct by remember { mutableStateOf(0.0) }
    var shockDaysElapsed by remember { mutableStateOf(0.0) }

    // Parse inputs safely
    val spot = spotText.toDoubleOrNull() ?: currentMarketSpot
    val strike = strikeText.toDoubleOrNull() ?: currentMarketSpot
    val days = (daysText.toDoubleOrNull() ?: 30.0).coerceAtLeast(0.1)
    val vol = ((volPctText.toDoubleOrNull() ?: 35.0) / 100.0).coerceIn(0.01, 5.0)
    val rate = (ratePctText.toDoubleOrNull() ?: 4.5) / 100.0
    val div = (divYieldPctText.toDoubleOrNull() ?: 0.0) / 100.0

    val inputs = remember(spot, strike, days, rate, vol, div) {
        OptionPricingInputs.fromDays(
            spotPrice = spot,
            strikePrice = strike,
            daysToExpiry = days,
            riskFreeRate = rate,
            volatility = vol,
            dividendYield = div
        )
    }

    val greeks = remember(inputs) {
        BlackScholesGreeksCalculator.compute(inputs)
    }

    // Active side metrics
    val price = if (optionSide == OptionSide.CALL) greeks.callPrice else greeks.putPrice
    val delta = if (optionSide == OptionSide.CALL) greeks.callDelta else greeks.putDelta
    val gamma = greeks.callGamma
    val thetaDaily = if (optionSide == OptionSide.CALL) greeks.callThetaDaily else greeks.putThetaDaily
    val vega = greeks.callVega
    val rho = if (optionSide == OptionSide.CALL) greeks.callRho else greeks.putRho
    val moneyness = if (optionSide == OptionSide.CALL) greeks.callMoneyness else greeks.putMoneyness
    val intrinsic = if (optionSide == OptionSide.CALL) greeks.callIntrinsicValue else greeks.putIntrinsicValue
    val timeValue = if (optionSide == OptionSide.CALL) greeks.callTimeValue else greeks.putTimeValue

    // P&L scenario estimation
    val deltaSpotDollar = spot * (shockSpotPct / 100.0)
    val scenarioPnl = remember(greeks, optionSide, deltaSpotDollar, shockVolPct, shockDaysElapsed) {
        if (optionSide == OptionSide.CALL) {
            greeks.estimateCallPnl(
                deltaSpot = deltaSpotDollar,
                deltaVolPct = shockVolPct,
                daysElapsed = shockDaysElapsed
            )
        } else {
            greeks.estimatePutPnl(
                deltaSpot = deltaSpotDollar,
                deltaVolPct = shockVolPct,
                daysElapsed = shockDaysElapsed
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("option_greeks_card"),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = TermSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TermBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Greeks Engine",
                        tint = CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "BLACK-SCHOLES OPTION GREEKS ENGINE",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onOpenHeatmap != null) {
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(CyanNeon.copy(alpha = 0.2f))
                                .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                .clickable { onOpenHeatmap() }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "D3 HEATMAP",
                                color = CyanGlow,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Call / Put Toggle
                    Row(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(if (optionSide == OptionSide.CALL) EmeraldBid else Color(0xFF0C111A))
                                .clickable { optionSide = OptionSide.CALL }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CALL",
                                color = if (optionSide == OptionSide.CALL) Color.Black else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(if (optionSide == OptionSide.PUT) RoseAsk else Color(0xFF0C111A))
                                .clickable { optionSide = OptionSide.PUT }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PUT",
                                color = if (optionSide == OptionSide.PUT) Color.White else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Market Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InputField(
                    label = "SPOT (S)",
                    value = spotText,
                    onValueChange = { spotText = it },
                    modifier = Modifier.weight(1.1f),
                    trailingAction = {
                        IconButton(
                            onClick = {
                                spotText = currentMarketSpot.toString()
                                strikeText = currentMarketSpot.toString()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Spot", tint = CyanNeon, modifier = Modifier.size(12.dp))
                        }
                    }
                )
                InputField(
                    label = "STRIKE (K)",
                    value = strikeText,
                    onValueChange = { strikeText = it },
                    modifier = Modifier.weight(1f)
                )
                InputField(
                    label = "DAYS (T)",
                    value = daysText,
                    onValueChange = { daysText = it },
                    modifier = Modifier.weight(0.8f)
                )
                InputField(
                    label = "VOL % (σ)",
                    value = volPctText,
                    onValueChange = { volPctText = it },
                    modifier = Modifier.weight(0.9f)
                )
                InputField(
                    label = "RATE % (r)",
                    value = ratePctText,
                    onValueChange = { ratePctText = it },
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Quick DTE Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(7, 14, 30, 45, 60, 90, 180, 365).forEach { d ->
                    val isSel = days.toInt() == d
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSel) CyanNeon.copy(alpha = 0.2f) else Color(0xFF090D15))
                            .border(1.dp, if (isSel) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                            .clickable { daysText = d.toString() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${d}D",
                            color = if (isSel) CyanGlow else TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Theoretical Price Banner & Moneyness
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF080C14))
                    .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${optionSide.name} THEORETICAL PRICE:",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$${"%.3f".format(price)}",
                        color = if (optionSide == OptionSide.CALL) EmeraldBid else RoseAsk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "INTRINSIC: $${"%.2f".format(intrinsic)}",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TIME: $${"%.2f".format(timeValue)}",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when (moneyness) {
                                    "ITM" -> EmeraldBid.copy(alpha = 0.2f)
                                    "ATM" -> CyanNeon.copy(alpha = 0.2f)
                                    else -> Color(0xFF222938)
                                }
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = moneyness,
                            color = when (moneyness) {
                                "ITM" -> EmeraldLight
                                "ATM" -> CyanGlow
                                else -> TextMuted
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // The 5 Core Greeks Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GreekCard(
                    symbol = "DELTA (Δ)",
                    value = "%.4f".format(delta),
                    subtext = "∂V/∂S (Hedge Ratio)",
                    color = if (delta >= 0) EmeraldBid else RoseAsk,
                    modifier = Modifier.weight(1f)
                )
                GreekCard(
                    symbol = "GAMMA (Γ)",
                    value = "%.5f".format(gamma),
                    subtext = "∂²V/∂S² (Curvature)",
                    color = CyanGlow,
                    modifier = Modifier.weight(1f)
                )
                GreekCard(
                    symbol = "THETA (Θ/d)",
                    value = "$${"%.4f".format(thetaDaily)}",
                    subtext = "1-Day Time Decay",
                    color = GoldWarning,
                    modifier = Modifier.weight(1f)
                )
                GreekCard(
                    symbol = "VEGA (ν)",
                    value = "$${"%.4f".format(vega)}",
                    subtext = "Per +1% Volatility",
                    color = PurpleDarkPool,
                    modifier = Modifier.weight(1f)
                )
                GreekCard(
                    symbol = "RHO (ρ)",
                    value = "$${"%.4f".format(rho)}",
                    subtext = "Per +1% Rate (100bps)",
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Higher-Order Greeks Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF06090F))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VANNA: ${"%.5f".format(greeks.vanna)} (dΔ/dσ)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("VOLGA: ${"%.5f".format(greeks.volga)} (dν/dσ)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("CHARM: ${"%.5f".format(greeks.charm)} (Delta bleed)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text("SPEED: ${"%.5f".format(greeks.speed)} (dΓ/dS)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }

            // Real-Time P&L Scenario Simulator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF090E18))
                    .border(0.5.dp, TermBorder, RoundedCornerShape(3.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GREEK P&L SCENARIO ATTRIBUTION",
                            color = CyanNeon,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PROJECTED VALUE: $${"%.2f".format(scenarioPnl.projectedOptionPrice)}", color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "EST. P&L: ${if (scenarioPnl.totalPnl >= 0) "+" else ""}${"%.2f".format(scenarioPnl.totalPnl)}",
                                color = if (scenarioPnl.totalPnl >= 0) EmeraldBid else RoseAsk,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Quick Shock Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "SPOT +3%" to { shockSpotPct = 3.0 },
                            "SPOT -3%" to { shockSpotPct = -3.0 },
                            "VOL +5%" to { shockVolPct = 5.0 },
                            "VOL -5%" to { shockVolPct = -5.0 },
                            "T+5 DAYS" to { shockDaysElapsed = 5.0 },
                            "RESET" to {
                                shockSpotPct = 0.0
                                shockVolPct = 0.0
                                shockDaysElapsed = 0.0
                            }
                        ).forEach { (label, action) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (label == "RESET") Color(0xFF1E293B) else Color(0xFF121A28))
                                    .clickable { action() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(label, color = if (label == "RESET") Color.White else CyanGlow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // P&L Component Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Δ PnL: $${"%.2f".format(scenarioPnl.deltaPnl)}", color = if (scenarioPnl.deltaPnl >= 0) EmeraldBid else RoseAsk, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("Γ PnL: $${"%.2f".format(scenarioPnl.gammaPnl)}", color = EmeraldBid, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("ν PnL: $${"%.2f".format(scenarioPnl.vegaPnl)}", color = if (scenarioPnl.vegaPnl >= 0) PurpleDarkPool else RoseAsk, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("Θ PnL: $${"%.2f".format(scenarioPnl.thetaPnl)}", color = if (scenarioPnl.thetaPnl >= 0) EmeraldBid else RoseAsk, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun GreekCard(
    symbol: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF090D15))
            .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Column {
            Text(symbol, color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text(value, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(subtext, color = TextMuted, fontSize = 6.5.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(label, color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF06090F))
                .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(CyanNeon)
            )
            trailingAction?.invoke()
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.options.BlackScholesGreeks
import com.example.engine.options.OptionGreeksEngine
import com.example.engine.options.StrikeExpiryCell
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Metric selection for Option Greek sensitivity heatmaps.
 */
enum class GreekHeatmapMetric {
    DELTA,
    GAMMA
}

/**
 * Contract option side selection.
 */
enum class GreekContractSide {
    CALL,
    PUT
}

/**
 * Mathematical scale and color interpolation engine for D3-style heatmaps.
 */
object D3Scale {
    /**
     * Continuous linear scale mapping domain [domainMin, domainMax] to range [rangeMin, rangeMax].
     */
    fun scaleLinear(domainMin: Double, domainMax: Double, rangeMin: Float, rangeMax: Float): (Double) -> Float {
        val span = max(1e-9, domainMax - domainMin)
        val rangeSpan = rangeMax - rangeMin
        return { value ->
            val norm = ((value - domainMin) / span).coerceIn(0.0, 1.0)
            (rangeMin + norm * rangeSpan).toFloat()
        }
    }

    /**
     * D3 Viridis Palette: Deep Indigo -> Blue -> Teal -> Green -> Yellow (Ideal for Delta)
     */
    fun interpolateViridis(t: Float): Color {
        val c = t.coerceIn(0f, 1f)
        return when {
            c < 0.25f -> lerpColor(Color(0xFF440154), Color(0xFF3B528B), c / 0.25f)
            c < 0.50f -> lerpColor(Color(0xFF3B528B), Color(0xFF21908C), (c - 0.25f) / 0.25f)
            c < 0.75f -> lerpColor(Color(0xFF21908C), Color(0xFF5DC863), (c - 0.50f) / 0.25f)
            else -> lerpColor(Color(0xFF5DC863), Color(0xFFFDE725), (c - 0.75f) / 0.25f)
        }
    }

    /**
     * D3 Plasma Palette: Deep Navy Blue -> Violet/Magenta -> Red-Orange -> Bright Yellow (Ideal for Gamma)
     */
    fun interpolatePlasma(t: Float): Color {
        val c = t.coerceIn(0f, 1f)
        return when {
            c < 0.25f -> lerpColor(Color(0xFF0D0887), Color(0xFF6A00A8), c / 0.25f)
            c < 0.50f -> lerpColor(Color(0xFF6A00A8), Color(0xFFB12A90), (c - 0.25f) / 0.25f)
            c < 0.75f -> lerpColor(Color(0xFFB12A90), Color(0xFFE16462), (c - 0.50f) / 0.25f)
            else -> lerpColor(Color(0xFFE16462), Color(0xFFF0F921), (c - 0.75f) / 0.25f)
        }
    }

    private fun lerpColor(c1: Color, c2: Color, f: Float): Color {
        val fraction = f.coerceIn(0f, 1f)
        return Color(
            red = c1.red + (c2.red - c1.red) * fraction,
            green = c1.green + (c2.green - c1.green) * fraction,
            blue = c1.blue + (c2.blue - c1.blue) * fraction,
            alpha = 1f
        )
    }
}

/**
 * Composable function `GreekHeatmapVisualization` that consumes the [OptionGreeksEngine]
 * to render an interactive sensitivity heatmap grid, visualizing Delta (Δ) and Gamma (Γ)
 * variations across strike prices (X-axis) and expirations (Y-axis).
 *
 * @param spotPrice Current underlying asset market price ($S$)
 * @param strikePrices Optional custom strike prices. If null, standard 7-strike ladder around spot is generated.
 * @param expirationDaysList Optional custom expiration tenors in days.
 * @param impliedVolatility Underlying volatility parameter ($\sigma$), defaults to 35% (0.35)
 * @param riskFreeRate Annualized benchmark interest rate ($r$), defaults to 4.5% (0.045)
 * @param dividendYield Annualized continuous dividend yield ($q$), defaults to 0.0%
 * @param onCellSelected Optional callback when a grid cell is selected for inspection
 */
@Composable
fun GreekHeatmapVisualization(
    modifier: Modifier = Modifier,
    spotPrice: Double = 138.50,
    strikePrices: List<Double>? = null,
    expirationDaysList: List<Double>? = null,
    impliedVolatility: Double = 0.35,
    riskFreeRate: Double = 0.045,
    dividendYield: Double = 0.0,
    initialMetric: GreekHeatmapMetric = GreekHeatmapMetric.DELTA,
    initialContractSide: GreekContractSide = GreekContractSide.CALL,
    onCellSelected: ((StrikeExpiryCell) -> Unit)? = null
) {
    var activeMetric by remember { mutableStateOf(initialMetric) }
    var activeSide by remember { mutableStateOf(initialContractSide) }
    var selectedCell by remember { mutableStateOf<StrikeExpiryCell?>(null) }

    // Adjustable volatility parameter for live dynamic stress testing
    var liveVol by remember(impliedVolatility) { mutableStateOf(impliedVolatility) }

    // Resolve strike prices (X-axis) and expirations (Y-axis)
    val strikes = remember(spotPrice, strikePrices) {
        strikePrices ?: OptionGreeksEngine.generateStandardStrikes(spotPrice, stepCount = 7, rangePct = 0.15)
    }

    val expirations = remember(expirationDaysList) {
        expirationDaysList ?: OptionGreeksEngine.DEFAULT_EXPIRATIONS_DAYS
    }

    // Compute sensitivity matrix using OptionGreeksEngine
    val gridCells = remember(spotPrice, strikes, expirations, liveVol, riskFreeRate, dividendYield) {
        OptionGreeksEngine.computeStrikeExpirySensitivityGrid(
            spotPrice = spotPrice,
            strikePrices = strikes,
            expirationDaysList = expirations,
            volatility = liveVol,
            riskFreeRate = riskFreeRate,
            dividendYield = dividendYield
        )
    }

    // Metric bounds for color normalization
    val (domainMin, domainMax) = remember(gridCells, activeMetric, activeSide) {
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE
        gridCells.forEach { cell ->
            val v = when (activeMetric) {
                GreekHeatmapMetric.DELTA -> if (activeSide == GreekContractSide.CALL) cell.greeks.callDelta else cell.greeks.putDelta
                GreekHeatmapMetric.GAMMA -> cell.greeks.callGamma
            }
            minVal = min(minVal, v)
            maxVal = max(maxVal, v)
        }
        Pair(minVal, maxVal)
    }

    // Default select ATM 30D cell if none selected
    LaunchedEffect(gridCells) {
        if (selectedCell == null && gridCells.isNotEmpty()) {
            val atmStrike = strikes.minByOrNull { abs(it - spotPrice) } ?: strikes[strikes.size / 2]
            val defaultExpiry = expirations.find { it == 30.0 } ?: expirations[expirations.size / 2]
            selectedCell = gridCells.find { it.strike == atmStrike && it.daysToExpiry == defaultExpiry }
                ?: gridCells[gridCells.size / 2]
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(TermSurface)
            .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
            .padding(8.dp)
            .testTag("greek_heatmap_visualization"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header & Metric Controls Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title & Mode Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (activeMetric == GreekHeatmapMetric.DELTA) Color(0xFF0F3042) else Color(0xFF38153A))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STRIKE × EXPIRY",
                        color = if (activeMetric == GreekHeatmapMetric.DELTA) CyanGlow else Color(0xFFF472B6),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${activeMetric.name} SENSITIVITY GRID",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Metric (Delta / Gamma) & Contract (Call / Put) Toggles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call / Put Toggle
                Row(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (activeSide == GreekContractSide.CALL) EmeraldBid else Color(0xFF090D15))
                            .clickable { activeSide = GreekContractSide.CALL }
                            .padding(horizontal = 6.dp)
                            .testTag("call_side_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CALL",
                            color = if (activeSide == GreekContractSide.CALL) Color.Black else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (activeSide == GreekContractSide.PUT) RoseAsk else Color(0xFF090D15))
                            .clickable { activeSide = GreekContractSide.PUT }
                            .padding(horizontal = 6.dp)
                            .testTag("put_side_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "PUT",
                            color = if (activeSide == GreekContractSide.PUT) Color.White else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Delta / Gamma Toggle
                Row(
                    modifier = Modifier
                        .height(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (activeMetric == GreekHeatmapMetric.DELTA) CyanNeon.copy(alpha = 0.25f) else Color(0xFF090D15))
                            .clickable { activeMetric = GreekHeatmapMetric.DELTA }
                            .padding(horizontal = 6.dp)
                            .testTag("delta_metric_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "DELTA (Δ)",
                            color = if (activeMetric == GreekHeatmapMetric.DELTA) CyanGlow else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (activeMetric == GreekHeatmapMetric.GAMMA) PurpleDarkPool.copy(alpha = 0.25f) else Color(0xFF090D15))
                            .clickable { activeMetric = GreekHeatmapMetric.GAMMA }
                            .padding(horizontal = 6.dp)
                            .testTag("gamma_metric_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "GAMMA (Γ)",
                            color = if (activeMetric == GreekHeatmapMetric.GAMMA) Color(0xFFD8B4FE) else TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Subtitle Info Bar: Spot, Reference Volatility, and Quick Vol Shocks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF06090F))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SPOT $${"%.2f".format(spotPrice)}",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "VOL: ${(liveVol * 100).toInt()}%",
                    color = GoldWarning,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "RATE: ${(riskFreeRate * 100).toInt()}bps",
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Volatility shock selectors
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(0.20, 0.35, 0.50).forEach { volOption ->
                    val isCur = abs(liveVol - volOption) < 0.01
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isCur) CyanNeon.copy(alpha = 0.25f) else Color(0xFF101726))
                            .clickable { liveVol = volOption }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${(volOption * 100).toInt()}%σ",
                            color = if (isCur) CyanGlow else TextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2D Interactive Canvas Grid Matrix
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF05080E))
                .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
        ) {
            val xCount = strikes.size
            val yCount = expirations.size
            val yLabelWidth = 36.dp
            val xLabelHeight = 18.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Y-Axis Labels (Expirations: Shortest at bottom or top)
                // Shortest expiry (7D) at top, longest (180D) at bottom
                Column(
                    modifier = Modifier
                        .width(yLabelWidth)
                        .fillMaxHeight()
                        .padding(bottom = xLabelHeight),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    expirations.forEach { exp ->
                        val label = when {
                            exp < 30.0 -> "${exp.toInt()}D"
                            exp == 30.0 -> "30D"
                            exp == 60.0 -> "60D"
                            exp == 90.0 -> "90D"
                            exp == 180.0 -> "180D"
                            else -> "${exp.toInt()}D"
                        }
                        Text(
                            text = label,
                            color = TextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }

                // Main Heatmap Matrix Canvas
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cW = w / xCount
                            val cH = h / yCount
                            val domainRange = max(1e-9, domainMax - domainMin)

                            gridCells.forEach { cell ->
                                val col = strikes.indexOf(cell.strike)
                                val row = expirations.indexOf(cell.daysToExpiry)

                                if (col >= 0 && row >= 0) {
                                    val x = col * cW
                                    val y = row * cH

                                    val cellMetricVal = when (activeMetric) {
                                        GreekHeatmapMetric.DELTA -> if (activeSide == GreekContractSide.CALL) cell.greeks.callDelta else cell.greeks.putDelta
                                        GreekHeatmapMetric.GAMMA -> cell.greeks.callGamma
                                    }

                                    val t = ((cellMetricVal - domainMin) / domainRange).toFloat().coerceIn(0f, 1f)
                                    val cellColor = when (activeMetric) {
                                        GreekHeatmapMetric.DELTA -> D3Scale.interpolateViridis(t)
                                        GreekHeatmapMetric.GAMMA -> D3Scale.interpolatePlasma(t)
                                    }

                                    // Draw cell background
                                    drawRoundRect(
                                        color = cellColor,
                                        topLeft = Offset(x + 1f, y + 1f),
                                        size = Size(cW - 2f, cH - 2f),
                                        cornerRadius = CornerRadius(2f, 2f)
                                    )

                                    // Reticle for selected cell
                                    if (selectedCell != null && selectedCell?.strike == cell.strike && selectedCell?.daysToExpiry == cell.daysToExpiry) {
                                        drawRoundRect(
                                            color = Color.White,
                                            topLeft = Offset(x + 1f, y + 1f),
                                            size = Size(cW - 2f, cH - 2f),
                                            cornerRadius = CornerRadius(2f, 2f),
                                            style = Stroke(width = 2.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Touch Interaction Grid Overlay with Value Labels
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (row in 0 until yCount) {
                                val expDays = expirations[row]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    for (col in 0 until xCount) {
                                        val strk = strikes[col]
                                        val cell = gridCells.find { it.strike == strk && it.daysToExpiry == expDays }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    if (cell != null) {
                                                        selectedCell = cell
                                                        onCellSelected?.invoke(cell)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (cell != null) {
                                                val mVal = when (activeMetric) {
                                                    GreekHeatmapMetric.DELTA -> if (activeSide == GreekContractSide.CALL) cell.greeks.callDelta else cell.greeks.putDelta
                                                    GreekHeatmapMetric.GAMMA -> cell.greeks.callGamma
                                                }
                                                val tNorm = ((mVal - domainMin) / max(1e-9, domainMax - domainMin)).toFloat()
                                                Text(
                                                    text = if (activeMetric == GreekHeatmapMetric.DELTA) "%.2f".format(mVal) else "%.3f".format(mVal),
                                                    color = if (tNorm > 0.62f) Color.Black else Color.White,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // X-Axis Labels (Strike Prices)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(xLabelHeight),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        strikes.forEach { s ->
                            val isAtm = abs(s - spotPrice) / spotPrice < 0.02
                            Text(
                                text = "$${"%.0f".format(s)}",
                                color = if (isAtm) CyanGlow else TextMuted,
                                fontSize = 7.5.sp,
                                fontWeight = if (isAtm) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // D3 Gradient Legend Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MIN: ${"%.3f".format(domainMin)}",
                color = TextMuted,
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace
            )

            // D3 Gradient preview strip
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (activeMetric == GreekHeatmapMetric.DELTA) {
                                listOf(Color(0xFF440154), Color(0xFF3B528B), Color(0xFF21908C), Color(0xFF5DC863), Color(0xFFFDE725))
                            } else {
                                listOf(Color(0xFF0D0887), Color(0xFF6A00A8), Color(0xFFB12A90), Color(0xFFE16462), Color(0xFFF0F921))
                            }
                        )
                    )
            )

            Text(
                text = "MAX: ${"%.3f".format(domainMax)}",
                color = CyanGlow,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Detailed Cell Inspection Card
        val target = selectedCell ?: gridCells.firstOrNull()
        if (target != null) {
            val cellGreeks = target.greeks
            val cellDelta = if (activeSide == GreekContractSide.CALL) cellGreeks.callDelta else cellGreeks.putDelta
            val cellPrice = if (activeSide == GreekContractSide.CALL) cellGreeks.callPrice else cellGreeks.putPrice
            val cellTheta = if (activeSide == GreekContractSide.CALL) cellGreeks.callThetaDaily else cellGreeks.putThetaDaily

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF070B12))
                    .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "STRIKE $${"%.1f".format(target.strike)}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "@ ${target.expiryLabel}",
                                color = CyanNeon,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            val moneyness = target.moneynessPct
                            Text(
                                text = "(${if (moneyness >= 0) "+" else ""}${"%.1f".format(moneyness)}% ${if (moneyness > 1.0) "ITM" else if (moneyness < -1.0) "OTM" else "ATM"})",
                                color = if (moneyness > 1.0) EmeraldBid else if (moneyness < -1.0) RoseAsk else CyanGlow,
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "THEO PX: $${"%.2f".format(cellPrice)}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Greeks breakdown row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Δ: ${"%.4f".format(cellDelta)}",
                            color = if (cellDelta >= 0) EmeraldBid else RoseAsk,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Γ: ${"%.5f".format(cellGreeks.callGamma)}",
                            color = Color(0xFFD8B4FE),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "ν: ${"%.3f".format(cellGreeks.callVega)}",
                            color = PurpleDarkPool,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Θ: ${"%.3f".format(cellTheta)}/d",
                            color = RoseAsk,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

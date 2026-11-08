package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.options.BlackScholesGreeks
import com.example.engine.options.BlackScholesGreeksCalculator
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class HeatmapMetric { DELTA, GAMMA }
enum class HeatmapYAxis { VOLATILITY, EXPIRY_DAYS }
enum class HeatmapRenderMode { NATIVE_CANVAS, D3_WEBVIEW }

data class HeatmapCellData(
    val xLabel: String,
    val yLabel: String,
    val spot: Double,
    val yValue: Double,
    val greeks: BlackScholesGreeks,
    val metricValue: Double,
    val normalizedValue: Float // 0.0f to 1.0f for D3 color scale
)

/**
 * Dedicated visualizer component that consumes [BlackScholesGreeksCalculator]
 * and renders a D3-based 2D heatmap matrix of Delta (Δ) and Gamma (Γ) sensitivities.
 */
@Composable
fun OptionGreeksSensitivityHeatmap(
    currentMarketSpot: Double,
    currentSymbol: String,
    initialStrike: Double = currentMarketSpot,
    initialDaysToExpiry: Double = 30.0,
    initialVol: Double = 0.35,
    initialRate: Double = 0.045,
    modifier: Modifier = Modifier
) {
    var optionSide by remember { mutableStateOf(OptionSide.CALL) }
    var selectedMetric by remember { mutableStateOf(HeatmapMetric.DELTA) }
    var yAxisType by remember { mutableStateOf(HeatmapYAxis.VOLATILITY) }
    var renderMode by remember { mutableStateOf(HeatmapRenderMode.NATIVE_CANVAS) }

    var strike by remember(initialStrike) { mutableStateOf(initialStrike) }
    var daysToExpiry by remember { mutableStateOf(initialDaysToExpiry) }
    var baseVol by remember { mutableStateOf(initialVol) }
    var riskFreeRate by remember { mutableStateOf(initialRate) }

    // Selected cell for detailed inspection
    var selectedCell by remember { mutableStateOf<HeatmapCellData?>(null) }

    // Generate Spot Price ladder (7 steps from -15% to +15%)
    val spotSteps = remember(currentMarketSpot) {
        val pctShocks = listOf(-0.15, -0.10, -0.05, 0.0, 0.05, 0.10, 0.15)
        pctShocks.map { shock ->
            val s = currentMarketSpot * (1.0 + shock)
            (s * 100).roundToInt() / 100.0
        }
    }

    // Generate Y-axis steps (Volatility or Expiry)
    val ySteps = remember(yAxisType, baseVol, daysToExpiry) {
        when (yAxisType) {
            HeatmapYAxis.VOLATILITY -> listOf(0.15, 0.25, 0.35, 0.45, 0.55, 0.65)
            HeatmapYAxis.EXPIRY_DAYS -> listOf(7.0, 14.0, 21.0, 30.0, 60.0, 90.0)
        }
    }

    // Compute 2D Matrix of Greeks
    val matrixData = remember(spotSteps, ySteps, strike, optionSide, selectedMetric, yAxisType, baseVol, daysToExpiry, riskFreeRate) {
        val cells = mutableListOf<HeatmapCellData>()
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        // 1st pass: compute raw values
        val tempRaw = mutableListOf<Triple<Double, Double, BlackScholesGreeks>>()
        for (yVal in ySteps) {
            for (spot in spotSteps) {
                val curDays = if (yAxisType == HeatmapYAxis.EXPIRY_DAYS) yVal else daysToExpiry
                val curVol = if (yAxisType == HeatmapYAxis.VOLATILITY) yVal else baseVol

                val g = BlackScholesGreeksCalculator.calculateForCurrentMarket(
                    spot = spot,
                    strike = strike,
                    daysToExpiry = curDays,
                    riskFreeRate = riskFreeRate,
                    volatility = curVol
                )
                val mVal = when (selectedMetric) {
                    HeatmapMetric.DELTA -> if (optionSide == OptionSide.CALL) g.callDelta else g.putDelta
                    HeatmapMetric.GAMMA -> g.callGamma
                }
                minVal = min(minVal, mVal)
                maxVal = max(maxVal, mVal)
                tempRaw.add(Triple(spot, yVal, g))
            }
        }

        val range = max(1e-6, maxVal - minVal)

        // 2nd pass: normalize values for D3 color scale
        tempRaw.forEach { (spot, yVal, g) ->
            val mVal = when (selectedMetric) {
                HeatmapMetric.DELTA -> if (optionSide == OptionSide.CALL) g.callDelta else g.putDelta
                HeatmapMetric.GAMMA -> g.callGamma
            }
            val norm = ((mVal - minVal) / range).toFloat().coerceIn(0f, 1f)
            val xLabel = "$${"%.1f".format(spot)}"
            val yLabel = if (yAxisType == HeatmapYAxis.VOLATILITY) "${(yVal * 100).toInt()}%" else "${yVal.toInt()}D"

            cells.add(
                HeatmapCellData(
                    xLabel = xLabel,
                    yLabel = yLabel,
                    spot = spot,
                    yValue = yVal,
                    greeks = g,
                    metricValue = mVal,
                    normalizedValue = norm
                )
            )
        }
        cells
    }

    // Keep initial inspection cell
    LaunchedEffect(matrixData) {
        if (selectedCell == null && matrixData.isNotEmpty()) {
            selectedCell = matrixData[matrixData.size / 2]
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("option_greeks_heatmap_card"),
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
            // Header Bar & Mode Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Heatmap",
                        tint = CyanNeon,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "D3 SENSITIVITY HEATMAP // ${selectedMetric.name}",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Render Mode Switch (Native Canvas vs D3 SVG WebView)
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (renderMode == HeatmapRenderMode.NATIVE_CANVAS) CyanNeon.copy(alpha = 0.25f) else Color(0xFF0C111A))
                            .clickable { renderMode = HeatmapRenderMode.NATIVE_CANVAS }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANVAS",
                            color = if (renderMode == HeatmapRenderMode.NATIVE_CANVAS) CyanGlow else TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (renderMode == HeatmapRenderMode.D3_WEBVIEW) PurpleDarkPool.copy(alpha = 0.25f) else Color(0xFF0C111A))
                            .clickable { renderMode = HeatmapRenderMode.D3_WEBVIEW }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D3.JS SVG",
                            color = if (renderMode == HeatmapRenderMode.D3_WEBVIEW) Color(0xFFD8B4FE) else TextMuted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Controls Toolbar: Metric Toggle, Y-Axis Toggle, Call/Put Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call / Put Toggle
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (optionSide == OptionSide.CALL) EmeraldBid else Color(0xFF0C111A))
                            .clickable { optionSide = OptionSide.CALL }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CALL", color = if (optionSide == OptionSide.CALL) Color.Black else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (optionSide == OptionSide.PUT) RoseAsk else Color(0xFF0C111A))
                            .clickable { optionSide = OptionSide.PUT }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PUT", color = if (optionSide == OptionSide.PUT) Color.White else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Delta vs Gamma Toggle
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (selectedMetric == HeatmapMetric.DELTA) Color(0xFF0F3042) else Color(0xFF0C111A))
                            .clickable { selectedMetric = HeatmapMetric.DELTA }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DELTA (Δ)", color = if (selectedMetric == HeatmapMetric.DELTA) CyanGlow else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (selectedMetric == HeatmapMetric.GAMMA) Color(0xFF38153A) else Color(0xFF0C111A))
                            .clickable { selectedMetric = HeatmapMetric.GAMMA }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("GAMMA (Γ)", color = if (selectedMetric == HeatmapMetric.GAMMA) Color(0xFFF472B6) else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Y-Axis Mode: Volatility vs DTE
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (yAxisType == HeatmapYAxis.VOLATILITY) Color(0xFF261D10) else Color(0xFF0C111A))
                            .clickable { yAxisType = HeatmapYAxis.VOLATILITY }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y: VOL %", color = if (yAxisType == HeatmapYAxis.VOLATILITY) GoldWarning else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(if (yAxisType == HeatmapYAxis.EXPIRY_DAYS) Color(0xFF142436) else Color(0xFF0C111A))
                            .clickable { yAxisType = HeatmapYAxis.EXPIRY_DAYS }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Y: DTE", color = if (yAxisType == HeatmapYAxis.EXPIRY_DAYS) CyanNeon else TextSecondary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Text(
                    text = "K: $${"%.1f".format(strike)}",
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Heatmap Viewport (Canvas or D3 SVG WebView)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF06090F))
                    .border(0.5.dp, TermBorderHighlight, RoundedCornerShape(3.dp))
            ) {
                if (renderMode == HeatmapRenderMode.NATIVE_CANVAS) {
                    NativeCanvasHeatmap(
                        cells = matrixData,
                        spotSteps = spotSteps,
                        ySteps = ySteps,
                        selectedCell = selectedCell,
                        selectedMetric = selectedMetric,
                        yAxisType = yAxisType,
                        onCellClick = { selectedCell = it }
                    )
                } else {
                    D3WebViewHeatmap(
                        cells = matrixData,
                        spotSteps = spotSteps,
                        ySteps = ySteps,
                        selectedMetric = selectedMetric,
                        yAxisType = yAxisType,
                        symbol = currentSymbol
                    )
                }
            }

            // Cell Inspection HUD (Selected Scenario Breakdown)
            val inspected = selectedCell ?: matrixData.firstOrNull()
            if (inspected != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF090D16))
                        .border(0.5.dp, TermBorder, RoundedCornerShape(3.dp))
                        .padding(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "INSPECTED SCENARIO: Spot ${inspected.xLabel} // ${if (yAxisType == HeatmapYAxis.VOLATILITY) "Vol" else "Expiry"} ${inspected.yLabel}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (optionSide == OptionSide.CALL) EmeraldBid.copy(alpha = 0.2f) else RoseAsk.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                ) {
                                    Text(
                                        text = "${optionSide.name} ${if (optionSide == OptionSide.CALL) inspected.greeks.callMoneyness else inspected.greeks.putMoneyness}",
                                        color = if (optionSide == OptionSide.CALL) EmeraldLight else RoseLight,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "THEOR. PX: $${"%.3f".format(if (optionSide == OptionSide.CALL) inspected.greeks.callPrice else inspected.greeks.putPrice)}",
                                color = CyanGlow,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Detailed Greek Breakdown for selected cell
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val curDelta = if (optionSide == OptionSide.CALL) inspected.greeks.callDelta else inspected.greeks.putDelta
                            val curTheta = if (optionSide == OptionSide.CALL) inspected.greeks.callThetaDaily else inspected.greeks.putThetaDaily
                            val shareDeltaEquiv = (curDelta * 100).toInt()

                            Text("DELTA (Δ): ${"%.4f".format(curDelta)} (${shareDeltaEquiv} shares)", color = if (curDelta >= 0) EmeraldBid else RoseAsk, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text("GAMMA (Γ): ${"%.5f".format(inspected.greeks.callGamma)}", color = CyanNeon, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text("VEGA: $${"%.3f".format(inspected.greeks.callVega)}", color = PurpleDarkPool, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text("THETA: $${"%.3f".format(curTheta)}/d", color = GoldWarning, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Native Jetpack Compose Canvas Heatmap rendering with D3 Viridis / Plasma interpolation
 */
@Composable
private fun NativeCanvasHeatmap(
    cells: List<HeatmapCellData>,
    spotSteps: List<Double>,
    ySteps: List<Double>,
    selectedCell: HeatmapCellData?,
    selectedMetric: HeatmapMetric,
    yAxisType: HeatmapYAxis,
    onCellClick: (HeatmapCellData) -> Unit
) {
    val xCount = spotSteps.size
    val yCount = ySteps.size

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        val yLabelWidth = 34.dp
        val xLabelHeight = 20.dp

        val gridWidth = totalWidth - yLabelWidth
        val gridHeight = totalHeight - xLabelHeight

        val cellWidth = gridWidth / max(1, xCount)
        val cellHeight = gridHeight / max(1, yCount)

        Row(modifier = Modifier.fillMaxSize()) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .width(yLabelWidth)
                    .height(gridHeight),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                // Reverse to display highest Vol/DTE on top
                ySteps.reversed().forEach { yVal ->
                    Text(
                        text = if (yAxisType == HeatmapYAxis.VOLATILITY) "${(yVal * 100).toInt()}%" else "${yVal.toInt()}D",
                        color = TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Grid & X-Axis
            Column(modifier = Modifier.weight(1f)) {
                // Heatmap Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cW = w / xCount
                        val cH = h / yCount

                        cells.forEach { cell ->
                            val colIdx = spotSteps.indexOf(cell.spot)
                            // Invert row index so highest y-step is at top (row 0)
                            val rawRowIdx = ySteps.indexOf(cell.yValue)
                            val rowIdx = (yCount - 1) - rawRowIdx

                            if (colIdx >= 0 && rowIdx >= 0) {
                                val x = colIdx * cW
                                val y = rowIdx * cH

                                // Interpolate color based on D3 color scales
                                val color = when (selectedMetric) {
                                    HeatmapMetric.DELTA -> interpolateD3Viridis(cell.normalizedValue)
                                    HeatmapMetric.GAMMA -> interpolateD3Plasma(cell.normalizedValue)
                                }

                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x + 1f, y + 1f),
                                    size = Size(cW - 2f, cH - 2f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )

                                // Draw highlight border if selected
                                if (selectedCell != null && selectedCell.spot == cell.spot && selectedCell.yValue == cell.yValue) {
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

                    // Clickable Overlay Matrix
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (row in 0 until yCount) {
                            val rawRow = (yCount - 1) - row
                            val yVal = ySteps[rawRow]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                for (col in 0 until xCount) {
                                    val spot = spotSteps[col]
                                    val matchingCell = cells.find { it.spot == spot && it.yValue == yVal }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { matchingCell?.let { onCellClick(it) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (matchingCell != null) {
                                            Text(
                                                text = if (selectedMetric == HeatmapMetric.DELTA) {
                                                    "%.2f".format(matchingCell.metricValue)
                                                } else {
                                                    "%.3f".format(matchingCell.metricValue)
                                                },
                                                color = if (matchingCell.normalizedValue > 0.6f) Color.Black else Color.White,
                                                fontSize = 7.5.sp,
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

                // X-Axis Spot Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(xLabelHeight),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    spotSteps.forEach { s ->
                        Text(
                            text = "$${"%.0f".format(s)}",
                            color = TextMuted,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Embedded D3.js SVG-based HTML Heatmap Renderer inside Android WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun D3WebViewHeatmap(
    cells: List<HeatmapCellData>,
    spotSteps: List<Double>,
    ySteps: List<Double>,
    selectedMetric: HeatmapMetric,
    yAxisType: HeatmapYAxis,
    symbol: String
) {
    val htmlContent = remember(cells, spotSteps, ySteps, selectedMetric, yAxisType, symbol) {
        buildD3Html(cells, spotSteps, ySteps, selectedMetric, yAxisType, symbol)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(0xFF06090F.toInt())
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}

/**
 * Builds standalone, zero-dependency HTML with SVG & D3-style color interpolation
 */
private fun buildD3Html(
    cells: List<HeatmapCellData>,
    spotSteps: List<Double>,
    ySteps: List<Double>,
    selectedMetric: HeatmapMetric,
    yAxisType: HeatmapYAxis,
    symbol: String
): String {
    val xCount = spotSteps.size
    val yCount = ySteps.size
    val width = 360
    val height = 190
    val margin = 35
    val gridWidth = width - margin - 10
    val gridHeight = height - margin - 10
    val cellW = gridWidth / xCount
    val cellH = gridHeight / yCount

    val rectsSvg = StringBuilder()
    cells.forEach { c ->
        val col = spotSteps.indexOf(c.spot)
        val rawRow = ySteps.indexOf(c.yValue)
        val row = (yCount - 1) - rawRow
        if (col >= 0 && row >= 0) {
            val x = margin + col * cellW
            val y = 10 + row * cellH
            val hexColor = if (selectedMetric == HeatmapMetric.DELTA) {
                toHexColor(interpolateD3Viridis(c.normalizedValue))
            } else {
                toHexColor(interpolateD3Plasma(c.normalizedValue))
            }
            val formattedVal = "%.3f".format(c.metricValue)
            rectsSvg.append(
                """
                <rect x="$x" y="$y" width="${cellW - 2}" height="${cellH - 2}" rx="2" fill="$hexColor" stroke="#0B0E14" stroke-width="0.5">
                    <title>$symbol Spot $${c.spot} | ${c.yLabel} | ${selectedMetric.name}: $formattedVal</title>
                </rect>
                <text x="${x + cellW / 2}" y="${y + cellH / 2 + 3}" font-size="7" fill="${if (c.normalizedValue > 0.6f) "#000" else "#fff"}" font-family="monospace" text-anchor="middle" font-weight="bold">$formattedVal</text>
                """.trimIndent()
            )
        }
    }

    // X Axis Labels
    val xLabelsSvg = StringBuilder()
    spotSteps.forEachIndexed { i, s ->
        val x = margin + i * cellW + cellW / 2
        xLabelsSvg.append("""<text x="$x" y="${height - 8}" font-size="7" fill="#64748B" font-family="monospace" text-anchor="middle">$${"%.0f".format(s)}</text>""")
    }

    // Y Axis Labels
    val yLabelsSvg = StringBuilder()
    ySteps.forEachIndexed { i, yVal ->
        val row = (yCount - 1) - i
        val y = 10 + row * cellH + cellH / 2 + 3
        val lbl = if (yAxisType == HeatmapYAxis.VOLATILITY) "${(yVal * 100).toInt()}%" else "${yVal.toInt()}D"
        yLabelsSvg.append("""<text x="${margin - 5}" y="$y" font-size="7.5" fill="#64748B" font-family="monospace" text-anchor="end">$lbl</text>""")
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body { margin: 0; padding: 0; background-color: #06090F; overflow: hidden; }
                svg { width: 100%; height: 100%; display: block; }
            </style>
        </head>
        <body>
            <svg viewBox="0 0 $width $height">
                $rectsSvg
                $xLabelsSvg
                $yLabelsSvg
            </svg>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Analytical D3 Viridis Color Scale Interpolator
 * (Purple -> Blue -> Teal -> Green -> Yellow)
 */
fun interpolateD3Viridis(t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return when {
        clamped < 0.25f -> {
            val f = clamped / 0.25f
            lerpColor(Color(0xFF440154), Color(0xFF3B528B), f)
        }
        clamped < 0.5f -> {
            val f = (clamped - 0.25f) / 0.25f
            lerpColor(Color(0xFF3B528B), Color(0xFF21908C), f)
        }
        clamped < 0.75f -> {
            val f = (clamped - 0.5f) / 0.25f
            lerpColor(Color(0xFF21908C), Color(0xFF5DC863), f)
        }
        else -> {
            val f = (clamped - 0.75f) / 0.25f
            lerpColor(Color(0xFF5DC863), Color(0xFFFDE725), f)
        }
    }
}

/**
 * Analytical D3 Plasma Color Scale Interpolator
 * (Dark Blue -> Magenta -> Flame Orange -> Yellow)
 */
fun interpolateD3Plasma(t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return when {
        clamped < 0.25f -> {
            val f = clamped / 0.25f
            lerpColor(Color(0xFF0D0887), Color(0xFF6A00A8), f)
        }
        clamped < 0.5f -> {
            val f = (clamped - 0.25f) / 0.25f
            lerpColor(Color(0xFF6A00A8), Color(0xFFB12A90), f)
        }
        clamped < 0.75f -> {
            val f = (clamped - 0.5f) / 0.25f
            lerpColor(Color(0xFFB12A90), Color(0xFFE16462), f)
        }
        else -> {
            val f = (clamped - 0.75f) / 0.25f
            lerpColor(Color(0xFFE16462), Color(0xFFF0F921), f)
        }
    }
}

private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    val r = c1.red + (c2.red - c1.red) * f
    val g = c1.green + (c2.green - c1.green) * f
    val b = c1.blue + (c2.blue - c1.blue) * f
    return Color(r, g, b, 1f)
}

private fun toHexColor(color: Color): String {
    val r = (color.red * 255).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

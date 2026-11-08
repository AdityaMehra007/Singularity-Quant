package com.example.ui.workspaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutionTrajectory
import com.example.ui.theme.*

@Composable
fun AlmgrenChrissWorkspace(
    trajectory: ExecutionTrajectory?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper Title & Summary
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
                        text = "ALMGREN-CHRISS // OPTIMAL EXECUTION TRAJECTORY",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MARKET IMPACT MINIMIZER",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (trajectory != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TARGET: ${trajectory.totalOrderShares.toInt()} ${trajectory.symbol}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("HORIZON: ${trajectory.horizonMinutes}m", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("URGENCY κ: ${"%.3f".format(trajectory.urgencyKappa)}", color = CyanNeon, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("SHORTFALL: $${"%.0f".format(trajectory.expectedImplementationShortfallUsd)}", color = GoldWarning, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Hyperbolic Trajectory Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            if (trajectory != null && trajectory.slices.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val totalShares = trajectory.totalOrderShares

                    // Grid
                    for (i in 1..3) {
                        val y = h * (i / 4f)
                        drawLine(Color(0xFF131D2D), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    val path = Path()
                    val stepX = w / trajectory.slices.size
                    path.moveTo(0f, 0f)

                    trajectory.slices.forEachIndexed { idx, s ->
                        val x = (idx + 1) * stepX
                        val y = ((totalShares - s.remainingInventory) / totalShares * h).toFloat()
                        path.lineTo(x, y)

                        // Slice nodes
                        drawCircle(
                            color = CyanNeon,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }

                    drawPath(path, color = CyanNeon, style = Stroke(width = 2.5f))
                }
            }
        }

        // Slice Execution Schedule Table
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .clip(RoundedCornerShape(4.dp))
                .background(TermSurface)
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "DISCRETE TIME SLICE SCHEDULE (x_j = X * sinh(κ(T-t)) / sinh(κT))",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B12))
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SLICE", color = CyanGlow, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("REMAINING", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("SLICED QTY", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("TEMP IMPACT", color = GoldWarning, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("STATUS", color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                }

                if (trajectory != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(trajectory.slices) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF090D15))
                                    .padding(vertical = 3.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.timeLabel, color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("${s.remainingInventory.toInt()}", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                                Text("${s.sliceShares.toInt()}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                Text("+${"%.1f".format(s.temporaryImpactBps)} bps", color = GoldWarning, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (s.status == "ACTIVE") EmeraldBid else Color(0xFF162030))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = s.status,
                                        color = if (s.status == "ACTIVE") Color.Black else TextMuted,
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
    }
}

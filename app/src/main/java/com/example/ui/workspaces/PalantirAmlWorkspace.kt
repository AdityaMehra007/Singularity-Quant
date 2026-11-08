package com.example.ui.workspaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AmlWashCycle
import com.example.model.OntologyEdge
import com.example.model.OntologyEntity
import com.example.ui.theme.*

@Composable
fun PalantirAmlWorkspace(
    entities: List<OntologyEntity>,
    edges: List<OntologyEdge>,
    detectedCycles: List<AmlWashCycle>,
    onTriggerScan: () -> Unit
) {
    var selectedEntity by remember { mutableStateOf<OntologyEntity?>(entities.firstOrNull()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper Title & Trigger Card
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
                        text = "PALANTIR GOTHAM // FINANCIAL CRIME & AML GRAPH",
                        color = CyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tarjan SCC Circular Ring Detection | Entity Knowledge Graph",
                        color = TextMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onTriggerScan,
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("scan_aml_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4C0519),
                        contentColor = RoseLight
                    ),
                    shape = RoundedCornerShape(3.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SCAN AML RINGS", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Detected AML Cycle Alert Banner
        if (detectedCycles.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E050D))
                    .border(1.dp, RoseAsk.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🚨 CRITICAL AML ALERT:", color = RoseLight, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${detectedCycles.size} Circular Wash Loop(s) Detected", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    detectedCycles.forEach { cycle ->
                        Text(
                            text = cycle.detectedLoopDescription,
                            color = Color(0xFFFFB4AB),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Middle Section: Interactive Canvas Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw Edges
                edges.forEach { edge ->
                    val src = entities.find { it.id == edge.sourceId }
                    val tgt = entities.find { it.id == edge.targetId }
                    if (src != null && tgt != null) {
                        val p1 = Offset(src.xRatio * w, src.yRatio * h)
                        val p2 = Offset(tgt.xRatio * w, tgt.yRatio * h)
                        drawLine(
                            color = if (edge.isSuspicious) RoseAsk.copy(alpha = 0.8f) else CyanNeon.copy(alpha = 0.4f),
                            start = p1,
                            end = p2,
                            strokeWidth = if (edge.isSuspicious) 2.5f else 1.2f
                        )
                    }
                }

                // Draw Nodes
                entities.forEach { entity ->
                    val pos = Offset(entity.xRatio * w, entity.yRatio * h)
                    val nodeColor = if (entity.isAlert) RoseAsk else CyanNeon
                    val bgCircleColor = if (entity.isAlert) Color(0xFF4C0519) else Color(0xFF07212C)

                    drawCircle(
                        color = bgCircleColor,
                        radius = 20f,
                        center = pos
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = 20f,
                        center = pos,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }
            }

            // Clickable overlay buttons positioned around nodes
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight
                entities.forEach { entity ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (boxWidth * entity.xRatio) - 40.dp,
                                y = (boxHeight * entity.yRatio) + 12.dp
                            )
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF0A0F1A).copy(alpha = 0.9f))
                            .border(0.5.dp, if (entity.isAlert) RoseAsk else TermBorder, RoundedCornerShape(2.dp))
                            .clickable { selectedEntity = entity }
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = entity.id.take(12),
                            color = if (entity.isAlert) RoseLight else CyanGlow,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Entity Dossier Inspector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .background(TermSurface)
                .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            val entity = selectedEntity ?: entities.firstOrNull()
            if (entity != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ENTITY DOSSIER: ${entity.name}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "TYPE: ${entity.type} // JURISDICTION: ${entity.jurisdiction}",
                                color = TextMuted,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (entity.isAlert) Color(0xFF4C0519) else Color(0xFF0D3325))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RISK SCORE: ${entity.riskScore} / 10.0",
                                color = if (entity.isAlert) RoseLight else EmeraldLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (entity.isAlert && entity.alertReason != null) {
                        Text(
                            text = "FLAG: ${entity.alertReason}",
                            color = RoseAsk,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("30D SETTLEMENT VOLUME: $${"%.2f".format(entity.totalVolumeUsd / 1_000_000.0)}M", color = TextSecondary, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        Text("STATUS: ${if (entity.isAlert) "QUARANTINED" else "CLEARED"}", color = if (entity.isAlert) RoseAsk else EmeraldBid, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

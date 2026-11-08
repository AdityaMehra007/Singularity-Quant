package com.example.ui.workspaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun ExecutionDeskWorkspace(
    symbol: String,
    currentPrice: Double,
    candles: List<CandleData>,
    bids: List<OrderLevel>,
    asks: List<OrderLevel>,
    trades: List<TradePrint>,
    openOrders: List<Order>,
    onSubmitOrder: (Side, OrderType, Double, Int) -> Unit,
    onCancelOrder: (String) -> Unit
) {
    var selectedSide by remember { mutableStateOf(Side.BUY) }
    var selectedOrderType by remember { mutableStateOf(OrderType.LIMIT) }
    var priceText by remember(currentPrice) { mutableStateOf(currentPrice.toString()) }
    var qtyText by remember { mutableStateOf("250") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBackground)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upper section: Chart + Order Book
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // High-Speed Canvas Candlestick & Microstructure Chart
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = symbol,
                                color = CyanGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "L3 TICKS",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "$${"%.2f".format(currentPrice)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Canvas Chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (candles.isEmpty()) return@Canvas
                        val w = size.width
                        val h = size.height

                        val closes = candles.map { it.close }
                        val minP = closes.minOrNull() ?: (currentPrice - 2.0)
                        val maxP = closes.maxOrNull() ?: (currentPrice + 2.0)
                        val range = max(0.01, maxP - minP)

                        // Grid lines
                        for (i in 1..3) {
                            val y = h * (i / 4f)
                            drawLine(
                                color = Color(0xFF182233),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        // Price polyline
                        val path = Path()
                        val stepX = w / max(1, candles.size - 1)
                        candles.forEachIndexed { i, c ->
                            val x = i * stepX
                            val y = h - ((c.close - minP) / range * h).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = CyanNeon, style = Stroke(width = 2.5f))

                        // Current price dashed marker
                        val curY = h - ((currentPrice - minP) / range * h).toFloat()
                        drawLine(
                            color = EmeraldBid,
                            start = Offset(0f, curY),
                            end = Offset(w, curY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            // L2 Depth Ladder
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TermSurface)
                    .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("L2 BOOK", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("SIZE", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("CUM", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Asks (reversed - lowest ask on bottom)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        asks.take(5).reversed().forEach { a ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(a.depthPct)
                                        .align(Alignment.CenterEnd)
                                        .background(RoseAsk.copy(alpha = 0.15f))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("%.2f".format(a.price), color = RoseAsk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                    Text("${a.qty}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text("${a.total}", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // Spread Mid Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(Color(0xFF131B28))
                            .border(0.5.dp, TermBorderHighlight)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SPREAD: $0.10 // MID: $${"%.2f".format(currentPrice)}",
                            color = CyanGlow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bids
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        bids.take(5).forEach { b ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(b.depthPct)
                                        .align(Alignment.CenterEnd)
                                        .background(EmeraldBid.copy(alpha = 0.15f))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("%.2f".format(b.price), color = EmeraldBid, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                                    Text("${b.qty}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text("${b.total}", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lower section: Order Ticket + Time & Sales tape
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Direct Market Access Order Entry Ticket
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TermSurface)
                    .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DMA ORDER TICKET", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = "Pre-trade risk", tint = EmeraldBid, modifier = Modifier.size(11.dp))
                            Text("RISK: OK", color = EmeraldBid, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Side selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, TermBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (selectedSide == Side.BUY) EmeraldBid else Color(0xFF0F1722))
                                .clickable { selectedSide = Side.BUY }
                                .testTag("buy_side_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BUY / LONG", color = if (selectedSide == Side.BUY) Color.Black else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (selectedSide == Side.SELL) RoseAsk else Color(0xFF0F1722))
                                .clickable { selectedSide = Side.SELL }
                                .testTag("sell_side_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SELL / SHORT", color = if (selectedSide == Side.SELL) Color.White else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Order Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(OrderType.LIMIT, OrderType.MARKET, OrderType.ICEBERG, OrderType.DARK_MIDPOINT).forEach { type ->
                            val isSel = selectedOrderType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) CyanNeon.copy(alpha = 0.2f) else Color(0xFF0B0F17))
                                    .border(1.dp, if (isSel) CyanNeon else TermBorder, RoundedCornerShape(3.dp))
                                    .clickable { selectedOrderType = type }
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (type) {
                                        OrderType.LIMIT -> "LIMIT"
                                        OrderType.MARKET -> "MKT"
                                        OrderType.ICEBERG -> "ICE"
                                        OrderType.DARK_MIDPOINT -> "DARK"
                                        else -> "FOK"
                                    },
                                    color = if (isSel) CyanGlow else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Price & Qty inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PRICE", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("order_price_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = TermBorder,
                                    focusedContainerColor = Color(0xFF06090F),
                                    unfocusedContainerColor = Color(0xFF06090F)
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("QUANTITY", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = qtyText,
                                onValueChange = { qtyText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("order_qty_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanNeon,
                                    unfocusedBorderColor = TermBorder,
                                    focusedContainerColor = Color(0xFF06090F),
                                    unfocusedContainerColor = Color(0xFF06090F)
                                )
                            )
                        }
                    }

                    // Transmit Button
                    Button(
                        onClick = {
                            val px = priceText.toDoubleOrNull() ?: currentPrice
                            val q = qtyText.toIntOrNull() ?: 100
                            onSubmitOrder(selectedSide, selectedOrderType, px, q)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("transmit_order_button"),
                        shape = RoundedCornerShape(3.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == Side.BUY) EmeraldBid else RoseAsk,
                            contentColor = if (selectedSide == Side.BUY) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "TRANSMIT ${selectedSide} (${selectedOrderType.name})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Time & Sales Tape
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(TermSurface)
                    .border(1.dp, TermBorder, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TIME & SALES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("TYPE", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(trades) { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t.timestamp.takeLast(9),
                                    color = TextMuted,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "$${"%.2f".format(t.price)}",
                                    color = if (t.side == Side.BUY) EmeraldBid else RoseAsk,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${t.qty}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (t.isDarkPool) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(PurpleDarkPool)
                                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("DARK", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Text(
                                        text = t.side.name,
                                        color = if (t.side == Side.BUY) EmeraldBid else RoseAsk,
                                        fontSize = 8.sp,
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

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CommandPaletteBar(
    commandText: String,
    terminalLog: String,
    onCommandTextChange: (String) -> Unit,
    onExecuteCommand: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090D15))
            .border(1.dp, TermBorder)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // CLI Input Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF04060A))
                .border(1.dp, TermBorderHighlight, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = "CLI Terminal",
                tint = CyanNeon,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CMD>",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = commandText,
                onValueChange = onCommandTextChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input"),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(CyanNeon),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onExecuteCommand(commandText) }),
                decorationBox = { innerTextField ->
                    if (commandText.isEmpty()) {
                        Text(
                            text = "BUY 500 NVDA @ 138.50 LIMIT, STRESS -10%, AUDIT, AML SCAN, NETTING...",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
            )
            IconButton(
                onClick = { onExecuteCommand(commandText) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("command_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Execute Command",
                    tint = CyanNeon,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "BUY 500 @ LIMIT" to "BUY 500 NVDA-PERP @ 138.50 LIMIT",
                "SELL 250 @ MKT" to "SELL 250 NVDA-PERP @ 138.50 MARKET",
                "D3 HEATMAP" to "HEATMAP",
                "DARK PEG 1000" to "BUY 1000 NVDA-PERP @ 138.50 DARK",
                "STRESS -10%" to "STRESS -10%",
                "STRESS +15%" to "STRESS +15%",
                "AUDIT LEDGER" to "AUDIT LEDGER",
                "AML SCAN" to "AML SCAN",
                "NETTING" to "NETTING",
                "KILL SWITCH" to "KILL SWITCH"
            ).forEach { (label, cmd) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                label.contains("BUY") -> Color(0xFF0D3325)
                                label.contains("SELL") -> Color(0xFF3B121C)
                                label.contains("KILL") -> Color(0xFF4C0519)
                                label.contains("STRESS") -> Color(0xFF332009)
                                else -> Color(0xFF131A26)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                label.contains("BUY") -> EmeraldBid.copy(alpha = 0.5f)
                                label.contains("SELL") -> RoseAsk.copy(alpha = 0.5f)
                                label.contains("KILL") -> RoseAsk
                                label.contains("STRESS") -> GoldWarning.copy(alpha = 0.5f)
                                else -> TermBorder
                            },
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { onExecuteCommand(cmd) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        color = when {
                            label.contains("BUY") -> EmeraldLight
                            label.contains("SELL") -> RoseLight
                            label.contains("KILL") -> RoseLight
                            label.contains("STRESS") -> GoldWarning
                            else -> CyanGlow
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Live Log Output
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (terminalLog.contains("FAILED") || terminalLog.contains("ERROR")) RoseAsk else EmeraldBid)
            )
            Text(
                text = terminalLog,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

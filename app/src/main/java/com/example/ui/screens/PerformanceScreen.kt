package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun PerformanceScreen(
    accountState: AccountState,
    tradeLogs: List<TradeLog>,
    onBack: () -> Unit
) {
    val totalTrades = tradeLogs.size
    val winningTrades = remember(tradeLogs) { tradeLogs.filter { it.netPnL > 0 } }
    val losingTrades = remember(tradeLogs) { tradeLogs.filter { it.netPnL < 0 } }

    val winRatePct = remember(totalTrades, winningTrades) {
        if (totalTrades > 0) (winningTrades.size.toDouble() / totalTrades.toDouble()) * 100.0 else 0.0
    }

    val grossProfits = remember(winningTrades) { winningTrades.sumOf { it.netPnL } }
    val grossLosses = remember(losingTrades) { abs(losingTrades.sumOf { it.netPnL }) }

    val profitFactor = remember(grossProfits, grossLosses) {
        if (grossLosses > 0.0) grossProfits / grossLosses else if (grossProfits > 0.0) 9.99 else 0.0
    }

    val avgWinner = remember(winningTrades) {
        if (winningTrades.isNotEmpty()) grossProfits / winningTrades.size else 0.0
    }
    val avgLoser = remember(losingTrades) {
        if (losingTrades.isNotEmpty()) grossLosses / losingTrades.size else 0.0
    }

    val expectancy = remember(winRatePct, avgWinner, avgLoser) {
        val winRate = winRatePct / 100.0
        val lossRate = 1.0 - winRate
        (winRate * avgWinner) - (lossRate * avgLoser)
    }

    val totalFees = remember(tradeLogs) { tradeLogs.sumOf { it.fees } }
    val avgCompliance = remember(tradeLogs) {
        if (tradeLogs.isNotEmpty()) tradeLogs.map { it.ruleComplianceScore }.average() else 100.0
    }

    // Equity Curve Points
    val equityCurve = remember(tradeLogs, accountState.startingBalance) {
        val list = mutableListOf(accountState.startingBalance)
        var running = accountState.startingBalance
        tradeLogs.reversed().forEach { log ->
            running += log.netPnL
            list.add(running)
        }
        list
    }

    // Market Breakdown
    val forexTrades = remember(tradeLogs) { tradeLogs.filter { it.marketType == MarketType.FOREX } }
    val indianTrades = remember(tradeLogs) { tradeLogs.filter { it.marketType == MarketType.INDIAN_MARKET } }
    val cryptoTrades = remember(tradeLogs) { tradeLogs.filter { it.marketType == MarketType.CRYPTO } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Column {
                Text("PERFORMANCE ANALYTICS", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Simulated Account Metrics & Journal Compliance", color = MintGreen, fontSize = 11.sp)
            }
        }

        // Equity Curve Visualization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ACCOUNT EQUITY CURVE", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$${String.format(Locale.US, "%,.2f", accountState.equity)}", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    val netTotal = accountState.equity - accountState.startingBalance
                    Text(
                        text = "${if (netTotal >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", abs(netTotal))}",
                        color = if (netTotal >= 0) BullishGreen else BearishRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (equityCurve.size < 2) {
                            // Flat line
                            drawLine(
                                color = MintGreen,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 3f
                            )
                            return@Canvas
                        }

                        val minVal = equityCurve.minOrNull() ?: 0.0
                        val maxVal = equityCurve.maxOrNull() ?: 1.0
                        val range = max(1.0, maxVal - minVal)
                        val stepX = size.width / (equityCurve.size - 1)

                        val path = Path()
                        equityCurve.forEachIndexed { idx, value ->
                            val x = idx * stepX
                            val y = ((maxVal - value) / range).toFloat() * size.height
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = MintGreen,
                            style = Stroke(width = 3.5f)
                        )
                    }
                }
            }
        }

        // Core Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Win Rate",
                value = "${String.format(Locale.US, "%.1f", winRatePct)}%",
                subtitle = "${winningTrades.size}W / ${losingTrades.size}L",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Profit Factor",
                value = String.format(Locale.US, "%.2f", profitFactor),
                subtitle = "Gross P/L Ratio",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Expectancy",
                value = "${if (expectancy >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", abs(expectancy))}",
                subtitle = "Per Trade",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Avg Winner",
                value = "+$${String.format(Locale.US, "%.2f", avgWinner)}",
                subtitle = "Mean gain",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Avg Loser",
                value = "-$${String.format(Locale.US, "%.2f", avgLoser)}",
                subtitle = "Mean loss",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Rule Compliance",
                value = "${avgCompliance.toInt()}%",
                subtitle = "Plan adherence",
                modifier = Modifier.weight(1f)
            )
        }

        // Market Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PERFORMANCE BY ASSET CLASS", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                MarketBreakdownRow("Forex", forexTrades)
                Divider(color = BorderStroke)
                MarketBreakdownRow("Indian Markets", indianTrades)
                Divider(color = BorderStroke)
                MarketBreakdownRow("Cryptocurrency", cryptoTrades)
            }
        }

        // Transaction Fees
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Transaction Costs Simulated", color = SecondaryText, fontSize = 11.sp)
                    Text("Spreads, Brokerage, STT, and Fees", color = SecondaryText.copy(alpha = 0.7f), fontSize = 10.sp)
                }
                Text("$${String.format(Locale.US, "%.2f", totalFees)}", color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = MintGreen, fontSize = 9.sp)
        }
    }
}

@Composable
fun MarketBreakdownRow(marketName: String, trades: List<TradeLog>) {
    val count = trades.size
    val netPnL = trades.sumOf { it.netPnL }
    val winCount = trades.count { it.netPnL > 0 }
    val winRate = if (count > 0) (winCount.toDouble() / count.toDouble()) * 100.0 else 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(marketName, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("$count trades • Win rate: ${winRate.toInt()}%", color = SecondaryText, fontSize = 11.sp)
        }
        Text(
            text = "${if (netPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", abs(netPnL))}",
            color = if (netPnL >= 0) BullishGreen else BearishRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

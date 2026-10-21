package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaperTradingScreen(
    accountState: AccountState,
    openPositions: List<Position>,
    pendingOrders: List<Order>,
    tradeLogs: List<TradeLog>,
    onClosePosition: (String) -> Unit,
    onPartialClosePosition: (String, Double) -> Unit,
    onCancelOrder: (String) -> Unit,
    onResetAccount: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Positions, 1: Pending Orders, 2: Journal & Closed Trades
    var selectedTradeLogForDetail by remember { mutableStateOf<TradeLog?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Account Balance Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PAPER TRADING ACCOUNT", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "$${String.format(Locale.US, "%,.2f", accountState.equity)}",
                            color = PrimaryText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Account", tint = MintGreen)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBlueGray)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Balance", color = SecondaryText, fontSize = 10.sp)
                        Text("$${String.format(Locale.US, "%,.2f", accountState.balance)}", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Used Margin", color = SecondaryText, fontSize = 10.sp)
                        Text("$${String.format(Locale.US, "%,.2f", accountState.usedMargin)}", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Free Margin", color = SecondaryText, fontSize = 10.sp)
                        Text("$${String.format(Locale.US, "%,.2f", accountState.freeMargin)}", color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
                .padding(2.dp)
        ) {
            listOf("Positions (${openPositions.size})", "Orders (${pendingOrders.size})", "Journal (${tradeLogs.size})").forEachIndexed { index, title ->
                val isSel = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) MintGreen else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSel) DeepBackground else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Content
        when (selectedTab) {
            0 -> {
                // Open Positions List
                if (openPositions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No open positions. Use the trade ticket to execute a paper trade.", color = SecondaryText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(openPositions) { pos ->
                            PositionCard(
                                position = pos,
                                onClose = { onClosePosition(pos.id) },
                                onPartialClose = { onPartialClosePosition(pos.id, pos.quantity / 2.0) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Pending Orders List
                if (pendingOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pending limit or stop orders.", color = SecondaryText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(pendingOrders) { order ->
                            PendingOrderCard(order = order, onCancel = { onCancelOrder(order.id) })
                        }
                    }
                }
            }
            2 -> {
                // Closed Trades & AI Journal
                if (tradeLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No closed trades recorded yet in journal.", color = SecondaryText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(tradeLogs) { log ->
                            TradeLogCard(tradeLog = log, onClick = { selectedTradeLogForDetail = log })
                        }
                    }
                }
            }
        }
    }

    // Trade Review Detail Dialog
    selectedTradeLogForDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedTradeLogForDetail = null },
            title = {
                Text("AI Post-Trade Review: ${log.instrumentId}", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net P&L:", color = SecondaryText)
                        Text(
                            "${if (log.netPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", kotlin.math.abs(log.netPnL))}",
                            color = if (log.netPnL >= 0) BullishGreen else BearishRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rule Compliance:", color = SecondaryText)
                        Text("${log.ruleComplianceScore}%", color = MintGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Exit Reason:", color = SecondaryText)
                        Text(log.exitReason, color = PrimaryText)
                    }
                    if (log.userTradeIdea.isNotBlank()) {
                        Text("Your Planned Idea:", color = SecondaryText, fontSize = 11.sp)
                        Text(log.userTradeIdea, color = PrimaryText, fontSize = 12.sp)
                    }
                    Divider(color = BorderStroke)
                    Text("AI Coach Review:", color = MintGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(log.aiReview, color = PrimaryText, fontSize = 12.sp, lineHeight = 16.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTradeLogForDetail = null }) {
                    Text("Close", color = MintGreen)
                }
            },
            containerColor = CardBackground
        )
    }

    // Reset Account Confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Virtual Account?", color = PrimaryText) },
            text = { Text("This will reset your paper trading balance back to $10,000 USD and clear open positions.", color = SecondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("Confirm Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
fun PositionCard(
    position: Position,
    onClose: () -> Unit,
    onPartialClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (position.direction == TradeDirection.BUY) BullishGreen else BearishRed
                    ) {
                        Text(
                            position.direction.name,
                            color = if (position.direction == TradeDirection.BUY) DeepBackground else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(position.instrumentId, color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${position.quantity} units", color = SecondaryText, fontSize = 12.sp)
                }
                Text(
                    text = "${if (position.unrealizedPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", kotlin.math.abs(position.unrealizedPnL))}",
                    color = if (position.unrealizedPnL >= 0) BullishGreen else BearishRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entry: ${String.format(Locale.US, "%.4f", position.entryPrice)}", color = SecondaryText, fontSize = 11.sp)
                Text("Current: ${String.format(Locale.US, "%.4f", position.currentPrice)}", color = SecondaryText, fontSize = 11.sp)
                position.stopLoss?.let { Text("SL: ${String.format(Locale.US, "%.4f", it)}", color = BearishRed, fontSize = 11.sp) }
                position.takeProfit?.let { Text("TP: ${String.format(Locale.US, "%.4f", it)}", color = BullishGreen, fontSize = 11.sp) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Close Full", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPartialClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("50% Partial Exit", color = MintGreen, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PendingOrderCard(order: Order, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.orderType.name} ${order.direction.name}", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(order.instrumentId, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("Target: ${order.targetPrice ?: 0.0} • Qty: ${order.quantity}", color = SecondaryText, fontSize = 11.sp)
            }
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Cancel", color = BearishRed, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TradeLogCard(tradeLog: TradeLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${tradeLog.direction.name} ${tradeLog.instrumentId}",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = DarkBlueGray) {
                        Text(tradeLog.exitReason, color = MintGreen, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Text(
                    text = "${if (tradeLog.netPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", kotlin.math.abs(tradeLog.netPnL))}",
                    color = if (tradeLog.netPnL >= 0) BullishGreen else BearishRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entry: ${tradeLog.entryPrice} → Exit: ${tradeLog.exitPrice}", color = SecondaryText, fontSize = 11.sp)
                Text("Compliance: ${tradeLog.ruleComplianceScore}%", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Text("Tap for AI Trade Review debrief →", color = MintGreen, fontSize = 11.sp)
        }
    }
}

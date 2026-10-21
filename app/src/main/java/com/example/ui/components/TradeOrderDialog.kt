package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.PaperTradingEngine
import com.example.engine.RiskCalculatorEngine
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun TradeOrderDialog(
    instrument: Instrument,
    quote: Quote,
    accountState: AccountState,
    initialDirection: TradeDirection = TradeDirection.BUY,
    initialQuantity: Double? = null,
    initialStopLoss: Double? = null,
    initialTakeProfit: Double? = null,
    onDismiss: () -> Unit,
    onConfirmMarketOrder: (direction: TradeDirection, quantity: Double, stopLoss: Double?, takeProfit: Double?, tradeIdea: String) -> Unit,
    onConfirmPendingOrder: (direction: TradeDirection, orderType: OrderType, targetPrice: Double, quantity: Double, stopLoss: Double?, takeProfit: Double?) -> Unit
) {
    var orderType by remember { mutableStateOf(OrderType.MARKET) }
    var direction by remember { mutableStateOf(initialDirection) }

    val defaultQty = initialQuantity ?: when (instrument.marketType) {
        MarketType.FOREX -> 0.10
        MarketType.INDIAN_MARKET -> instrument.minQuantity
        MarketType.CRYPTO -> if (instrument.id.startsWith("BTC")) 0.05 else 0.5
    }

    var quantityStr by remember { mutableStateOf(defaultQty.toString()) }
    var pendingPriceStr by remember { mutableStateOf(instrument.formatPrice(quote.mid)) }
    var stopLossStr by remember {
        mutableStateOf(initialStopLoss?.let { instrument.formatPrice(it) } ?: "")
    }
    var takeProfitStr by remember {
        mutableStateOf(initialTakeProfit?.let { instrument.formatPrice(it) } ?: "")
    }
    var tradeIdeaText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quantity = quantityStr.toDoubleOrNull() ?: defaultQty
    val currentPrice = if (direction == TradeDirection.BUY) quote.ask else quote.bid
    val execPrice = if (orderType == OrderType.MARKET) currentPrice else (pendingPriceStr.toDoubleOrNull() ?: currentPrice)
    val stopLoss = stopLossStr.toDoubleOrNull()
    val takeProfit = takeProfitStr.toDoubleOrNull()

    // Calculate real-time risk parameters
    val riskResult = remember(accountState.equity, direction, execPrice, stopLoss, takeProfit, quantity) {
        if (stopLoss != null && stopLoss > 0) {
            val stopDist = kotlin.math.abs(execPrice - stopLoss)
            val pips = stopDist / instrument.pipOrTickSize
            val riskUsd = PaperTradingEngine.computeGrossPnL(instrument, direction, execPrice, stopLoss, quantity)
            val absRiskUsd = kotlin.math.abs(riskUsd)
            val rewardUsd = if (takeProfit != null && takeProfit > 0) {
                PaperTradingEngine.computeGrossPnL(instrument, direction, execPrice, takeProfit, quantity)
            } else 0.0
            val rr = if (absRiskUsd > 0 && rewardUsd > 0) rewardUsd / absRiskUsd else 0.0
            Triple(absRiskUsd, rewardUsd, rr)
        } else {
            Triple(0.0, 0.0, 0.0)
        }
    }

    val requiredMargin = remember(execPrice, quantity) {
        PaperTradingEngine.calculateRequiredMargin(instrument, execPrice, quantity)
    }

    val estimatedFee = remember(execPrice, quantity) {
        PaperTradingEngine.computeTransactionFee(instrument, execPrice, quantity) * 2.0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = instrument.symbol,
                            color = PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${instrument.name} • ${instrument.exchange}",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Order Type Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBlueGray)
                        .padding(2.dp)
                ) {
                    OrderType.values().forEach { type ->
                        val isSelected = orderType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MintGreen else Color.Transparent)
                                .clickable { orderType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.name,
                                color = if (isSelected) DeepBackground else PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direction Selector: BUY (Ask) vs SELL (Bid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BUY BUTTON
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == TradeDirection.BUY) BullishGreen else DarkBlueGray)
                            .clickable { direction = TradeDirection.BUY }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BUY (LONG)",
                                color = if (direction == TradeDirection.BUY) DeepBackground else BullishGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ask: ${instrument.formatPrice(quote.ask)}",
                                color = if (direction == TradeDirection.BUY) DeepBackground.copy(alpha = 0.8f) else SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // SELL BUTTON
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == TradeDirection.SELL) BearishRed else DarkBlueGray)
                            .clickable { direction = TradeDirection.SELL }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SELL (SHORT)",
                                color = if (direction == TradeDirection.SELL) Color.White else BearishRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Bid: ${instrument.formatPrice(quote.bid)}",
                                color = if (direction == TradeDirection.SELL) Color.White.copy(alpha = 0.8f) else SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity / Lots input
                val unitLabel = when (instrument.marketType) {
                    MarketType.FOREX -> "Lots (1.0 = 100k units)"
                    MarketType.INDIAN_MARKET -> if (instrument.isIndex) "Contracts (Lot: ${instrument.contractSize.toInt()})" else "Shares"
                    MarketType.CRYPTO -> "Quantity (${instrument.baseCurrency})"
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(unitLabel, color = SecondaryText, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintGreen,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )

                // Quick stepper buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stepOptions = when (instrument.marketType) {
                        MarketType.FOREX -> listOf(0.01, 0.05, 0.10, 0.50, 1.00)
                        MarketType.INDIAN_MARKET -> listOf(1.0, 10.0, 25.0, 50.0, 100.0)
                        MarketType.CRYPTO -> listOf(0.01, 0.05, 0.1, 0.5, 1.0)
                    }
                    stepOptions.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkBlueGray,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { quantityStr = step.toString() }
                        ) {
                            Text(
                                text = if (step < 1.0) step.toString() else step.toInt().toString(),
                                color = SecondaryText,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (orderType != OrderType.MARKET) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pendingPriceStr,
                        onValueChange = { pendingPriceStr = it },
                        label = { Text("Limit/Stop Trigger Price", color = SecondaryText, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MintGreen,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SL and TP Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossStr,
                        onValueChange = { stopLossStr = it },
                        label = { Text("Stop Loss (SL)", color = BearishRed, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BearishRed,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        )
                    )
                    OutlinedTextField(
                        value = takeProfitStr,
                        onValueChange = { takeProfitStr = it },
                        label = { Text("Take Profit (TP)", color = BullishGreen, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Educational requirement: Explain trade idea
                OutlinedTextField(
                    value = tradeIdeaText,
                    onValueChange = { tradeIdeaText = it },
                    label = { Text("Trade Reason / SMC Setup Idea (Learning Mode)", color = SecondaryText, fontSize = 11.sp) },
                    placeholder = { Text("e.g. FVG mitigation + 15m bullish BOS", color = SecondaryText.copy(alpha = 0.5f), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintGreen,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Risk & Margin Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBlueGray)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Required Margin:", color = SecondaryText, fontSize = 11.sp)
                        Text("$${String.format(java.util.Locale.US, "%.2f", requiredMargin)}", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monetary Risk (SL):", color = SecondaryText, fontSize = 11.sp)
                        Text(
                            if (riskResult.first > 0) "-$${String.format(java.util.Locale.US, "%.2f", riskResult.first)}" else "None configured",
                            color = if (riskResult.first > 0) BearishRed else SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Potential Reward (TP):", color = SecondaryText, fontSize = 11.sp)
                        Text(
                            if (riskResult.second > 0) "+$${String.format(java.util.Locale.US, "%.2f", riskResult.second)}" else "Open",
                            color = if (riskResult.second > 0) BullishGreen else SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (riskResult.third > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reward-to-Risk (R:R):", color = SecondaryText, fontSize = 11.sp)
                            Text("1 : ${String.format(java.util.Locale.US, "%.2f", riskResult.third)}", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Est. Round-Trip Fees:", color = SecondaryText, fontSize = 11.sp)
                        Text("$${String.format(java.util.Locale.US, "%.2f", estimatedFee)}", color = SecondaryText, fontSize = 11.sp)
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage ?: "", color = BearishRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Place Order Action Button
                Button(
                    onClick = {
                        if (quantity <= 0.0) {
                            errorMessage = "Please enter a valid quantity."
                            return@Button
                        }
                        if (requiredMargin > accountState.equity) {
                            errorMessage = "Insufficient margin. Available equity: $${String.format(java.util.Locale.US, "%.2f", accountState.equity)}"
                            return@Button
                        }
                        // Stop loss logic validation
                        if (stopLoss != null) {
                            if (direction == TradeDirection.BUY && stopLoss >= execPrice) {
                                errorMessage = "For BUY trades, Stop Loss must be BELOW entry price."
                                return@Button
                            }
                            if (direction == TradeDirection.SELL && stopLoss <= execPrice) {
                                errorMessage = "For SELL trades, Stop Loss must be ABOVE entry price."
                                return@Button
                            }
                        }

                        if (orderType == OrderType.MARKET) {
                            onConfirmMarketOrder(direction, quantity, stopLoss, takeProfit, tradeIdeaText)
                        } else {
                            val target = pendingPriceStr.toDoubleOrNull() ?: execPrice
                            onConfirmPendingOrder(direction, orderType, target, quantity, stopLoss, takeProfit)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (direction == TradeDirection.BUY) BullishGreen else BearishRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (orderType == OrderType.MARKET) "EXECUTE ${direction.name} ORDER" else "PLACE PENDING ${direction.name}",
                        color = if (direction == TradeDirection.BUY) DeepBackground else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import com.example.engine.RiskCalculatorEngine
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun RiskCalculatorScreen(
    accountState: AccountState,
    quotes: Map<String, Quote>,
    initialInstrument: Instrument? = null,
    onBack: () -> Unit,
    onApplyToTrade: (instrument: Instrument, direction: TradeDirection, quantity: Double, sl: Double?, tp: Double?) -> Unit
) {
    var selectedInstrument by remember {
        mutableStateOf(initialInstrument ?: InstrumentsCatalog.ALL_INSTRUMENTS.first())
    }

    val quote = quotes[selectedInstrument.id]
    var direction by remember { mutableStateOf(TradeDirection.BUY) }

    var entryPriceStr by remember(selectedInstrument, quote) {
        mutableStateOf(quote?.let { selectedInstrument.formatPrice(if (direction == TradeDirection.BUY) it.ask else it.bid) } ?: "1.00")
    }

    var stopLossStr by remember(selectedInstrument, entryPriceStr, direction) {
        val entry = entryPriceStr.toDoubleOrNull() ?: 1.0
        val defaultSl = if (direction == TradeDirection.BUY) entry * 0.995 else entry * 1.005
        mutableStateOf(selectedInstrument.formatPrice(defaultSl))
    }

    var targetPriceStr by remember(selectedInstrument, entryPriceStr, direction) {
        val entry = entryPriceStr.toDoubleOrNull() ?: 1.0
        val defaultTp = if (direction == TradeDirection.BUY) entry * 1.015 else entry * 0.985
        mutableStateOf(selectedInstrument.formatPrice(defaultTp))
    }

    var selectedRiskPercent by remember { mutableStateOf(1.0) } // default 1% rule
    var customRiskPercentStr by remember { mutableStateOf("1.0") }

    val entryPrice = entryPriceStr.toDoubleOrNull() ?: 0.0
    val stopLossPrice = stopLossStr.toDoubleOrNull() ?: 0.0
    val targetPrice = targetPriceStr.toDoubleOrNull()

    // Calculate risk
    val calculation = remember(accountState.equity, selectedInstrument, direction, entryPrice, stopLossPrice, targetPrice, selectedRiskPercent) {
        RiskCalculatorEngine.calculateRisk(
            equity = accountState.equity,
            instrument = selectedInstrument,
            direction = direction,
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            targetPrice = targetPrice,
            riskPercent = selectedRiskPercent
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Column {
                Text(
                    text = "ACCURATE RISK CALCULATOR",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Position Sizing • 1% Capital Preservation Rule",
                    color = MintGreen,
                    fontSize = 11.sp
                )
            }
        }

        // Instrument Selector Carousel
        Column {
            Text("SELECT INSTRUMENT", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InstrumentsCatalog.ALL_INSTRUMENTS) { inst ->
                    val isSel = inst.id == selectedInstrument.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) MintGreen else CardBackground,
                        modifier = Modifier.clickable { selectedInstrument = inst }
                    ) {
                        Text(
                            text = inst.symbol,
                            color = if (isSel) DeepBackground else PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Direction Selector (Buy / Sell)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (direction == TradeDirection.BUY) BullishGreen else CardBackground)
                    .clickable { direction = TradeDirection.BUY }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BUY / LONG",
                    color = if (direction == TradeDirection.BUY) DeepBackground else BullishGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (direction == TradeDirection.SELL) BearishRed else CardBackground)
                    .clickable { direction = TradeDirection.SELL }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SELL / SHORT",
                    color = if (direction == TradeDirection.SELL) Color.White else BearishRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Educational Risk Presets
        Column {
            Text("RISK PERCENTAGE OF EQUITY ($${String.format(Locale.US, "%,.2f", accountState.equity)})", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5, 1.0, 2.0, 3.0).forEach { pct ->
                    val isSel = selectedRiskPercent == pct
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) MintGreen else CardBackground,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedRiskPercent = pct
                                customRiskPercentStr = pct.toString()
                            }
                    ) {
                        Text(
                            text = "$pct%",
                            color = if (isSel) DeepBackground else PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = entryPriceStr,
                    onValueChange = { entryPriceStr = it },
                    label = { Text("Entry Price (${selectedInstrument.quoteCurrency})", color = SecondaryText, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MintGreen,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )

                OutlinedTextField(
                    value = stopLossStr,
                    onValueChange = { stopLossStr = it },
                    label = { Text("Stop Loss Price (SL)", color = BearishRed, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BearishRed,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )

                OutlinedTextField(
                    value = targetPriceStr,
                    onValueChange = { targetPriceStr = it },
                    label = { Text("Take Profit Price (TP)", color = BullishGreen, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BullishGreen,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )
            }
        }

        // Calculation Results Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (calculation.isValid) DarkBlueGray else CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!calculation.isValid) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = calculation.validationError ?: "Invalid calculation parameters",
                            color = BearishRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Sizing Result Hero
                    val qtyUnit = when (selectedInstrument.marketType) {
                        MarketType.FOREX -> "Lots"
                        MarketType.INDIAN_MARKET -> if (selectedInstrument.isIndex) "Contracts" else "Shares"
                        MarketType.CRYPTO -> selectedInstrument.baseCurrency
                    }

                    Column {
                        Text(
                            text = "RECOMMENDED POSITION SIZE",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.2f", calculation.recommendedQuantityOrLots),
                                color = MintGreen,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = qtyUnit,
                                color = PrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Divider(color = BorderStroke)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monetary Risk ($selectedRiskPercent%):", color = SecondaryText, fontSize = 12.sp)
                        Text("-$${String.format(Locale.US, "%.2f", calculation.monetaryRisk)}", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Potential Reward:", color = SecondaryText, fontSize = 12.sp)
                        Text("+$${String.format(Locale.US, "%.2f", calculation.potentialReward)}", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Reward-to-Risk (R:R):", color = SecondaryText, fontSize = 12.sp)
                        Text("1 : ${String.format(Locale.US, "%.2f", calculation.rewardToRiskRatio)}", color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stop Loss Distance:", color = SecondaryText, fontSize = 12.sp)
                        Text("${String.format(Locale.US, "%.1f", calculation.stopDistancePipsOrTicks)} pips/ticks", color = PrimaryText, fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Required Margin:", color = SecondaryText, fontSize = 12.sp)
                        Text("$${String.format(Locale.US, "%.2f", calculation.estimatedMargin)}", color = PrimaryText, fontSize = 12.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Round-Trip Fees:", color = SecondaryText, fontSize = 12.sp)
                        Text("$${String.format(Locale.US, "%.2f", calculation.estimatedFee)}", color = SecondaryText, fontSize = 12.sp)
                    }
                }
            }
        }

        // Apply to Trade Button
        Button(
            onClick = {
                if (calculation.isValid) {
                    onApplyToTrade(selectedInstrument, direction, calculation.recommendedQuantityOrLots, stopLossPrice, targetPrice)
                }
            },
            enabled = calculation.isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
        ) {
            Text("APPLY SIZING & OPEN TRADE TICKET", color = DeepBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

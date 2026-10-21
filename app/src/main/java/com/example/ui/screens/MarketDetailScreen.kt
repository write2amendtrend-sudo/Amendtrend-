package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.repository.MarketDataRepository
import com.example.engine.PaperTradingEngine
import com.example.engine.SmartMoneyConceptEngine
import com.example.model.*
import com.example.ui.components.CandlestickChart
import com.example.ui.components.TradeOrderDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MarketDetailScreen(
    instrument: Instrument,
    quote: Quote?,
    accountState: AccountState,
    marketDataRepository: MarketDataRepository,
    openPositions: List<Position>,
    onBack: () -> Unit,
    onNavigateToAiAnalyst: (Instrument) -> Unit,
    onNavigateToRiskCalculator: (Instrument) -> Unit,
    onClosePosition: (String) -> Unit,
    onPartialClosePosition: (String, Double) -> Unit,
    onExecuteMarketOrder: (direction: TradeDirection, quantity: Double, sl: Double?, tp: Double?, tradeIdea: String) -> Unit,
    onExecutePendingOrder: (direction: TradeDirection, orderType: OrderType, target: Double, quantity: Double, sl: Double?, tp: Double?) -> Unit,
    subscriptionState: UserSubscriptionState = UserSubscriptionState(),
    onNavigateToPremium: () -> Unit = {},
    userTimezone: UserTimezone = UserTimezone.UTC
) {
    var selectedTimeframe by remember { mutableStateOf(Timeframe.M15) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderDialogDirection by remember { mutableStateOf(TradeDirection.BUY) }

    // Replay mode state
    var isReplayMode by remember { mutableStateOf(false) }
    var isPlayingReplay by remember { mutableStateOf(false) }
    var replayCandlesCount by remember { mutableStateOf(50) }

    val allCandles = remember(instrument, selectedTimeframe, replayCandlesCount) {
        marketDataRepository.getCandles(instrument, selectedTimeframe, count = 75)
    }

    val displayCandles = remember(allCandles, isReplayMode, replayCandlesCount) {
        if (isReplayMode) {
            allCandles.take(replayCandlesCount.coerceIn(15, allCandles.size))
        } else {
            allCandles
        }
    }

    // Auto replay loop
    LaunchedEffect(isPlayingReplay) {
        while (isPlayingReplay) {
            delay(1000)
            if (replayCandlesCount < allCandles.size) {
                replayCandlesCount += 1
            } else {
                isPlayingReplay = false
            }
        }
    }

    // Compute SMC analysis for current candles
    val smcAnalysis = remember(displayCandles) {
        SmartMoneyConceptEngine.analyzeCandles(instrument, selectedTimeframe, displayCandles)
    }

    val myPositions = remember(openPositions, instrument.id) {
        openPositions.filter { it.instrumentId == instrument.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }
                Column {
                    Text(
                        text = instrument.symbol,
                        color = PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${instrument.name} • ${instrument.exchange}",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            if (quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = instrument.formatPrice(quote.mid),
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Spread: ${String.format(Locale.US, "%.1f", instrument.baseSpreadPipsOrTicks)} | Bid: ${instrument.formatPrice(quote.bid)}",
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Licensed Data Provider & Market Session Status Card
        if (quote != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val badgeColor = when (quote.dataType) {
                                DataType.LIVE -> BullishGreen
                                DataType.DELAYED -> WarningGold
                                DataType.HISTORICAL -> AccentCyan
                                DataType.MARKET_CLOSED -> BearishRed
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                            ) {
                                Text(
                                    text = "● ${quote.dataType.label}",
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quote.sessionNotice,
                                color = if (quote.isMarketOpen) MintGreen else SecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = { marketDataRepository.syncFromLicensedProviders() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MintGreen, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Provider: ${quote.dataSource}",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${userTimezone.code}: ${userTimezone.formatTimeOnly(quote.timestamp)}",
                            color = PrimaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Timeframe selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardBackground)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Timeframe.values().forEach { tf ->
                val isSelected = selectedTimeframe == tf
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MintGreen else Color.Transparent)
                        .clickable { selectedTimeframe = tf }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tf.label,
                        color = if (isSelected) DeepBackground else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Historical Replay Controller
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = if (isReplayMode) SurfaceVariantDark else CardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isReplayMode,
                        onCheckedChange = {
                            isReplayMode = it
                            if (!it) isPlayingReplay = false else replayCandlesCount = 35
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepBackground,
                            checkedTrackColor = MintGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReplayMode) "REPLAY MODE (No Lookahead)" else "Live Market Stream",
                        color = if (isReplayMode) MintGreen else SecondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isReplayMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                if (replayCandlesCount < allCandles.size) replayCandlesCount += 1
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Step +1 Bar", tint = PrimaryText)
                        }
                        IconButton(
                            onClick = { isPlayingReplay = !isPlayingReplay },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isPlayingReplay) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MintGreen
                            )
                        }
                    }
                }
            }
        }

        // Interactive Candlestick Chart
        val firstPos = myPositions.firstOrNull()
        CandlestickChart(
            candles = displayCandles,
            instrument = instrument,
            userTimezone = userTimezone,
            smcEvents = smcAnalysis.smcEvents,
            supportLevel = smcAnalysis.supportLevel,
            resistanceLevel = smcAnalysis.resistanceLevel,
            entryLevel = firstPos?.entryPrice,
            stopLossLevel = firstPos?.stopLoss ?: smcAnalysis.stopLossPrice,
            takeProfitLevel = firstPos?.takeProfit ?: smcAnalysis.takeProfitPrice
        )

        // SMC Quick Signal Pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT STRUCTURE", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${smcAnalysis.trend} • ${smcAnalysis.setupRecommendation}",
                        color = if (smcAnalysis.trend == TrendDirection.BULLISH) BullishGreen else if (smcAnalysis.trend == TrendDirection.BEARISH) BearishRed else WarningGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { onNavigateToAiAnalyst(instrument) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintGreen)
                ) {
                    Text("Deep AI Scan", fontSize = 11.sp)
                }
            }
        }

        // VIP Facility: Indian Options Chain & Greeks or Leverage Specs
        if (instrument.marketType == MarketType.INDIAN_MARKET) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !subscriptionState.isPremium) { onNavigateToPremium() },
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
                            Icon(
                                imageVector = if (subscriptionState.isPremium) Icons.Default.Verified else Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (subscriptionState.isPremium) MintGreen else WarningGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "NSE OPTIONS CHAIN & GREEKS",
                                color = if (subscriptionState.isPremium) MintGreen else PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (subscriptionState.isPremium) BullishGreen.copy(alpha = 0.2f) else MintGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (subscriptionState.isPremium) "ACTIVE RADAR" else "VIP FACILITY",
                                color = MintGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (subscriptionState.isPremium) {
                        val currentPrice = quote?.lastPrice ?: instrument.contractMultiplier
                        val strikeStep = if (instrument.id.contains("BANK")) 100.0 else 50.0
                        val baseStrike = (kotlin.math.round(currentPrice / strikeStep) * strikeStep)

                        Text(
                            text = "Weekly Expiry Greeks • Max Pain Level: ₹${String.format(Locale.US, "%,.0f", baseStrike)}",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBlueGray, RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CE Delta", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Strike", color = MintGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("PE Delta", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Theta", color = SecondaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        listOf(-1, 0, 1).forEach { offset ->
                            val s = baseStrike + (offset * strikeStep)
                            val isAtm = offset == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isAtm) MintGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(vertical = 3.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (offset <= 0) "Δ 0.54" else "Δ 0.38",
                                    color = BullishGreen,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.0f", s)} ${if (isAtm) "(ATM)" else ""}",
                                    color = if (isAtm) MintGreen else PrimaryText,
                                    fontSize = 10.sp,
                                    fontWeight = if (isAtm) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (offset >= 0) "Δ -0.52" else "Δ -0.35",
                                    color = BearishRed,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "-₹18.4/d",
                                    color = SecondaryText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Unlock simulated NSE options chains, strike-by-strike Greeks (Delta, Gamma, Theta, Vega) & Max Pain heatmaps with VIP ($10/mo).",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                        OutlinedButton(
                            onClick = onNavigateToPremium,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MintGreen)
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlock Options Chain with VIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Positions on this instrument
        if (myPositions.isNotEmpty()) {
            Text("ACTIVE POSITION ON ${instrument.symbol}", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            myPositions.forEach { pos ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${pos.direction} ${pos.quantity} units @ ${instrument.formatPrice(pos.entryPrice)}",
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (pos.unrealizedPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", kotlin.math.abs(pos.unrealizedPnL))}",
                                color = if (pos.unrealizedPnL >= 0) BullishGreen else BearishRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onClosePosition(pos.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Market Close", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onPartialClosePosition(pos.id, pos.quantity / 2.0) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("50% Partial Close", color = MintGreen, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Trading Execution Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    orderDialogDirection = TradeDirection.BUY
                    showOrderDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BullishGreen)
            ) {
                Text("BUY / LONG", color = DeepBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    orderDialogDirection = TradeDirection.SELL
                    showOrderDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
            ) {
                Text("SELL / SHORT", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Calculator Shortcut
        TextButton(
            onClick = { onNavigateToRiskCalculator(instrument) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Calculate Position Size with 1% Risk Rule", color = MintGreen, fontSize = 12.sp)
        }
    }

    if (showOrderDialog && quote != null) {
        TradeOrderDialog(
            instrument = instrument,
            quote = quote,
            accountState = accountState,
            initialDirection = orderDialogDirection,
            initialStopLoss = smcAnalysis.stopLossPrice,
            initialTakeProfit = smcAnalysis.takeProfitPrice,
            onDismiss = { showOrderDialog = false },
            onConfirmMarketOrder = { dir, qty, sl, tp, idea ->
                onExecuteMarketOrder(dir, qty, sl, tp, idea)
                showOrderDialog = false
            },
            onConfirmPendingOrder = { dir, type, target, qty, sl, tp ->
                onExecutePendingOrder(dir, type, target, qty, sl, tp)
                showOrderDialog = false
            }
        )
    }
}

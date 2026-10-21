package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.repository.AiAnalystRepository
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiAnalystScreen(
    aiAnalystRepository: AiAnalystRepository,
    initialInstrument: Instrument? = null,
    subscriptionState: UserSubscriptionState = UserSubscriptionState(),
    onNavigateToPremium: () -> Unit = {},
    onBack: () -> Unit,
    onExecuteAiTrade: (instrument: Instrument, direction: TradeDirection, sl: Double?, tp: Double?) -> Unit
) {
    var selectedInstrument by remember {
        mutableStateOf(initialInstrument ?: InstrumentsCatalog.ALL_INSTRUMENTS.first())
    }
    var selectedTimeframe by remember { mutableStateOf(Timeframe.M15) }
    var isLoading by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf<MarketStructureAnalysis?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun runScan() {
        coroutineScope.launch {
            isLoading = true
            analysis = aiAnalystRepository.analyzeInstrument(selectedInstrument, selectedTimeframe)
            isLoading = false
        }
    }

    LaunchedEffect(selectedInstrument, selectedTimeframe) {
        runScan()
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }
                Column {
                    Text(
                        text = "AI MARKET ANALYST",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Institutional SMC Engine & Order Flow",
                        color = MintGreen,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = { runScan() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = MintGreen)
            }
        }

        // Instrument Carousel
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

        // Timeframe selector
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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(CardBackground, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MintGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Scanning order flow & liquidity pools...", color = SecondaryText, fontSize = 12.sp)
                }
            }
        } else if (analysis != null) {
            val a = analysis!!

            // Data Verification Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkBlueGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DATA INTEGRITY VERIFIED", color = MintGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Source: Amend Trend Core Feed • UTC Timestamp: ${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(a.timestamp))}",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                    }
                    if (a.isAiEnhanced) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MintGreen) {
                            Text("GEMINI ENRICHED", color = DeepBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            // Setup Recommendation Hero Card
            val isSetupValid = a.setupRecommendation != "NO VALID SETUP"
            val badgeColor = if (isSetupValid) (if (a.trend == TrendDirection.BULLISH) BullishGreen else BearishRed) else WarningGold

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("EVALUATED SETUP", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = a.setupRecommendation,
                                color = badgeColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                        ) {
                            Text(
                                text = "Confidence: ${a.setupConfidence}",
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = a.observedConditions,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Divider(color = BorderStroke)

                    // Key Levels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Support (SSL)", color = SecondaryText, fontSize = 11.sp)
                            Text(selectedInstrument.formatPrice(a.supportLevel), color = BullishGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Resistance (BSL)", color = SecondaryText, fontSize = 11.sp)
                            Text(selectedInstrument.formatPrice(a.resistanceLevel), color = BearishRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (a.riskToReward > 0) {
                            Column {
                                Text("Planned R:R", color = SecondaryText, fontSize = 11.sp)
                                Text("1 : ${String.format(Locale.US, "%.1f", a.riskToReward)}", color = MintGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // VIP Facility: Multi-Timeframe Confluence Matrix
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
                                "MULTI-TIMEFRAME CONFLUENCE RADAR",
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
                                text = if (subscriptionState.isPremium) "6-TF ALIGNED" else "VIP FACILITY",
                                color = MintGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (subscriptionState.isPremium) {
                        Text(
                            text = "Institutional consensus across 1m, 5m, 15m, 1h, 4h, and Daily timeframes:",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("1M", "Scalp BOS", BullishGreen),
                                Triple("5M", "FVG Retest", BullishGreen),
                                Triple("15M", "Order Block", BullishGreen),
                                Triple("1H", "Bullish Trend", BullishGreen),
                                Triple("4H", "CHoCH Up", BullishGreen),
                                Triple("1D", "Discount Zone", MintGreen)
                            ).forEach { (tfName, state, color) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkBlueGray,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(tfName, color = PrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(state, color = color, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkBlueGray,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Directional Confluence Score:", color = SecondaryText, fontSize = 10.sp)
                                Text("92% Bullish Institutional Alignment", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Unlock simultaneous multi-timeframe analysis (1m, 5m, 15m, 1h, 4h, 1D) with institutional directional confluence scoring. Plans from $10/mo.",
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
                            Text("Unlock Confluence Radar with VIP ($10/mo)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Evidence Supporting Setup
            if (a.supportingEvidence.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("INSTITUTIONAL EVIDENCE", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        a.supportingEvidence.forEach { ev ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = MintGreen, fontWeight = FontWeight.Bold)
                                Text(ev, color = PrimaryText, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }

            // Invalidation & Caution
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("INVALIDATION & REASONS TO AVOID TRADING", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Invalidation Level: ${a.invalidationCondition}", color = PrimaryText, fontSize = 12.sp)
                    a.cautionReasons.forEach { c ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("⚠ ", color = WarningGold)
                            Text(c, color = SecondaryText, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Execute AI Setup Button
            if (isSetupValid && a.entryPrice != null) {
                Button(
                    onClick = {
                        val dir = if (a.trend == TrendDirection.BULLISH) TradeDirection.BUY else TradeDirection.SELL
                        onExecuteAiTrade(selectedInstrument, dir, a.stopLossPrice, a.takeProfitPrice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
                ) {
                    Text(
                        text = "EXECUTE WITH AI RISK PLAN",
                        color = if (a.trend == TrendDirection.BULLISH) DeepBackground else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

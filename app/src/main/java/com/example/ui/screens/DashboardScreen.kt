package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.AmendTrendLogo
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    accountState: AccountState,
    quotes: Map<String, Quote>,
    onSelectInstrument: (Instrument) -> Unit,
    onNavigateToRiskCalculator: (Instrument?) -> Unit,
    onNavigateToAiAnalyst: (Instrument?) -> Unit,
    onNavigateToVideoStudio: (Instrument?) -> Unit,
    onNavigateToEducation: () -> Unit,
    subscriptionState: UserSubscriptionState = UserSubscriptionState(),
    onNavigateToPremium: () -> Unit = {},
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    userTimezone: UserTimezone = UserTimezone.UTC,
    onRefreshProviders: () -> Unit = {}
) {
    var selectedMarketTab by remember { mutableStateOf<MarketType?>(null) } // null = ALL

    val instruments = remember(selectedMarketTab) {
        if (selectedMarketTab == null) {
            InstrumentsCatalog.ALL_INSTRUMENTS
        } else {
            InstrumentsCatalog.ALL_INSTRUMENTS.filter { it.marketType == selectedMarketTab }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Top Header with Logo & Live Provider Status
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AmendTrendLogo(size = 40.dp)

                    val statusBg = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> BullishGreen
                        ConnectionStatus.RATE_LIMITED -> WarningGold
                        ConnectionStatus.RECONNECTING -> AccentCyan
                        ConnectionStatus.FALLBACK_SIMULATION -> DarkBlueGray
                    }
                    val statusText = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> "LIVE FEED"
                        ConnectionStatus.RATE_LIMITED -> "RATE LIMITED"
                        ConnectionStatus.RECONNECTING -> "RETRYING"
                        ConnectionStatus.FALLBACK_SIMULATION -> "SIMULATION"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBackground)
                            .clickable { onRefreshProviders() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusBg)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = if (connectionStatus == ConnectionStatus.CONNECTED) MintGreen else PrimaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = SecondaryText, modifier = Modifier.size(14.dp))
                    }
                }

                // Data Providers & Timezone Bar
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Feeds: Binance • Frankfurter ECB • NSE",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${userTimezone.code}: ${userTimezone.formatTimeOnly(System.currentTimeMillis())}",
                            color = MintGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Account Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VIRTUAL ACCOUNT EQUITY",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        val pnl = accountState.equity - accountState.startingBalance
                        val pnlPct = if (accountState.startingBalance > 0) (pnl / accountState.startingBalance) * 100 else 0.0
                        Text(
                            text = "${if (pnl >= 0) "+" else ""}${String.format(Locale.US, "%.2f", pnlPct)}%",
                            color = if (pnl >= 0) BullishGreen else BearishRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", accountState.equity)}",
                        color = PrimaryText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBlueGray)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Balance", color = SecondaryText, fontSize = 11.sp)
                            Text("$${String.format(Locale.US, "%,.2f", accountState.balance)}", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Used Margin", color = SecondaryText, fontSize = 11.sp)
                            Text("$${String.format(Locale.US, "%,.2f", accountState.usedMargin)}", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Free Margin", color = SecondaryText, fontSize = 11.sp)
                            Text("$${String.format(Locale.US, "%,.2f", accountState.freeMargin)}", color = MintGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // VIP Membership Facility Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPremium() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (subscriptionState.isPremium) MintGreen.copy(alpha = 0.2f) else DarkBlueGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (subscriptionState.isPremium) Icons.Default.Verified else Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (subscriptionState.isPremium) MintGreen else WarningGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (subscriptionState.isPremium) "VIP ACTIVE" else "AMEND TREND VIP",
                                    color = if (subscriptionState.isPremium) MintGreen else PrimaryText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (subscriptionState.isPremium) BullishGreen.copy(alpha = 0.2f) else MintGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (subscriptionState.isPremium) "${subscriptionState.plan?.durationLabel}" else "PLANS: $10 • $60 • $120",
                                        color = MintGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (subscriptionState.isPremium) {
                                    "All 9 facilities unlocked: 1:500 Leverage, Confluence AI, Options Chain & HD Studio."
                                } else {
                                    "Unlock multi-timeframe AI scan, 1:500 leverage, HD video studio & NSE options."
                                },
                                color = SecondaryText,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MintGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Quick Action Hub
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickHubButton(
                    icon = Icons.Default.Analytics,
                    title = "AI Analyst",
                    subtitle = "SMC Scan",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToAiAnalyst(null) }
                )
                QuickHubButton(
                    icon = Icons.Default.Calculate,
                    title = "Calculator",
                    subtitle = "1% Risk Rule",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToRiskCalculator(null) }
                )
                QuickHubButton(
                    icon = Icons.Default.VideoLibrary,
                    title = "AI Studio",
                    subtitle = "TTS Video",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToVideoStudio(null) }
                )
                QuickHubButton(
                    icon = Icons.Default.School,
                    title = "Learn",
                    subtitle = "Quizzes",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEducation
                )
            }
        }

        // 4. Market Filter Tabs (All, Forex, Indian Markets, Crypto)
        item {
            Column {
                Text(
                    text = "SUPPORTED MARKETS",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MarketFilterChip(
                            label = "All Markets (${InstrumentsCatalog.ALL_INSTRUMENTS.size})",
                            isSelected = selectedMarketTab == null,
                            onClick = { selectedMarketTab = null }
                        )
                    }
                    items(MarketType.values()) { type ->
                        MarketFilterChip(
                            label = type.displayName,
                            isSelected = selectedMarketTab == type,
                            onClick = { selectedMarketTab = type }
                        )
                    }
                }
            }
        }

        // 5. Instruments List
        items(instruments) { inst ->
            val quote = quotes[inst.id]
            InstrumentRowCard(
                instrument = inst,
                quote = quote,
                onClick = { onSelectInstrument(inst) }
            )
        }
    }
}

@Composable
fun QuickHubButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkBlueGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MintGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SecondaryText, fontSize = 9.sp)
        }
    }
}

@Composable
fun MarketFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MintGreen else CardBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) DeepBackground else SecondaryText,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun InstrumentRowCard(
    instrument: Instrument,
    quote: Quote?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instrument.symbol,
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkBlueGray
                    ) {
                        Text(
                            text = instrument.marketType.displayName,
                            color = SecondaryText,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (quote != null) {
                        val badgeColor = when (quote.dataType) {
                            DataType.LIVE -> BullishGreen
                            DataType.DELAYED -> WarningGold
                            DataType.HISTORICAL -> AccentCyan
                            DataType.MARKET_CLOSED -> BearishRed
                        }
                        Text(
                            text = quote.dataType.label,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(" • ", color = SecondaryText, fontSize = 9.sp)
                    }
                    Text(
                        text = "${instrument.name} • ${instrument.tradingSession}",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = instrument.formatPrice(quote.mid),
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPositive = quote.change24hPercent >= 0
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", quote.change24hPercent)}%",
                            color = if (isPositive) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sprd: ${String.format(Locale.US, "%.1f", instrument.baseSpreadPipsOrTicks)}",
                            color = SecondaryText,
                            fontSize = 10.sp
                        )
                    }
                }
            } else {
                Text("...", color = SecondaryText, fontSize = 14.sp)
            }
        }
    }
}

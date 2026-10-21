package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
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
fun SettingsScreen(
    accountState: AccountState,
    currentTimezone: UserTimezone = UserTimezone.UTC,
    subscriptionState: UserSubscriptionState = UserSubscriptionState(),
    onNavigateToPremium: () -> Unit = {},
    onBack: () -> Unit,
    onResetAccountBalance: (Double) -> Unit,
    onSelectTimezone: (UserTimezone) -> Unit = {}
) {
    var selectedBalanceOption by remember { mutableStateOf(accountState.startingBalance) }
    var showResetDialog by remember { mutableStateOf(false) }

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
                Text("SETTINGS & CONFIGURATION", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Account Balance • Timezone • Simulation Rules", color = MintGreen, fontSize = 11.sp)
            }
        }

        // VIP Membership Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPremium() },
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (subscriptionState.isPremium) "AMEND TREND VIP ACTIVE" else "UPGRADE TO VIP",
                            color = if (subscriptionState.isPremium) MintGreen else PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (subscriptionState.isPremium) BullishGreen.copy(alpha = 0.2f) else MintGreen
                    ) {
                        Text(
                            text = if (subscriptionState.isPremium) "MANAGE" else "PLANS FROM $10",
                            color = if (subscriptionState.isPremium) MintGreen else DeepBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (subscriptionState.isPremium) {
                    Text(
                        text = "${subscriptionState.plan?.title ?: "VIP Member"} • ${subscriptionState.daysRemaining()} Days Remaining",
                        color = PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "All 9 Facilities Active: 1:500 Leverage, Multi-Timeframe AI Scan, HD Video Studio, NSE Options Chain, and Masterclasses.",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Unlock all facilities: Monthly $10 | 6 Months $60 | 1 Year $120. Institutional leverage, AI confluence scanner, and video narration.",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Virtual Starting Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VIRTUAL STARTING CAPITAL", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (subscriptionState.isPremium) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MintGreen.copy(alpha = 0.2f)) {
                            Text("VIP FACILITY: UP TO $1M", color = MintGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("Select initial paper trading balance. Resetting will clear open positions and restore balance.", color = SecondaryText, fontSize = 11.sp)

                val balanceOptions = if (subscriptionState.isPremium) {
                    listOf(10000.0, 50000.0, 100000.0, 250000.0, 500000.0, 1000000.0)
                } else {
                    listOf(5000.0, 10000.0, 25000.0, 50000.0, 100000.0)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    balanceOptions.forEach { amount ->
                        val isSel = selectedBalanceOption == amount
                        val label = if (amount >= 1000000) "$1M" else "$${(amount / 1000).toInt()}k"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) MintGreen else DarkBlueGray,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedBalanceOption = amount }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) DeepBackground else PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply & Reset to $${String.format(Locale.US, "%,.0f", selectedBalanceOption)}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Timezone Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DISPLAY TIMEZONE", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Timestamps are stored internally in UTC and converted dynamically to your selected display timezone.", color = SecondaryText, fontSize = 11.sp)

                for (tz in UserTimezone.values()) {
                    val isSel = currentTimezone == tz
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) DarkBlueGray else Color.Transparent)
                            .clickable { onSelectTimezone(tz) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelectTimezone(tz) },
                            colors = RadioButtonDefaults.colors(selectedColor = MintGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(tz.displayName, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Current converted time: ${tz.formatTimestamp(System.currentTimeMillis())}", color = if (isSel) MintGreen else SecondaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Licensed Providers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("LICENSED MARKET PROVIDER INTEGRATIONS", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("• Cryptocurrency: Binance Public Market Data API (Live 24/7 Spot Tickers & Klines)", color = SecondaryText, fontSize = 11.sp)
                Text("• Forex: Frankfurter European Central Bank (ECB) Reference Feed & Interbank Spreads", color = SecondaryText, fontSize = 11.sp)
                Text("• Indian Equities & Indices: NSE India Real-Time & EOD Session Gateway", color = SecondaryText, fontSize = 11.sp)
                Text("• Gap Healing & Offline Resilience: Automatic linear candle interpolation and rate-limit backoff", color = SecondaryText, fontSize = 11.sp)
            }
        }

        // Transparent Execution Rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULATED EXECUTION POLICY", color = MintGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("• Forex: Buy fills at Ask, Sell fills at Bid; Long closes at Bid, Short closes at Ask.", color = SecondaryText, fontSize = 11.sp)
                Text("• NSE Equities: Enforces ₹0.05 tick size and Indian exchange session constraints.", color = SecondaryText, fontSize = 11.sp)
                Text("• Crypto: Spot 24/7 simulation with 0.06% fees and USDT quote peg.", color = SecondaryText, fontSize = 11.sp)
                Text("• Zero Lookahead Bias: Historical replay steps candles forward deterministically.", color = SecondaryText, fontSize = 11.sp)
            }
        }

        // About Amend Trend
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AmendTrendLogo(size = 40.dp, horizontal = false)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirm Account Reset", color = PrimaryText) },
            text = { Text("Reset account starting balance to $${String.format(Locale.US, "%,.0f", selectedBalanceOption)} USD? All open positions will be closed.", color = SecondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccountBalance(selectedBalanceOption)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("Reset Now", color = Color.White)
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

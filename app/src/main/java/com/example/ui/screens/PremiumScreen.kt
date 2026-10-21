package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AmendTrendLogo
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumScreen(
    subscriptionState: UserSubscriptionState,
    onBack: () -> Unit,
    onSubscribe: (SubscriptionPlan) -> Unit,
    onCancelSubscription: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    BackHandler { onBack() }

    var selectedPlan by remember {
        mutableStateOf(subscriptionState.plan ?: SubscriptionPlan.YEARLY)
    }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_premium")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "AMEND TREND VIP",
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "STYLE SHIFT • UNLOCK ALL FACILITIES",
                        color = MintGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hero Brand Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AmendTrendLogo(size = 54.dp, horizontal = false)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (subscriptionState.isPremium) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BullishGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE: ${subscriptionState.plan?.title ?: "VIP Institutional"}",
                                    color = MintGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${subscriptionState.daysRemaining()} days remaining (Expires ${dateFormat.format(Date(subscriptionState.expiryDate))})",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Transaction ID: ${subscriptionState.transactionId}",
                            color = SecondaryText.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DarkBlueGray,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MintGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "★ PROFESSIONAL TRADING SUITE ★",
                                color = MintGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Elevate From Novice to Institutional Precision",
                            color = PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Unlock multi-timeframe AI market scanning, institutional leverage up to 1:500, AI video studio with voice narration, and full Indian NSE Options Chain.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        // Subscription Plans Section (Monthly $10, 6-Monthly $60, Yearly $120)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CHOOSE YOUR PLAN",
                    color = MintGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                SubscriptionPlan.values().forEach { plan ->
                    val isSelected = selectedPlan == plan
                    val isCurrentActive = subscriptionState.isPremium && subscriptionState.plan == plan

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MintGreen else DarkBlueGray,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedPlan = plan }
                            .testTag("plan_${plan.id.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CardBackground else CardBackground.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlan = plan },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MintGreen,
                                            unselectedColor = SecondaryText
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = plan.title,
                                            color = PrimaryText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = plan.billingCycleDisplay,
                                            color = SecondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = plan.priceDisplay,
                                        color = MintGreen,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    plan.badge?.let { badge ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MintGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = badge,
                                                color = MintGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            plan.savingsText?.let { savings ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DarkBlueGray.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MintGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = savings, color = SecondaryText, fontSize = 10.sp)
                                    }
                                }
                            }

                            if (isCurrentActive) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✓ Currently Active Plan",
                                    color = BullishGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Subscribe / Change Plan / Cancel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showCheckoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_subscribe_checkout"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Icon(
                        imageVector = if (subscriptionState.isPremium) Icons.Default.SwapHoriz else Icons.Default.Stars,
                        contentDescription = null,
                        tint = DeepBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (subscriptionState.isPremium) {
                            "Switch to ${selectedPlan.title} (${selectedPlan.priceDisplay})"
                        } else {
                            "Get ${selectedPlan.title} • ${selectedPlan.priceDisplay}"
                        },
                        color = DeepBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onRestorePurchases,
                        modifier = Modifier.testTag("btn_restore_purchases")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Purchases", color = SecondaryText, fontSize = 12.sp)
                    }

                    if (subscriptionState.isPremium) {
                        TextButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.testTag("btn_cancel_subscription")
                        ) {
                            Text("Cancel Subscription", color = BearishRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // All Facilities & Privileges Breakdown
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREMIUM FACILITIES & CAPABILITIES",
                        color = MintGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkBlueGray
                    ) {
                        Text(
                            text = "9 FACILITIES",
                            color = SecondaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Compare the enhanced tools provided to master Forex, Indian markets, indices, and crypto:",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }

        // Facilities List
        items(PremiumFacilitiesCatalog.ALL_FACILITIES) { facility ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkBlueGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (facility.iconName) {
                                "Analytics" -> Icons.Default.Analytics
                                "Bolt" -> Icons.Default.Bolt
                                "AccountBalance" -> Icons.Default.AccountBalance
                                "Paid" -> Icons.Default.Paid
                                "Videocam" -> Icons.Default.Videocam
                                "QueryStats" -> Icons.Default.QueryStats
                                "School" -> Icons.Default.School
                                "Psychology" -> Icons.Default.Psychology
                                else -> Icons.Default.FileDownload
                            },
                            contentDescription = null,
                            tint = MintGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = facility.title,
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MintGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = facility.category,
                                    color = MintGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = facility.description,
                            color = SecondaryText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Free vs Premium Side-by-Side Comparison Matrix
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlueGray)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FREE TIER VS. PREMIUM VIP SUMMARY",
                        color = MintGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ComparisonRow(feature = "Forex & Crypto Real-time Ticks", free = "Standard", pro = "Ultra-Low Latency (<100ms)")
                    ComparisonRow(feature = "AI Market Scanner", free = "1H & 1D only", pro = "1m, 5m, 15m, 1h, 4h, 1D All")
                    ComparisonRow(feature = "SMC Confluence Matrix", free = "Single timeframe", pro = "Multi-Timeframe 6-in-1")
                    ComparisonRow(feature = "Forex Max Leverage", free = "1:30 Retail", pro = "Up to 1:500 Institutional")
                    ComparisonRow(feature = "Paper Starting Balance", free = "$10,000 fixed", pro = "Up to $1,000,000 Custom")
                    ComparisonRow(feature = "AI Video Studio", free = "Single scene test", pro = "Full 60fps HD + Voice Narration")
                    ComparisonRow(feature = "Indian NSE Options Chain", free = "Basic Spot Only", pro = "Greeks & Max Pain Radar")
                    ComparisonRow(feature = "Trading Academy", free = "Basics Only", pro = "All Advanced Masterclasses")
                }
            }
        }

        // Trust & Guarantee Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔒 Secured via Google Play Billing",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Cancel anytime in Google Play Store subscriptions. Auto-renews unless cancelled at least 24 hours prior to cycle end.",
                    color = SecondaryText.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Google Play Simulated Checkout Dialog
    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MintGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Play Purchase",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Confirm subscription to ${selectedPlan.title}",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkBlueGray,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Product:", color = SecondaryText, fontSize = 11.sp)
                                Text("Amend Trend VIP (${selectedPlan.durationLabel})", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Price:", color = SecondaryText, fontSize = 11.sp)
                                Text(selectedPlan.priceDisplay, color = MintGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Billing:", color = SecondaryText, fontSize = 11.sp)
                                Text(selectedPlan.billingCycleDisplay, color = SecondaryText, fontSize = 10.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Method:", color = SecondaryText, fontSize = 11.sp)
                                Text("Google Play Balance / Card", color = PrimaryText, fontSize = 11.sp)
                            }
                        }
                    }

                    Text(
                        text = "Instant unlock of all 9 facilities: Multi-Timeframe AI Confluence, 1:500 Leverage, HD Video Studio, NSE Options Chain, and ICT Masterclasses.",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubscribe(selectedPlan)
                        showCheckoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    modifier = Modifier.testTag("btn_confirm_purchase")
                ) {
                    Text("Subscribe • ${selectedPlan.priceDisplay}", color = DeepBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = CardBackground
        )
    }

    // Cancel Subscription Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel VIP Subscription?", color = PrimaryText) },
            text = {
                Text(
                    text = "If you cancel, you will retain your facilities until ${dateFormat.format(Date(subscriptionState.expiryDate))}. After that, your account will revert to the standard Free tier.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSubscription()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("Confirm Cancellation", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep My Plan", color = MintGreen)
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
private fun ComparisonRow(feature: String, free: String, pro: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(text = feature, color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.weight(0.9f)) {
            Text(text = free, color = SecondaryText, fontSize = 10.sp)
        }
        Column(modifier = Modifier.weight(1.3f)) {
            Text(text = pro, color = MintGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

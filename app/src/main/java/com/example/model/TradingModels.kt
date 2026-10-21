package com.example.model

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceUsd: Double,
    val billingSubtitle: String,
    val badge: String,
    val durationDays: Int
) {
    FREE("free", "Free Starter", 0.0, "Basic paper trading and education", "STARTER", 0),
    MONTHLY("monthly", "Pro Monthly", 10.0, "$10 / month • Cancel anytime", "POPULAR", 30),
    SIX_MONTH("six_month", "Pro 6-Month", 60.0, "$60 semi-annually ($10/mo) • Save on commitment", "MOST POPULAR", 180),
    ANNUAL("annual", "Pro Annual VIP", 120.0, "$120 / year ($10/mo) • Full institutional suite", "VIP INSTITUTIONAL", 365)
}

data class AccountState(
    val balance: Double = 10000.0,
    val startingBalance: Double = 10000.0,
    val currency: String = "USD",
    val realizedPnL: Double = 0.0,
    val unrealizedPnL: Double = 0.0,
    val usedMargin: Double = 0.0,
    val isProUser: Boolean = false,
    val subscriptionTier: String = "FREE",
    val subscriptionExpiresAt: Long = 0L
) {
    val equity: Double get() = balance + unrealizedPnL
    val freeMargin: Double get() = (equity - usedMargin).coerceAtLeast(0.0)
    val marginLevelPercent: Double
        get() = if (usedMargin > 0.0) (equity / usedMargin) * 100.0 else 0.0
    val isSubscriptionActive: Boolean
        get() = isProUser && (subscriptionExpiresAt == 0L || subscriptionExpiresAt > System.currentTimeMillis())
}

data class Order(
    val id: String = java.util.UUID.randomUUID().toString(),
    val instrumentId: String,
    val marketType: MarketType,
    val direction: TradeDirection,
    val orderType: OrderType,
    val quantity: Double, // Lot size for Forex, shares/contracts for NSE, coins for Crypto
    val targetPrice: Double? = null, // for LIMIT / STOP orders
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

data class Position(
    val id: String = java.util.UUID.randomUUID().toString(),
    val instrumentId: String,
    val marketType: MarketType,
    val direction: TradeDirection,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val unrealizedPnL: Double = 0.0,
    val realizedPnL: Double = 0.0,
    val margin: Double = 0.0,
    val openTime: Long = System.currentTimeMillis(),
    val tradeNotes: String = ""
)

data class TradeLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val positionId: String,
    val instrumentId: String,
    val marketType: MarketType,
    val direction: TradeDirection,
    val quantity: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val grossPnL: Double,
    val fees: Double,
    val netPnL: Double,
    val pipsOrPoints: Double,
    val exitReason: String, // "TP HIT", "SL HIT", "MANUAL_CLOSE", "PARTIAL_EXIT"
    val openTime: Long,
    val closeTime: Long = System.currentTimeMillis(),
    val ruleComplianceScore: Int = 100, // 0 - 100
    val userTradeIdea: String = "",
    val aiReview: String = ""
)

data class RiskCalculationResult(
    val instrumentId: String,
    val direction: TradeDirection,
    val entryPrice: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double?,
    val riskPercent: Double,
    val monetaryRisk: Double,
    val recommendedQuantityOrLots: Double,
    val potentialReward: Double,
    val rewardToRiskRatio: Double,
    val estimatedMargin: Double,
    val estimatedFee: Double,
    val stopDistancePipsOrTicks: Double,
    val isValid: Boolean,
    val validationError: String? = null
)

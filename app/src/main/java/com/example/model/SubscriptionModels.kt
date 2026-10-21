package com.example.model

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val durationLabel: String,
    val priceDisplay: String,
    val billingCycleDisplay: String,
    val priceUsd: Double,
    val durationDays: Int,
    val badge: String?,
    val savingsText: String?
) {
    MONTHLY(
        id = "MONTHLY",
        title = "Monthly Pass",
        durationLabel = "1 Month",
        priceDisplay = "$10",
        billingCycleDisplay = "Billed monthly ($10/mo)",
        priceUsd = 10.0,
        durationDays = 30,
        badge = null,
        savingsText = "Flexible month-to-month access"
    ),
    SIX_MONTHS(
        id = "SIX_MONTHS",
        title = "6-Month Pro",
        durationLabel = "6 Months",
        priceDisplay = "$60",
        billingCycleDisplay = "Billed every 6 months ($10/mo)",
        priceUsd = 60.0,
        durationDays = 180,
        badge = "POPULAR",
        savingsText = "Includes Bonus Multi-Timeframe Scanner"
    ),
    YEARLY(
        id = "YEARLY",
        title = "VIP Institutional",
        durationLabel = "1 Year",
        priceDisplay = "$120",
        billingCycleDisplay = "Billed annually ($10/mo)",
        priceUsd = 120.0,
        durationDays = 365,
        badge = "BEST VALUE • ALL FACILITIES",
        savingsText = "All Masterclasses & 1:500 Leverage Unlocked"
    )
}

data class PremiumFacility(
    val iconName: String,
    val title: String,
    val description: String,
    val category: String,
    val isVipOnly: Boolean = false // Available on 6-month & yearly or yearly only
)

object PremiumFacilitiesCatalog {
    val ALL_FACILITIES = listOf(
        PremiumFacility(
            iconName = "Analytics",
            title = "Multi-Timeframe AI Confluence Scanner",
            description = "Simultaneous 1m, 5m, 15m, 1h, 4h, and Daily institutional market scan with overall directional consensus percentage.",
            category = "AI Market Analysis"
        ),
        PremiumFacility(
            iconName = "Bolt",
            title = "Fair Value Gap & Liquidity Sweep Radar",
            description = "Real-time institutional footprint alerts pinpointing retail stop runs, order blocks, and unmitigated imbalance zones.",
            category = "AI Market Analysis"
        ),
        PremiumFacility(
            iconName = "AccountBalance",
            title = "Institutional Leverage (Up to 1:500)",
            description = "Trade Forex with 1:100 to 1:500 leverage and Crypto with up to 1:20 leverage in paper trading simulation.",
            category = "Paper Trading"
        ),
        PremiumFacility(
            iconName = "Paid",
            title = "Custom Starting Capital up to $1,000,000",
            description = "Simulate institutional hedge fund portfolios or test $100k, $250k, and $1M prop firm evaluation challenges.",
            category = "Paper Trading"
        ),
        PremiumFacility(
            iconName = "Videocam",
            title = "AI Video Studio HD Narration & Scripts",
            description = "Generate 60fps chart playback videos with AI voice narration in 16:9 (YouTube) & 9:16 (Shorts/Reels) with full script exports.",
            category = "Video & Media"
        ),
        PremiumFacility(
            iconName = "QueryStats",
            title = "Indian NSE Options Chain & Greeks Matrix",
            description = "Live simulated options Greeks (Delta, Gamma, Theta, Vega) and Max Pain levels for NIFTY 50 and NIFTY BANK.",
            category = "Indian Markets"
        ),
        PremiumFacility(
            iconName = "School",
            title = "ICT & SMC Institutional Masterclasses",
            description = "In-depth modules on Central Bank Dealers Range, Footprint Delta, and prop firm risk management with certified completion badge.",
            category = "Academy"
        ),
        PremiumFacility(
            iconName = "Psychology",
            title = "Personalized Gemini AI Psychology Coach",
            description = "Automated post-trade psychological review to detect revenge trading, FOMO, and over-leveraging with actionable guidance.",
            category = "Trading Psychology"
        ),
        PremiumFacility(
            iconName = "FileDownload",
            title = "CSV Trading Journal & Ledger Export",
            description = "Export complete auditable trade history, fills, fees, and P&L logs for external analytics or tax reporting.",
            category = "Analytics & Reports"
        )
    )
}

data class UserSubscriptionState(
    val isPremium: Boolean = false,
    val plan: SubscriptionPlan? = null,
    val startDate: Long = 0L,
    val expiryDate: Long = 0L,
    val autoRenew: Boolean = true,
    val transactionId: String = ""
) {
    fun daysRemaining(): Long {
        if (!isPremium || expiryDate <= 0) return 0
        val diff = expiryDate - System.currentTimeMillis()
        return if (diff > 0) (diff / (1000 * 60 * 60 * 24)) + 1 else 0
    }

    fun isExpired(): Boolean = isPremium && expiryDate > 0 && System.currentTimeMillis() > expiryDate

    fun maxLeverageForex(): Int = if (isPremium) 500 else 30
    fun maxLeverageCrypto(): Int = if (isPremium) 20 else 1
    fun canAccessMultiTimeframeScan(): Boolean = isPremium
    fun canAccessOptionsChain(): Boolean = isPremium
    fun canAccessAdvancedMasterclass(): Boolean = isPremium
    fun canAccessFullVideoStudio(): Boolean = isPremium
}

package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.LessonProgressEntity
import com.example.model.Lesson
import com.example.model.QuizQuestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EducationRepository(private val db: AppDatabase) {
    private val educationDao = db.educationDao()

    val progressFlow: Flow<Map<String, Boolean>> = educationDao.getAllProgressFlow().map { list ->
        list.associate { it.lessonId to it.isCompleted }
    }

    suspend fun markLessonCompleted(lessonId: String, score: Int) {
        educationDao.saveProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                isCompleted = true,
                quizScore = score,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    fun getAllLessons(): List<Lesson> = listOf(
        Lesson(
            id = "forex_basics",
            category = "Forex",
            title = "Forex Fundamentals & Math",
            subtitle = "Pips, Lots, Base/Quote Currencies, and Leverage",
            readTimeMinutes = 5,
            contentMarkdown = """
                ### What is Foreign Exchange?
                In Forex trading, currencies are traded in pairs such as EUR/USD. The first currency is the **Base Currency**, and the second is the **Quote Currency**.
                
                ### The Pip
                A **Pip** (Percentage in Point) is typically the 4th decimal place (0.0001) for major pairs, except for JPY pairs where it is the 2nd decimal place (0.01).
                
                ### Lot Sizes
                * **Standard Lot (1.00)**: 100,000 units (~$10 per pip on EUR/USD)
                * **Mini Lot (0.10)**: 10,000 units (~$1 per pip)
                * **Micro Lot (0.01)**: 1,000 units (~$0.10 per pip)
                
                ### Execution Rule
                * To open a **BUY**, you pay the **Ask** price.
                * To open a **SELL**, you receive the **Bid** price.
                * Long positions close at **Bid**, Short positions close at **Ask**.
            """.trimIndent(),
            keyTakeaways = listOf(
                "EUR/USD 1 standard lot = $10/pip movement",
                "Buy entries fill at Ask; Sell entries fill at Bid",
                "JPY pairs use 0.01 as 1 pip"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q1",
                    question = "On EUR/USD, if you trade 1 standard lot, approximately how much is 1 pip worth in USD?",
                    options = listOf("$1.00", "$10.00", "$100.00", "$0.10"),
                    correctIndex = 1,
                    explanation = "1 standard lot equals 100,000 units. 100,000 * 0.0001 = $10.00 per pip."
                ),
                QuizQuestion(
                    id = "q2",
                    question = "When entering a Long (Buy) market order, at which price is your order executed?",
                    options = listOf("Bid Price", "Ask Price", "Mid Price", "Last Closed Candle"),
                    correctIndex = 1,
                    explanation = "Buyers buy from the market at the Ask price, while Sellers sell to the market at the Bid price."
                )
            )
        ),
        Lesson(
            id = "indian_markets",
            category = "Indian Markets",
            title = "NSE Equities & Sessions",
            subtitle = "Tick Sizes, Trading Hours, and Index Derivatives",
            readTimeMinutes = 6,
            contentMarkdown = """
                ### NSE India Market Structure
                The National Stock Exchange of India (NSE) is the primary venue for trading equities such as Reliance, TCS, and Infosys, along with flagship indices NIFTY 50 and NIFTY BANK.
                
                ### Market Hours
                * **Pre-open Session**: 09:00 - 09:08 IST
                * **Normal Market Open**: 09:15 - 15:30 IST
                
                ### Tick Size & Quantities
                * Standard tick size for NSE equities is **₹0.05**.
                * Single stocks can be bought in quantities of 1 share.
                * Index futures and options trade in standardized lots: **NIFTY 50 = 25 shares/lot**, **BANK NIFTY = 15 shares/lot**.
                
                ### Trading Discipline
                Always simulate trades considering exchange brokerage, STT, and tick boundaries.
            """.trimIndent(),
            keyTakeaways = listOf(
                "NSE core trading hours run 09:15 AM to 03:30 PM IST",
                "Equity tick size is ₹0.05",
                "Nifty 50 lot size is 25; Bank Nifty lot size is 15"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_nse_1",
                    question = "What is the minimum price increment (tick size) for NSE equities?",
                    options = listOf("₹0.01", "₹0.05", "₹0.25", "₹1.00"),
                    correctIndex = 1,
                    explanation = "Indian cash equity stocks on the NSE trade in ticks of ₹0.05."
                ),
                QuizQuestion(
                    id = "q_nse_2",
                    question = "Can a retail trader directly buy 1 unit of the cash index NIFTY 50 directly?",
                    options = listOf("Yes, anytime", "No, indices must be traded via simulated ETFs, Futures, or Options", "Only on Thursdays", "Only after 3:30 PM"),
                    correctIndex = 1,
                    explanation = "Stock indices are composite statistical calculations and cannot be bought directly without derivative contracts or ETFs."
                )
            )
        ),
        Lesson(
            id = "crypto_mechanics",
            category = "Crypto",
            title = "Cryptocurrency Mechanics",
            subtitle = "24/7 Liquidity, Volatility, and Funding Rates",
            readTimeMinutes = 5,
            contentMarkdown = """
                ### 24/7 Global Trading
                Unlike traditional equities or Forex with weekend closures, Crypto operates continuously across global venues.
                
                ### USDT Base Pairs
                Most liquid trading occurs against stablecoins like Tether (USDT), which are pegged 1:1 to the US Dollar.
                
                ### Liquidity & Slippage
                Due to rapid price shifts, high slippage can occur during sudden volatility. Proper limit orders protect against unfavorable fills.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Crypto trades 24 hours a day, 7 days a week",
                "USDT pairs represent virtual US dollar denomination",
                "Always check order book liquidity to avoid slippage"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_cr_1",
                    question = "When does the cryptocurrency spot market close for the weekend?",
                    options = listOf("Friday 5pm EST", "Saturday midnight", "Never; crypto trades 24/7/365", "Sunday 8pm UTC"),
                    correctIndex = 2,
                    explanation = "Cryptocurrency spot markets operate 24 hours a day, 7 days a week with no market close."
                )
            )
        ),
        Lesson(
            id = "market_structure",
            category = "Technical Analysis",
            title = "Market Structure: HH, HL, LH, LL",
            subtitle = "Mastering Institutional Trends and Swings",
            readTimeMinutes = 7,
            contentMarkdown = """
                ### What is Market Structure?
                Market structure is the visual record of price swings over time.
                
                ### Bullish Structure (Uptrend)
                * **Higher High (HH)**: Price rises above the previous peak.
                * **Higher Low (HL)**: Retracement stays above the previous trough.
                
                ### Bearish Structure (Downtrend)
                * **Lower High (LH)**: Rally fails to exceed the previous high.
                * **Lower Low (LL)**: Drop plunges below the previous low.
                
                ### The Golden Rule
                Never enter a trade against current timeframe structure without confirmed Change of Character (CHoCH).
            """.trimIndent(),
            keyTakeaways = listOf(
                "Uptrends are defined by consecutive Higher Highs and Higher Lows",
                "Downtrends are defined by Lower Highs and Lower Lows",
                "Trade with structural direction for the highest probability"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_ms_1",
                    question = "What two characteristics define a healthy bullish market trend?",
                    options = listOf("Lower Highs and Lower Lows", "Higher Highs and Higher Lows", "Equal Highs and Equal Lows", "Flat moving averages"),
                    correctIndex = 1,
                    explanation = "A bullish market structure is confirmed when price consistently establishes Higher Highs (HH) followed by Higher Lows (HL)."
                )
            )
        ),
        Lesson(
            id = "smc_bos_choch",
            category = "Smart Money Concepts",
            title = "Break of Structure (BOS) vs CHoCH",
            subtitle = "Spotting Trend Continuations and Reversals",
            readTimeMinutes = 8,
            contentMarkdown = """
                ### Break of Structure (BOS)
                A **BOS** is a trend *continuation* signal. When price in an established uptrend closes above the last Higher High, a bullish BOS is printed.
                
                ### Change of Character (CHoCH)
                A **CHoCH** is a trend *reversal* signal. When an uptrend fails and price aggressively breaks below the previous Higher Low, character has changed to bearish.
                
                ### Wick vs Body Close
                Institutional confirmation requires a full **candle body close** across the swing level. A mere wick piercing the level is considered a **Liquidity Sweep**!
            """.trimIndent(),
            keyTakeaways = listOf(
                "BOS signifies trend continuation",
                "CHoCH signifies the first signal of structural trend reversal",
                "Candle body closes confirm BOS; wicks indicate liquidity sweeps"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_smc_1",
                    question = "If an uptrending market breaks below its most recent Higher Low with a full candle body close, what has occurred?",
                    options = listOf("A continuation BOS", "A Change of Character (CHoCH)", "A False Alarm", "An inside bar"),
                    correctIndex = 1,
                    explanation = "When price breaks the structural low of an uptrend, it signals a Change of Character (CHoCH) and potential shift to a bearish trend."
                )
            )
        ),
        Lesson(
            id = "fvg_order_blocks",
            category = "Smart Money Concepts",
            title = "Fair Value Gaps & Order Blocks",
            subtitle = "Locating Institutional Entry Footprints",
            readTimeMinutes = 8,
            contentMarkdown = """
                ### Fair Value Gaps (FVG)
                A Fair Value Gap is a 3-candle imbalance where aggressive buying or selling creates a price void:
                * **Bullish FVG**: Candle 1 High < Candle 3 Low. The space in between is an unmitigated imbalance zone where price often returns to retest.
                
                ### Order Blocks (OB)
                An Order Block is the last opposing candle prior to a violent impulse move that causes a BOS or CHoCH.
                * Banks accumulate or distribute orders here before driving price.
            """.trimIndent(),
            keyTakeaways = listOf(
                "FVG is an imbalance between Candle 1 and Candle 3 wicks",
                "Order Blocks act as high-probability retest zones",
                "Combine FVG with Order Blocks for precision entries"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_fvg_1",
                    question = "What constitutes a 3-candle Bullish Fair Value Gap (FVG)?",
                    options = listOf(
                        "Candle 3 closes higher than Candle 1",
                        "The high of Candle 1 does not overlap with the low of Candle 3",
                        "All three candles are red",
                        "Volume is zero on Candle 2"
                    ),
                    correctIndex = 1,
                    explanation = "A bullish FVG occurs when the high of the 1st candle leaves an unfilled gap with the low of the 3rd candle due to aggressive displacement in the 2nd candle."
                )
            )
        ),
        Lesson(
            id = "risk_psychology",
            category = "Risk Management",
            title = "The 1% Risk Rule & Psychology",
            subtitle = "Protecting Capital and Emotional Equilibrium",
            readTimeMinutes = 6,
            contentMarkdown = """
                ### The 1% Rule
                Never risk more than **1% to 2%** of your total account equity on any single trade.
                * If your account is $10,000, your maximum monetary risk on a 1% setup is **$100**.
                
                ### Position Sizing Formula
                Position Size = Monetary Risk / (Stop Loss Distance in Pips/Points * Pip Value)
                
                ### Trading Psychology
                A loss that complies with your risk plan is a **GOOD TRADE**. A win that violated your stop loss or over-leveraged is a **DANGEROUS TRADE** that will destroy long-term capital.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Cap single-trade risk at 1% of account equity",
                "Adjust position size to stop loss distance, not vice versa",
                "Follow trading rules independently of outcome emotion"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_rk_1",
                    question = "On a $10,000 trading account, adhering to a 1% risk rule, what is the maximum dollar amount you should risk on one trade?",
                    options = listOf("$500", "$200", "$100", "$1,000"),
                    correctIndex = 2,
                    explanation = "1% of $10,000 is exactly $100."
                )
            )
        ),
        Lesson(
            id = "ict_cbdr_masterclass",
            category = "VIP Masterclass",
            title = "Interbank Price Delivery & CBDR",
            subtitle = "Central Bank Dealers Range & Judas Swings",
            readTimeMinutes = 10,
            isVipOnly = true,
            contentMarkdown = """
                ### Central Bank Dealers Range (CBDR)
                The Central Bank Dealers Range is measured between 14:00 and 20:00 New York time.
                * It establishes the liquidity benchmark for algorithmic market expansion in the London session.
                
                ### The Judas Swing (False Run)
                Algorithms frequently deliver an aggressive run opposite to true directional order flow during London Open (02:00 - 05:00 NY time):
                * In a bullish setup: Price sweeps below Asian session lows or CBDR low, collects sell-side liquidity, and violently reverses upward.
                * In a bearish setup: Price pushes above Asian highs into buy-side liquidity before driving down into true daily bias.
                
                ### Execution Confluence
                Look for:
                1. Higher Timeframe (4H/Daily) Order Block mitigation.
                2. Lower Timeframe (M15/M5) Liquidity sweep of CBDR boundary.
                3. Market Structure Shift with Displacement leaving an unmitigated FVG.
            """.trimIndent(),
            keyTakeaways = listOf(
                "CBDR benchmark forms baseline for London algorithmic expansion",
                "Judas Swings engineer retail stop hunts before true directional moves",
                "Enter on FVG retracement post-liquidity purge"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_cbdr_1",
                    question = "What is the primary function of an ICT 'Judas Swing' during London Open?",
                    options = listOf(
                        "To initiate a long-term multi-week trend",
                        "To engineer liquidity by trapping breakout traders and triggering retail stops before the genuine move",
                        "To close all interbank orders before the weekend",
                        "To stabilize spreads during news releases"
                    ),
                    correctIndex = 1,
                    explanation = "The Judas Swing is an intentional institutional liquidity run designed to lure retail breakout traders and hunt stop losses before the true expansion."
                )
            )
        ),
        Lesson(
            id = "options_greeks_masterclass",
            category = "VIP Masterclass",
            title = "NSE Options Greeks & Gamma Scalping",
            subtitle = "Delta, Gamma, Theta Decay, and Max Pain Levels",
            readTimeMinutes = 9,
            isVipOnly = true,
            contentMarkdown = """
                ### The Core Options Greeks
                * **Delta (Δ)**: Rate of change of option price per ₹1 move in the underlying index (NIFTY 50 or BANK NIFTY). ATM options have ~0.50 Delta.
                * **Gamma (Γ)**: Acceleration of Delta. Gamma is highest near ATM and peaks drastically on weekly expiry Thursdays.
                * **Theta (Θ)**: Time decay. Accelerated decay occurs within the final 48 hours before expiry.
                * **Vega (ν)**: Sensitivity to implied volatility (India VIX) shifts.
                
                ### Max Pain Theory
                Max Pain is the strike price where option buyers collectively stand to lose the most money upon expiry.
                * Institutional market makers and option writers tend to hedge delta dynamically, gravitating the closing index price toward the Max Pain level by 03:30 PM on expiry day.
            """.trimIndent(),
            keyTakeaways = listOf(
                "ATM options carry ~0.50 Delta; Delta accelerates with Gamma",
                "Option sellers profit from accelerated Theta decay approaching expiry",
                "Underlying index price often clusters near Max Pain on expiry"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_greeks_1",
                    question = "Which option Greek measures the rate of decay in an option premium purely due to the passage of time?",
                    options = listOf("Delta", "Gamma", "Theta", "Vega"),
                    correctIndex = 2,
                    explanation = "Theta represents the dollar/rupee loss in an option value per passing day (time decay)."
                )
            )
        ),
        Lesson(
            id = "prop_firm_masterclass",
            category = "VIP Masterclass",
            title = "$100k+ Prop Firm Challenge Sizing",
            subtitle = "Strict Daily Drawdown, Trailing High Water Marks, and Consistency",
            readTimeMinutes = 8,
            isVipOnly = true,
            contentMarkdown = """
                ### Prop Firm Evaluation Rules
                Modern proprietary trading firms (e.g. FTMO, FundedNext) enforce strict mathematical risk boundaries:
                * **Maximum Daily Loss**: Typically 5% based on previous day's balance or equity high water mark.
                * **Maximum Overall Loss**: Typically 8% - 10% trailing or static drawdown.
                * **Profit Target**: 8% to 10% in Phase 1, 5% in Phase 2.
                
                ### Asymmetric Risk Management
                * Risk **0.5% to 0.75%** per trade, never 2%+.
                * If 2 consecutive losses occur in a single trading session, stop trading immediately to preserve daily buffer.
                * Only take A+ confluence setups with minimum 1:2.5 Risk-to-Reward.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Cap risk at 0.5% per trade to survive standard 5% daily drawdown limits",
                "Honor hard stop rules after 2 consecutive daily losses",
                "Focus on consistency and low drawdown rather than rushed profit targets"
            ),
            quiz = listOf(
                QuizQuestion(
                    id = "q_prop_1",
                    question = "On a $100,000 funded account with a strict 5% max daily drawdown ($5,000), what is the safest risk allocation per trade?",
                    options = listOf("3% ($3,000)", "0.5% to 0.75% ($500 - $750)", "5% ($5,000)", "10% ($10,000)"),
                    correctIndex = 1,
                    explanation = "A 0.5% to 0.75% risk per trade gives you a safe 7 to 10 trade loss buffer before hitting the 5% daily limit."
                )
            )
        )
    )
}

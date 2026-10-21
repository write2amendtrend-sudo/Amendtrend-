package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.model.*
import com.example.ui.components.AmendTrendLogo
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class Screen {
    object Dashboard : Screen()
    data class MarketDetail(val instrument: Instrument) : Screen()
    object PaperTrading : Screen()
    data class AiAnalyst(val instrument: Instrument? = null) : Screen()
    data class RiskCalculator(val instrument: Instrument? = null) : Screen()
    data class VideoStudio(val instrument: Instrument? = null) : Screen()
    object Education : Screen()
    object Performance : Screen()
    object Settings : Screen()
    object PremiumMembership : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var marketDataRepo: MarketDataRepository
    private lateinit var tradingRepo: TradingRepository
    private lateinit var aiAnalystRepo: AiAnalystRepository
    private lateinit var educationRepo: EducationRepository
    private lateinit var mediaStudioRepo: MediaStudioRepository
    private lateinit var subscriptionRepo: SubscriptionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = AppDatabase.getInstance(this)
        marketDataRepo = MarketDataRepository()
        tradingRepo = TradingRepository(db, marketDataRepo)
        aiAnalystRepo = AiAnalystRepository(marketDataRepo)
        educationRepo = EducationRepository(db)
        mediaStudioRepo = MediaStudioRepository(db)
        subscriptionRepo = SubscriptionRepository(db)

        // Initialize default $10,000 balance
        lifecycleScope.launch {
            tradingRepo.initializeAccountIfNotExists(10000.0)
        }

        // Background price tick loop to simulate real market order flow & evaluate SL/TP
        lifecycleScope.launch {
            var counter = 0
            while (isActive) {
                delay(2500)
                counter++
                // Randomly tick 2 instruments
                val inst1 = InstrumentsCatalog.ALL_INSTRUMENTS.random()
                val inst2 = InstrumentsCatalog.ALL_INSTRUMENTS.random()
                marketDataRepo.tickPrice(inst1.id)
                marketDataRepo.tickPrice(inst2.id)

                // Every 15 seconds (6 ticks), sync from live licensed external providers
                if (counter % 6 == 0) {
                    marketDataRepo.syncFromLicensedProviders()
                }
            }
        }

        setContent {
            AmendTrendTheme {
                val accountState by tradingRepo.accountFlow.collectAsState(initial = AccountState())
                val quotes by marketDataRepo.quotesFlow.collectAsState(initial = emptyMap())
                val openPositions by tradingRepo.openPositionsFlow.collectAsState(initial = emptyList())
                val pendingOrders by tradingRepo.pendingOrdersFlow.collectAsState(initial = emptyList())
                val tradeLogs by tradingRepo.tradeLogsFlow.collectAsState(initial = emptyList())
                val connectionStatus by marketDataRepo.connectionStatusFlow.collectAsState(initial = ConnectionStatus.CONNECTED)
                val userTimezone by marketDataRepo.userTimezoneFlow.collectAsState(initial = UserTimezone.UTC)

                // Evaluate open positions periodically when quotes change
                LaunchedEffect(quotes) {
                    tradingRepo.evaluatePositionsAndOrders(quotes)
                }

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                val coroutineScope = rememberCoroutineScope()

                // Back navigation handling
                BackHandler(enabled = currentScreen !is Screen.Dashboard) {
                    currentScreen = Screen.Dashboard
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepBackground,
                    topBar = {
                        TopAppBar(
                            title = {
                                AmendTrendLogo(size = 32.dp, showTagline = false)
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentScreen = Screen.Performance },
                                    modifier = Modifier.testTag("btn_top_performance")
                                ) {
                                    Icon(Icons.Default.Insights, contentDescription = "Performance", tint = MintGreen)
                                }
                                IconButton(
                                    onClick = { currentScreen = Screen.Settings },
                                    modifier = Modifier.testTag("btn_top_settings")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SecondaryText)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = CardBackground,
                                titleContentColor = PrimaryText
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = CardBackground,
                            contentColor = PrimaryText
                        ) {
                            NavigationBarItem(
                                selected = currentScreen is Screen.Dashboard,
                                onClick = { currentScreen = Screen.Dashboard },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = MintGreen,
                                    indicatorColor = MintGreen,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.PaperTrading,
                                onClick = { currentScreen = Screen.PaperTrading },
                                icon = { Icon(Icons.Default.ShowChart, contentDescription = "Trade") },
                                label = { Text("Trading", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = MintGreen,
                                    indicatorColor = MintGreen,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("nav_trade")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.AiAnalyst,
                                onClick = { currentScreen = Screen.AiAnalyst(null) },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Analyst") },
                                label = { Text("AI Scan", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = MintGreen,
                                    indicatorColor = MintGreen,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("nav_analyst")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.RiskCalculator,
                                onClick = { currentScreen = Screen.RiskCalculator(null) },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                                label = { Text("Risk Calc", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = MintGreen,
                                    indicatorColor = MintGreen,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("nav_calc")
                            )

                            NavigationBarItem(
                                selected = currentScreen is Screen.VideoStudio,
                                onClick = { currentScreen = Screen.VideoStudio(null) },
                                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Studio") },
                                label = { Text("Studio", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DeepBackground,
                                    selectedTextColor = MintGreen,
                                    indicatorColor = MintGreen,
                                    unselectedIconColor = SecondaryText,
                                    unselectedTextColor = SecondaryText
                                ),
                                modifier = Modifier.testTag("nav_studio")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val scr = currentScreen) {
                            is Screen.Dashboard -> {
                                DashboardScreen(
                                    accountState = accountState,
                                    quotes = quotes,
                                    onSelectInstrument = { inst -> currentScreen = Screen.MarketDetail(inst) },
                                    onNavigateToRiskCalculator = { inst -> currentScreen = Screen.RiskCalculator(inst) },
                                    onNavigateToAiAnalyst = { inst -> currentScreen = Screen.AiAnalyst(inst) },
                                    onNavigateToVideoStudio = { inst -> currentScreen = Screen.VideoStudio(inst) },
                                    onNavigateToEducation = { currentScreen = Screen.Education },
                                    connectionStatus = connectionStatus,
                                    userTimezone = userTimezone,
                                    onRefreshProviders = { marketDataRepo.syncFromLicensedProviders() }
                                )
                            }
                            is Screen.MarketDetail -> {
                                MarketDetailScreen(
                                    instrument = scr.instrument,
                                    quote = quotes[scr.instrument.id],
                                    accountState = accountState,
                                    marketDataRepository = marketDataRepo,
                                    openPositions = openPositions,
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onNavigateToAiAnalyst = { inst -> currentScreen = Screen.AiAnalyst(inst) },
                                    onNavigateToRiskCalculator = { inst -> currentScreen = Screen.RiskCalculator(inst) },
                                    userTimezone = userTimezone,
                                    onClosePosition = { posId ->
                                        coroutineScope.launch {
                                            tradingRepo.closePosition(posId)
                                            Toast.makeText(this@MainActivity, "Position closed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onPartialClosePosition = { posId, qty ->
                                        coroutineScope.launch {
                                            tradingRepo.partialClosePosition(posId, qty)
                                            Toast.makeText(this@MainActivity, "Partial closed $qty units", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onExecuteMarketOrder = { dir, qty, sl, tp, idea ->
                                        coroutineScope.launch {
                                            val res = tradingRepo.placeMarketOrder(scr.instrument, dir, qty, sl, tp, idea)
                                            if (res.isSuccess) {
                                                Toast.makeText(this@MainActivity, "${dir.name} Order executed!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(this@MainActivity, res.exceptionOrNull()?.message ?: "Order failed", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onExecutePendingOrder = { dir, type, target, qty, sl, tp ->
                                        coroutineScope.launch {
                                            val res = tradingRepo.placePendingOrder(scr.instrument, dir, type, target, qty, sl, tp)
                                            if (res.isSuccess) {
                                                Toast.makeText(this@MainActivity, "${type.name} Order placed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                            is Screen.PaperTrading -> {
                                PaperTradingScreen(
                                    accountState = accountState,
                                    openPositions = openPositions,
                                    pendingOrders = pendingOrders,
                                    tradeLogs = tradeLogs,
                                    onClosePosition = { posId ->
                                        coroutineScope.launch {
                                            tradingRepo.closePosition(posId)
                                            Toast.makeText(this@MainActivity, "Position closed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onPartialClosePosition = { posId, qty ->
                                        coroutineScope.launch {
                                            tradingRepo.partialClosePosition(posId, qty)
                                            Toast.makeText(this@MainActivity, "Partial closed $qty units", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCancelOrder = { orderId ->
                                        coroutineScope.launch {
                                            tradingRepo.cancelPendingOrder(orderId)
                                            Toast.makeText(this@MainActivity, "Order cancelled", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onResetAccount = {
                                        coroutineScope.launch {
                                            tradingRepo.resetAccount(10000.0)
                                            Toast.makeText(this@MainActivity, "Account reset to $10,000", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            is Screen.AiAnalyst -> {
                                AiAnalystScreen(
                                    aiAnalystRepository = aiAnalystRepo,
                                    initialInstrument = scr.instrument,
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onExecuteAiTrade = { inst, dir, sl, tp ->
                                        currentScreen = Screen.MarketDetail(inst)
                                    }
                                )
                            }
                            is Screen.RiskCalculator -> {
                                RiskCalculatorScreen(
                                    accountState = accountState,
                                    quotes = quotes,
                                    initialInstrument = scr.instrument,
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onApplyToTrade = { inst, dir, qty, sl, tp ->
                                        currentScreen = Screen.MarketDetail(inst)
                                    }
                                )
                            }
                            is Screen.VideoStudio -> {
                                VideoStudioScreen(
                                    mediaStudioRepository = mediaStudioRepo,
                                    initialInstrument = scr.instrument,
                                    onBack = { currentScreen = Screen.Dashboard }
                                )
                            }
                            is Screen.Education -> {
                                EducationScreen(
                                    educationRepository = educationRepo,
                                    onBack = { currentScreen = Screen.Dashboard }
                                )
                            }
                            is Screen.Performance -> {
                                PerformanceScreen(
                                    accountState = accountState,
                                    tradeLogs = tradeLogs,
                                    onBack = { currentScreen = Screen.Dashboard }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    accountState = accountState,
                                    currentTimezone = userTimezone,
                                    onBack = { currentScreen = Screen.Dashboard },
                                    onResetAccountBalance = { newBal ->
                                        coroutineScope.launch {
                                            tradingRepo.resetAccount(newBal)
                                            Toast.makeText(this@MainActivity, "Balance reset to $$newBal", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSelectTimezone = { tz ->
                                        marketDataRepo.setTimezone(tz)
                                        Toast.makeText(this@MainActivity, "Timezone updated to ${tz.code}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

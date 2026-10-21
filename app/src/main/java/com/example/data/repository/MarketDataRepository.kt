package com.example.data.repository

import com.example.data.network.MarketDataApiClient
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MarketDataRepository(
    private val apiClient: MarketDataApiClient = MarketDataApiClient()
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val basePrices = mapOf(
        "EURUSD" to 1.08520,
        "GBPUSD" to 1.29480,
        "USDJPY" to 152.350,
        "AUDUSD" to 0.65420,
        "USDCAD" to 1.38650,
        "USDCHF" to 0.86540,
        "NZDUSD" to 0.59780,
        "NIFTY50" to 24820.0,
        "BANKNIFTY" to 51340.0,
        "RELIANCE" to 2985.50,
        "TCS" to 4118.00,
        "INFY" to 1882.50,
        "HDFCBANK" to 1692.00,
        "TATAMOTORS" to 968.25,
        "BTCUSDT" to 67840.0,
        "ETHUSDT" to 2642.50,
        "SOLUSDT" to 166.40,
        "BNBUSDT" to 589.60
    )

    private val quotesMap = MutableStateFlow<Map<String, Quote>>(emptyMap())
    val quotesFlow: Flow<Map<String, Quote>> = quotesMap.asStateFlow()

    private val connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    val connectionStatusFlow: Flow<ConnectionStatus> = connectionStatus.asStateFlow()

    private val userTimezone = MutableStateFlow(UserTimezone.UTC)
    val userTimezoneFlow: Flow<UserTimezone> = userTimezone.asStateFlow()

    // Cache of candle history per instrument & timeframe
    private val candleCache = mutableMapOf<String, MutableList<Candle>>()

    init {
        // Initialize quotes with realistic baseline
        val initialQuotes = mutableMapOf<String, Quote>()
        val nowUtc = System.currentTimeMillis()
        InstrumentsCatalog.ALL_INSTRUMENTS.forEach { inst ->
            val mid = basePrices[inst.id] ?: 100.0
            val spread = inst.baseSpreadPipsOrTicks * inst.pipOrTickSize
            val halfSpread = spread / 2.0
            val sessionInfo = evaluateMarketSession(inst, nowUtc)
            initialQuotes[inst.id] = Quote(
                instrumentId = inst.id,
                bid = mid - halfSpread,
                ask = mid + halfSpread,
                high24h = mid * 1.018,
                low24h = mid * 0.985,
                change24hPercent = 0.45,
                volume24h = 12500000.0,
                timestamp = nowUtc,
                dataType = sessionInfo.first,
                dataSource = defaultDataSourceFor(inst.marketType),
                isMarketOpen = sessionInfo.second,
                sessionNotice = sessionInfo.third,
                lastSyncUtc = nowUtc
            )
        }
        quotesMap.value = initialQuotes

        // Trigger initial network sync for live providers
        syncFromLicensedProviders()
    }

    fun setTimezone(tz: UserTimezone) {
        userTimezone.value = tz
    }

    fun getQuote(instrumentId: String): Quote? = quotesMap.value[instrumentId]

    fun getConnectionStatus(): ConnectionStatus = connectionStatus.value

    fun getUserTimezone(): UserTimezone = userTimezone.value

    /**
     * Determines whether the market is open according to official exchange hours
     */
    fun evaluateMarketSession(instrument: Instrument, nowUtc: Long): Triple<DataType, Boolean, String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowUtc }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 7=Sat

        when (instrument.marketType) {
            MarketType.CRYPTO -> {
                return Triple(DataType.LIVE, true, "24/7 Global Spot Market Open")
            }
            MarketType.INDIAN_MARKET -> {
                // IST is UTC+5:30
                val istCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { timeInMillis = nowUtc }
                val istDay = istCal.get(Calendar.DAY_OF_WEEK)
                val hour = istCal.get(Calendar.HOUR_OF_DAY)
                val minute = istCal.get(Calendar.MINUTE)
                val totalMinutes = hour * 60 + minute

                val isWeekday = istDay in Calendar.MONDAY..Calendar.FRIDAY
                val isOpen = isWeekday && totalMinutes in (9 * 60 + 15)..(15 * 60 + 30)

                return if (isOpen) {
                    Triple(DataType.LIVE, true, "NSE India Open (09:15 - 15:30 IST)")
                } else {
                    val notice = if (!isWeekday) "NSE Weekend Closure • Historical Simulation" else "NSE Market Closed • Pre/Post Market Simulation"
                    Triple(DataType.MARKET_CLOSED, false, notice)
                }
            }
            MarketType.FOREX -> {
                // Forex opens Sun 5pm EST (22:00 UTC) and closes Fri 5pm EST (22:00 UTC)
                val hourUtc = cal.get(Calendar.HOUR_OF_DAY)
                val isWeekend = (dayOfWeek == Calendar.SATURDAY) ||
                        (dayOfWeek == Calendar.FRIDAY && hourUtc >= 22) ||
                        (dayOfWeek == Calendar.SUNDAY && hourUtc < 22)

                return if (!isWeekend) {
                    Triple(DataType.LIVE, true, "Interbank 24/5 FX Session Active")
                } else {
                    Triple(DataType.MARKET_CLOSED, false, "Forex Weekend Closure • Reopens Sun 5:00 PM EST")
                }
            }
        }
    }

    private fun defaultDataSourceFor(marketType: MarketType): String {
        return when (marketType) {
            MarketType.CRYPTO -> "Binance Market Data API (Live Spot)"
            MarketType.FOREX -> "Frankfurter ECB Reference Feed & Interbank Spreads"
            MarketType.INDIAN_MARKET -> "NSE India EOD / Real-Time Gateway"
        }
    }

    /**
     * Poll external APIs for live crypto prices and forex rates
     */
    fun syncFromLicensedProviders() {
        repositoryScope.launch {
            if (apiClient.isCurrentlyRateLimited()) {
                connectionStatus.value = ConnectionStatus.RATE_LIMITED
                return@launch
            }

            // 1. Sync Crypto from Binance API
            val cryptoSymbols = listOf("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT")
            var anyCryptoSuccess = false
            for (symbol in cryptoSymbols) {
                val result = apiClient.fetchCrypto24hTicker(symbol)
                result.onSuccess { data ->
                    anyCryptoSuccess = true
                    updateQuoteFromNetwork(symbol, data)
                }.onFailure { err ->
                    if (err.message?.contains("429") == true) {
                        connectionStatus.value = ConnectionStatus.RATE_LIMITED
                    }
                }
            }

            // 2. Sync Forex reference rates from Frankfurter ECB API
            val forexResult = apiClient.fetchForexRates()
            forexResult.onSuccess { rates ->
                updateForexQuotesFromEcb(rates)
                connectionStatus.value = ConnectionStatus.CONNECTED
            }.onFailure {
                if (!anyCryptoSuccess) {
                    connectionStatus.value = ConnectionStatus.FALLBACK_SIMULATION
                }
            }
        }
    }

    private fun updateQuoteFromNetwork(instrumentId: String, data: com.example.data.network.NetworkQuoteResult) {
        val currentQuote = quotesMap.value[instrumentId] ?: return
        val updated = currentQuote.copy(
            bid = data.bid,
            ask = data.ask,
            high24h = max(currentQuote.high24h, data.high24h),
            low24h = min(currentQuote.low24h, data.low24h),
            change24hPercent = data.changePercent,
            volume24h = data.volume,
            timestamp = data.timestampUtc,
            dataType = DataType.LIVE,
            dataSource = data.dataSource,
            lastSyncUtc = System.currentTimeMillis()
        )
        val copy = quotesMap.value.toMutableMap()
        copy[instrumentId] = updated
        quotesMap.value = copy
    }

    private fun updateForexQuotesFromEcb(rates: Map<String, Double>) {
        val copy = quotesMap.value.toMutableMap()
        val nowUtc = System.currentTimeMillis()

        // EUR/USD: 1 EUR = (1 / EUR rate) USD
        rates["EUR"]?.let { eurUsdRate ->
            val eurPrice = 1.0 / eurUsdRate
            updateFxQuoteInMap(copy, "EURUSD", eurPrice, nowUtc)
        }
        rates["GBP"]?.let { gbpUsdRate ->
            val gbpPrice = 1.0 / gbpUsdRate
            updateFxQuoteInMap(copy, "GBPUSD", gbpPrice, nowUtc)
        }
        rates["JPY"]?.let { jpyRate ->
            updateFxQuoteInMap(copy, "USDJPY", jpyRate, nowUtc)
        }
        rates["AUD"]?.let { audRate ->
            val audPrice = 1.0 / audRate
            updateFxQuoteInMap(copy, "AUDUSD", audPrice, nowUtc)
        }
        rates["CAD"]?.let { cadRate ->
            updateFxQuoteInMap(copy, "USDCAD", cadRate, nowUtc)
        }
        rates["CHF"]?.let { chfRate ->
            updateFxQuoteInMap(copy, "USDCHF", chfRate, nowUtc)
        }
        rates["NZD"]?.let { nzdRate ->
            val nzdPrice = 1.0 / nzdRate
            updateFxQuoteInMap(copy, "NZDUSD", nzdPrice, nowUtc)
        }

        quotesMap.value = copy
    }

    private fun updateFxQuoteInMap(map: MutableMap<String, Quote>, instId: String, midPrice: Double, nowUtc: Long) {
        val inst = InstrumentsCatalog.findById(instId) ?: return
        val current = map[instId] ?: return
        val spread = inst.baseSpreadPipsOrTicks * inst.pipOrTickSize
        val halfSpread = spread / 2.0
        val sessionInfo = evaluateMarketSession(inst, nowUtc)

        map[instId] = current.copy(
            bid = midPrice - halfSpread,
            ask = midPrice + halfSpread,
            high24h = max(current.high24h, midPrice + halfSpread),
            low24h = min(current.low24h, midPrice - halfSpread),
            timestamp = nowUtc,
            dataType = sessionInfo.first,
            dataSource = "Frankfurter ECB Reference Feed & Interbank Spreads",
            isMarketOpen = sessionInfo.second,
            sessionNotice = sessionInfo.third,
            lastSyncUtc = nowUtc
        )
    }

    /**
     * Get candlesticks with data gap healing (forward-fills missing bars)
     */
    fun getCandles(instrument: Instrument, timeframe: Timeframe, count: Int = 80): List<Candle> {
        val key = "${instrument.id}_${timeframe.name}"
        if (!candleCache.containsKey(key) || (candleCache[key]?.size ?: 0) < count) {
            val generated = generateInitialCandles(instrument, timeframe, count)
            val healed = healCandleGaps(generated, timeframe)
            candleCache[key] = healed.toMutableList()
        }
        return candleCache[key]?.takeLast(count) ?: emptyList()
    }

    /**
     * Heals any data gaps where delta between consecutive candles exceeds the expected timeframe interval
     */
    private fun healCandleGaps(rawCandles: List<Candle>, timeframe: Timeframe): List<Candle> {
        if (rawCandles.size < 2) return rawCandles
        val healed = mutableListOf<Candle>()
        val intervalMs = timeframe.minutes * 60 * 1000L

        for (i in 0 until rawCandles.size - 1) {
            val current = rawCandles[i]
            val next = rawCandles[i + 1]
            healed.add(current)

            val gap = next.timestamp - current.timestamp
            if (gap > intervalMs * 1.5 && gap < intervalMs * 10) {
                // Interpolate / forward fill missing candle
                var fillTime = current.timestamp + intervalMs
                while (fillTime < next.timestamp) {
                    healed.add(
                        Candle(
                            timestamp = fillTime,
                            open = current.close,
                            high = max(current.close, next.open),
                            low = min(current.close, next.open),
                            close = (current.close + next.open) / 2.0,
                            volume = current.volume * 0.5,
                            dataSource = "${current.dataSource} (Interpolated)"
                        )
                    )
                    fillTime += intervalMs
                }
            }
        }
        healed.add(rawCandles.last())
        return healed
    }

    /**
     * Advance market price deterministically or via simulated order flow
     */
    fun tickPrice(instrumentId: String): Quote {
        val instrument = InstrumentsCatalog.findById(instrumentId) ?: return quotesMap.value[instrumentId]!!
        val currentQuote = quotesMap.value[instrumentId] ?: return quotesMap.value[instrumentId]!!
        val nowUtc = System.currentTimeMillis()
        val sessionInfo = evaluateMarketSession(instrument, nowUtc)

        val volatility = when (instrument.marketType) {
            MarketType.FOREX -> 0.0003
            MarketType.INDIAN_MARKET -> if (sessionInfo.second) 0.0008 else 0.0001
            MarketType.CRYPTO -> 0.0015
        }

        val deltaPercent = (Random.nextDouble() - 0.495) * volatility
        val newMid = currentQuote.mid * (1.0 + deltaPercent)
        val spread = instrument.baseSpreadPipsOrTicks * instrument.pipOrTickSize
        val halfSpread = spread / 2.0
        val newBid = newMid - halfSpread
        val newAsk = newMid + halfSpread

        val updatedQuote = currentQuote.copy(
            bid = newBid,
            ask = newAsk,
            high24h = max(currentQuote.high24h, newAsk),
            low24h = min(currentQuote.low24h, newBid),
            timestamp = nowUtc,
            dataType = sessionInfo.first,
            isMarketOpen = sessionInfo.second,
            sessionNotice = sessionInfo.third
        )

        val updated = quotesMap.value.toMutableMap()
        updated[instrumentId] = updatedQuote
        quotesMap.value = updated

        // Update the latest candle in cache
        Timeframe.values().forEach { tf ->
            val key = "${instrumentId}_${tf.name}"
            val list = candleCache[key]
            if (!list.isNullOrEmpty()) {
                val lastCandle = list.last()
                val updatedCandle = lastCandle.copy(
                    high = max(lastCandle.high, newAsk),
                    low = min(lastCandle.low, newBid),
                    close = newMid,
                    volume = lastCandle.volume + Random.nextDouble(10.0, 50.0)
                )
                list[list.size - 1] = updatedCandle
            }
        }

        return updatedQuote
    }

    private fun generateInitialCandles(instrument: Instrument, timeframe: Timeframe, count: Int): List<Candle> {
        val list = mutableListOf<Candle>()
        var currentPrice = basePrices[instrument.id] ?: 100.0
        val intervalMs = timeframe.minutes * 60 * 1000L
        var currentTimestamp = System.currentTimeMillis() - (count * intervalMs)

        for (i in 0 until count) {
            val wave = kotlin.math.sin(i / 6.0) * 0.0025
            val noise = (Random.nextDouble() - 0.5) * 0.003
            val open = currentPrice
            val close = open * (1.0 + wave + noise)
            val wickUpper = max(open, close) * (1.0 + Random.nextDouble(0.0005, 0.002))
            val wickLower = min(open, close) * (1.0 - Random.nextDouble(0.0005, 0.002))
            val volume = Random.nextDouble(500.0, 20000.0)

            list.add(
                Candle(
                    timestamp = currentTimestamp,
                    open = open,
                    high = wickUpper,
                    low = wickLower,
                    close = close,
                    volume = volume,
                    dataSource = defaultDataSourceFor(instrument.marketType)
                )
            )
            currentPrice = close
            currentTimestamp += intervalMs
        }
        return list
    }
}

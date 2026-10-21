package com.example

import com.example.engine.PaperTradingEngine
import com.example.engine.RiskCalculatorEngine
import com.example.engine.SmartMoneyConceptEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class FinancialCalculationsTest {

    @Test
    fun testForexPipValue_EurUsd() {
        val eurusd = InstrumentsCatalog.findById("EURUSD")!!
        val pipVal = PaperTradingEngine.calculateForexPipValueUsd(eurusd, 1.0850)
        // EUR/USD standard lot (100,000 units) has $10 per pip
        assertEquals(10.0, pipVal, 0.001)
    }

    @Test
    fun testForexPipValue_UsdJpy() {
        val usdjpy = InstrumentsCatalog.findById("USDJPY")!!
        val pipVal = PaperTradingEngine.calculateForexPipValueUsd(usdjpy, 150.0)
        // (0.01 / 150.0) * 100,000 = 6.6667 USD
        assertEquals(6.6667, pipVal, 0.01)
    }

    @Test
    fun testForexPnL_BuyAndSell() {
        val eurusd = InstrumentsCatalog.findById("EURUSD")!!
        // Long 1 standard lot: entry 1.0850, exit 1.0880 (+30 pips) -> $300 profit
        val buyPnL = PaperTradingEngine.computeGrossPnL(
            instrument = eurusd,
            direction = TradeDirection.BUY,
            entryPrice = 1.0850,
            exitPrice = 1.0880,
            quantity = 1.0
        )
        assertEquals(300.0, buyPnL, 0.5)

        // Short 1 standard lot: entry 1.0880, exit 1.0850 (+30 pips) -> $300 profit
        val sellPnL = PaperTradingEngine.computeGrossPnL(
            instrument = eurusd,
            direction = TradeDirection.SELL,
            entryPrice = 1.0880,
            exitPrice = 1.0850,
            quantity = 1.0
        )
        assertEquals(300.0, sellPnL, 0.5)
    }

    @Test
    fun testIndianMarketPnL_NiftyAndReliance() {
        val nifty = InstrumentsCatalog.findById("NIFTY50")!!
        // Nifty 1 lot = 25 units. Buy entry 24,800, exit 24,900 (+100 points) = 2,500 INR
        // 2500 INR / 86.5 = ~28.90 USD
        val niftyPnL = PaperTradingEngine.computeGrossPnL(
            instrument = nifty,
            direction = TradeDirection.BUY,
            entryPrice = 24800.0,
            exitPrice = 24900.0,
            quantity = 25.0
        )
        assertTrue(niftyPnL > 25.0 && niftyPnL < 35.0)

        val reliance = InstrumentsCatalog.findById("RELIANCE")!!
        val reliancePnL = PaperTradingEngine.computeGrossPnL(
            instrument = reliance,
            direction = TradeDirection.BUY,
            entryPrice = 2900.0,
            exitPrice = 2950.0,
            quantity = 10.0
        )
        // 50 INR * 10 shares = 500 INR / 86.5 = ~5.78 USD
        assertEquals(5.78, reliancePnL, 0.2)
    }

    @Test
    fun testCryptoPnL_BtcUsdt() {
        val btc = InstrumentsCatalog.findById("BTCUSDT")!!
        // Buy 0.5 BTC at 67,000, exit at 68,000 (+1,000 * 0.5 = +$500)
        val pnl = PaperTradingEngine.computeGrossPnL(
            instrument = btc,
            direction = TradeDirection.BUY,
            entryPrice = 67000.0,
            exitPrice = 68000.0,
            quantity = 0.5
        )
        assertEquals(500.0, pnl, 0.01)
    }

    @Test
    fun testRiskCalculator_OnePercentRule() {
        val eurusd = InstrumentsCatalog.findById("EURUSD")!!
        // $10,000 equity, 1% risk = $100 risk
        // Entry 1.0850, Stop Loss 1.0830 (20 pips risk)
        // 20 pips * $10/lot = $200 risk per 1.0 lot -> 0.50 lots for $100 risk
        val result = RiskCalculatorEngine.calculateRisk(
            equity = 10000.0,
            instrument = eurusd,
            direction = TradeDirection.BUY,
            entryPrice = 1.0850,
            stopLossPrice = 1.0830,
            targetPrice = 1.0910, // 60 pips target -> 1:3 R:R
            riskPercent = 1.0
        )

        assertTrue(result.isValid)
        assertEquals(100.0, result.monetaryRisk, 0.01)
        assertEquals(0.50, result.recommendedQuantityOrLots, 0.05)
        assertEquals(300.0, result.potentialReward, 5.0)
        assertEquals(3.0, result.rewardToRiskRatio, 0.1)
    }

    @Test
    fun testRiskCalculator_RejectsInvalidStopLoss() {
        val eurusd = InstrumentsCatalog.findById("EURUSD")!!
        // BUY with stop loss ABOVE entry is invalid
        val result = RiskCalculatorEngine.calculateRisk(
            equity = 10000.0,
            instrument = eurusd,
            direction = TradeDirection.BUY,
            entryPrice = 1.0850,
            stopLossPrice = 1.0880,
            targetPrice = 1.0920,
            riskPercent = 1.0
        )
        assertFalse(result.isValid)
        assertNotNull(result.validationError)
    }

    @Test
    fun testSmartMoneyConceptEngine_DetectsCandleAnalysis() {
        val eurusd = InstrumentsCatalog.findById("EURUSD")!!
        val candles = mutableListOf<Candle>()
        var price = 1.0800
        for (i in 0 until 30) {
            val open = price
            val close = open + (if (i % 2 == 0) 0.0010 else -0.0004)
            candles.add(
                Candle(
                    timestamp = 1000L * i,
                    open = open,
                    high = maxOf(open, close) + 0.0005,
                    low = minOf(open, close) - 0.0005,
                    close = close,
                    volume = 1000.0
                )
            )
            price = close
        }

        val analysis = SmartMoneyConceptEngine.analyzeCandles(eurusd, Timeframe.M15, candles)
        assertNotNull(analysis)
        assertEquals(eurusd.id, analysis.instrumentId)
        assertTrue(analysis.supportLevel > 0.0)
        assertTrue(analysis.resistanceLevel > 0.0)
    }

    @Test
    fun testTimezoneConversions_UtcToIstAndEst() {
        // 2026-10-21 00:00:00 UTC = 1792540800000L
        val fixedUtcMillis = 1792540800000L

        val utcFormatted = UserTimezone.UTC.formatTimestamp(fixedUtcMillis)
        val istFormatted = UserTimezone.IST.formatTimestamp(fixedUtcMillis)
        val estFormatted = UserTimezone.EST.formatTimestamp(fixedUtcMillis)

        assertNotNull(utcFormatted)
        assertNotNull(istFormatted)
        assertNotNull(estFormatted)

        // IST is UTC+5:30, EST is UTC-4:00 (EDT) or UTC-5:00 (EST)
        assertNotEquals(utcFormatted, istFormatted)
        assertNotEquals(utcFormatted, estFormatted)
    }

    @Test
    fun testMarketSessionEvaluation_CryptoAlwaysOpen() {
        val btc = InstrumentsCatalog.findById("BTCUSDT")!!
        val repo = com.example.data.repository.MarketDataRepository()
        val session = repo.evaluateMarketSession(btc, System.currentTimeMillis())

        assertEquals(DataType.LIVE, session.first)
        assertTrue(session.second)
    }
}

package com.example.engine

import com.example.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SmartMoneyConceptEngine {

    fun analyzeCandles(
        instrument: Instrument,
        timeframe: Timeframe,
        candles: List<Candle>
    ): MarketStructureAnalysis {
        if (candles.size < 15) {
            val lastClose = candles.lastOrNull()?.close ?: 0.0
            return MarketStructureAnalysis(
                instrumentId = instrument.id,
                timeframe = timeframe,
                trend = TrendDirection.RANGING,
                structurePoints = emptyList(),
                smcEvents = emptyList(),
                supportLevel = lastClose * 0.99,
                resistanceLevel = lastClose * 1.01,
                setupRecommendation = "NO VALID SETUP",
                setupConfidence = "None",
                entryPrice = null,
                stopLossPrice = null,
                takeProfitPrice = null,
                riskToReward = 0.0,
                observedConditions = "Insufficient candle history (< 15 candles) to identify institutional order flow.",
                supportingEvidence = emptyList(),
                invalidationCondition = "Wait for more price discovery.",
                cautionReasons = listOf("Lack of historical context", "Market structure undefined")
            )
        }

        // 1. Identify Swing Points (window of 2 left, 2 right)
        val structurePoints = mutableListOf<StructurePoint>()
        for (i in 2 until candles.size - 2) {
            val c = candles[i]
            val isSwingHigh = c.high > candles[i - 1].high &&
                    c.high > candles[i - 2].high &&
                    c.high > candles[i + 1].high &&
                    c.high > candles[i + 2].high

            val isSwingLow = c.low < candles[i - 1].low &&
                    c.low < candles[i - 2].low &&
                    c.low < candles[i + 1].low &&
                    c.low < candles[i + 2].low

            if (isSwingHigh) {
                val prevHigh = structurePoints.filter { it.type == StructureType.HIGHER_HIGH || it.type == StructureType.LOWER_HIGH }.lastOrNull()
                val type = if (prevHigh == null || c.high >= prevHigh.price) StructureType.HIGHER_HIGH else StructureType.LOWER_HIGH
                structurePoints.add(StructurePoint(i, c.timestamp, c.high, type))
            } else if (isSwingLow) {
                val prevLow = structurePoints.filter { it.type == StructureType.HIGHER_LOW || it.type == StructureType.LOWER_LOW }.lastOrNull()
                val type = if (prevLow == null || c.low >= prevLow.price) StructureType.HIGHER_LOW else StructureType.LOWER_LOW
                structurePoints.add(StructurePoint(i, c.timestamp, c.low, type))
            }
        }

        // 2. Identify SMC Events: BOS, CHoCH, FVGs, Sweeps
        val smcEvents = mutableListOf<SmcEvent>()

        // Detect FVGs
        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]

            // Bullish FVG: c1.high < c3.low and c2 is a strong bullish impulse
            if (c3.low > c1.high && c2.isBullish) {
                smcEvents.add(
                    SmcEvent(
                        type = "FAIR_VALUE_GAP",
                        priceLevel = (c1.high + c3.low) / 2.0,
                        startIndex = i - 2,
                        endIndex = i,
                        isBullish = true,
                        label = "+FVG [${instrument.formatPrice(c1.high)} - ${instrument.formatPrice(c3.low)}]"
                    )
                )
            }
            // Bearish FVG: c1.low > c3.high and c2 is a strong bearish impulse
            if (c3.high < c1.low && !c2.isBullish) {
                smcEvents.add(
                    SmcEvent(
                        type = "FAIR_VALUE_GAP",
                        priceLevel = (c1.low + c3.high) / 2.0,
                        startIndex = i - 2,
                        endIndex = i,
                        isBullish = false,
                        label = "-FVG [${instrument.formatPrice(c3.high)} - ${instrument.formatPrice(c1.low)}]"
                    )
                )
            }
        }

        // Detect BOS & CHoCH
        val highs = structurePoints.filter { it.type == StructureType.HIGHER_HIGH || it.type == StructureType.LOWER_HIGH }
        val lows = structurePoints.filter { it.type == StructureType.HIGHER_LOW || it.type == StructureType.LOWER_LOW }

        val lastClose = candles.last().close
        var currentTrend = TrendDirection.RANGING

        // Check recent structure points
        if (highs.size >= 2 && lows.size >= 2) {
            val lastH = highs.last()
            val lastL = lows.last()

            val isRecentUptrend = lastH.type == StructureType.HIGHER_HIGH && lastL.type == StructureType.HIGHER_LOW
            val isRecentDowntrend = lastH.type == StructureType.LOWER_HIGH && lastL.type == StructureType.LOWER_LOW

            currentTrend = when {
                isRecentUptrend -> TrendDirection.BULLISH
                isRecentDowntrend -> TrendDirection.BEARISH
                else -> TrendDirection.RANGING
            }

            // Check if last candle caused a BOS or CHoCH
            if (lastClose > lastH.price) {
                val isBOS = currentTrend == TrendDirection.BULLISH
                smcEvents.add(
                    SmcEvent(
                        type = if (isBOS) "BOS" else "CHoCH",
                        priceLevel = lastH.price,
                        startIndex = lastH.index,
                        endIndex = candles.size - 1,
                        isBullish = true,
                        label = if (isBOS) "Bullish BOS" else "Bullish CHoCH (Trend Shift)"
                    )
                )
            } else if (lastClose < lastL.price) {
                val isBOS = currentTrend == TrendDirection.BEARISH
                smcEvents.add(
                    SmcEvent(
                        type = if (isBOS) "BOS" else "CHoCH",
                        priceLevel = lastL.price,
                        startIndex = lastL.index,
                        endIndex = candles.size - 1,
                        isBullish = false,
                        label = if (isBOS) "Bearish BOS" else "Bearish CHoCH (Trend Shift)"
                    )
                )
            }
        }

        // Detect Liquidity Sweep on most recent 10 candles
        for (i in max(0, candles.size - 10) until candles.size) {
            val candle = candles[i]
            for (high in highs) {
                if (candle.high > high.price && candle.close < high.price && candle.isBullish.not()) {
                    smcEvents.add(
                        SmcEvent(
                            type = "LIQUIDITY_SWEEP",
                            priceLevel = high.price,
                            startIndex = high.index,
                            endIndex = i,
                            isBullish = false,
                            label = "Buy-Side Liquidity (BSL) Swept"
                        )
                    )
                }
            }
            for (low in lows) {
                if (candle.low < low.price && candle.close > low.price && candle.isBullish) {
                    smcEvents.add(
                        SmcEvent(
                            type = "LIQUIDITY_SWEEP",
                            priceLevel = low.price,
                            startIndex = low.index,
                            endIndex = i,
                            isBullish = true,
                            label = "Sell-Side Liquidity (SSL) Swept"
                        )
                    )
                }
            }
        }

        // Support & Resistance based on key swings
        val support = lows.map { it.price }.minOrNull() ?: (lastClose * 0.99)
        val resistance = highs.map { it.price }.maxOrNull() ?: (lastClose * 1.01)

        // Evaluate Setup
        val recentBullishFvg = smcEvents.filter { it.type == "FAIR_VALUE_GAP" && it.isBullish }.lastOrNull()
        val recentBearishFvg = smcEvents.filter { it.type == "FAIR_VALUE_GAP" && !it.isBullish }.lastOrNull()
        val hasBslSweep = smcEvents.any { it.type == "LIQUIDITY_SWEEP" && !it.isBullish }
        val hasSslSweep = smcEvents.any { it.type == "LIQUIDITY_SWEEP" && it.isBullish }

        var setupRecommendation = "NO VALID SETUP"
        var setupConfidence = "None"
        var entryPrice: Double? = null
        var stopLossPrice: Double? = null
        var takeProfitPrice: Double? = null
        var riskToReward = 0.0
        val supportingEvidence = mutableListOf<String>()
        val cautionReasons = mutableListOf<String>()
        var observedConditions = ""
        var invalidationCondition = ""

        if (currentTrend == TrendDirection.BULLISH && (hasSslSweep || recentBullishFvg != null)) {
            setupRecommendation = "BULLISH SMC DISCOUNT RETEST"
            setupConfidence = "High"
            entryPrice = recentBullishFvg?.priceLevel ?: (lastClose * 0.998)
            stopLossPrice = support * 0.997
            takeProfitPrice = resistance
            val riskDist = abs((entryPrice ?: lastClose) - (stopLossPrice ?: (lastClose * 0.99)))
            val rewardDist = abs((takeProfitPrice ?: (lastClose * 1.02)) - (entryPrice ?: lastClose))
            riskToReward = if (riskDist > 0) rewardDist / riskDist else 2.5
            observedConditions = "Market exhibiting clean Higher Highs and Higher Lows on ${timeframe.label} timeframe with bullish institutional footprint."
            supportingEvidence.add("Confirmed Higher Low structural swing")
            if (hasSslSweep) supportingEvidence.add("Sell-Side Liquidity (SSL) sweep below key swing low")
            if (recentBullishFvg != null) supportingEvidence.add("Unmitigated Bullish Fair Value Gap at ${instrument.formatPrice(recentBullishFvg.priceLevel)}")
            invalidationCondition = "Hourly close below key swing low at ${instrument.formatPrice(stopLossPrice ?: 0.0)}"
            cautionReasons.add("Do not enter before price touches discount FVG / Order Block zone")
            cautionReasons.add("Watch out for high-impact macroeconomic news releases")
        } else if (currentTrend == TrendDirection.BEARISH && (hasBslSweep || recentBearishFvg != null)) {
            setupRecommendation = "BEARISH SMC PREMIUM EXPANSION"
            setupConfidence = "High"
            entryPrice = recentBearishFvg?.priceLevel ?: (lastClose * 1.002)
            stopLossPrice = resistance * 1.003
            takeProfitPrice = support
            val riskDist = abs((stopLossPrice ?: (lastClose * 1.01)) - (entryPrice ?: lastClose))
            val rewardDist = abs((entryPrice ?: lastClose) - (takeProfitPrice ?: (lastClose * 0.98)))
            riskToReward = if (riskDist > 0) rewardDist / riskDist else 2.5
            observedConditions = "Downtrend structure validated with Lower Highs and Lower Lows on ${timeframe.label}."
            supportingEvidence.add("Lower High formed following liquidity intake")
            if (hasBslSweep) supportingEvidence.add("Buy-Side Liquidity (BSL) swept above prior resistance")
            if (recentBearishFvg != null) supportingEvidence.add("Active Bearish Imbalance at ${instrument.formatPrice(recentBearishFvg.priceLevel)}")
            invalidationCondition = "Candle body close above swing high at ${instrument.formatPrice(stopLossPrice ?: 0.0)}"
            cautionReasons.add("Do not chase price at lows; wait for premium retracement")
        } else {
            setupRecommendation = "NO VALID SETUP"
            setupConfidence = "None"
            observedConditions = "Market is currently range-bound or consolidating inside an equilibrium range without clear displacement or order flow confirmation."
            cautionReasons.add("Equilibrium chop: unfavorable risk-to-reward ratio")
            cautionReasons.add("No clean liquidity sweep or BOS observed in recent price action")
            invalidationCondition = "Wait for a clean Break of Structure (BOS) or external range liquidity sweep before committing capital."
        }

        return MarketStructureAnalysis(
            instrumentId = instrument.id,
            timeframe = timeframe,
            trend = currentTrend,
            structurePoints = structurePoints,
            smcEvents = smcEvents,
            supportLevel = support,
            resistanceLevel = resistance,
            setupRecommendation = setupRecommendation,
            setupConfidence = setupConfidence,
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            takeProfitPrice = takeProfitPrice,
            riskToReward = riskToReward,
            observedConditions = observedConditions,
            supportingEvidence = supportingEvidence,
            invalidationCondition = invalidationCondition,
            cautionReasons = cautionReasons,
            timestamp = System.currentTimeMillis()
        )
    }
}

package com.example.engine

import com.example.model.Instrument
import com.example.model.MarketType
import com.example.model.RiskCalculationResult
import com.example.model.TradeDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object RiskCalculatorEngine {

    fun calculateRisk(
        equity: Double,
        instrument: Instrument,
        direction: TradeDirection,
        entryPrice: Double,
        stopLossPrice: Double,
        targetPrice: Double?,
        riskPercent: Double
    ): RiskCalculationResult {
        if (equity <= 0.0) {
            return errorResult(instrument, direction, entryPrice, stopLossPrice, targetPrice, riskPercent, "Account equity must be greater than zero.")
        }
        if (entryPrice <= 0.0 || stopLossPrice <= 0.0) {
            return errorResult(instrument, direction, entryPrice, stopLossPrice, targetPrice, riskPercent, "Prices must be positive numbers.")
        }

        // Validate stop loss side
        if (direction == TradeDirection.BUY && stopLossPrice >= entryPrice) {
            return errorResult(instrument, direction, entryPrice, stopLossPrice, targetPrice, riskPercent, "For BUY trades, Stop Loss must be BELOW entry price.")
        }
        if (direction == TradeDirection.SELL && stopLossPrice <= entryPrice) {
            return errorResult(instrument, direction, entryPrice, stopLossPrice, targetPrice, riskPercent, "For SELL trades, Stop Loss must be ABOVE entry price.")
        }

        val monetaryRisk = equity * (riskPercent / 100.0)
        val stopDistancePrice = abs(entryPrice - stopLossPrice)
        val stopDistancePipsOrTicks = stopDistancePrice / instrument.pipOrTickSize

        val recommendedQuantity: Double = when (instrument.marketType) {
            MarketType.FOREX -> {
                val pipValuePerStandardLot = PaperTradingEngine.calculateForexPipValueUsd(instrument, entryPrice)
                val riskPerStandardLot = stopDistancePipsOrTicks * pipValuePerStandardLot
                if (riskPerStandardLot > 0) {
                    val rawLots = monetaryRisk / riskPerStandardLot
                    // Round to nearest 0.01 lot
                    val step = instrument.quantityStep
                    val stepped = round(rawLots / step) * step
                    max(instrument.minQuantity, stepped)
                } else instrument.minQuantity
            }
            MarketType.INDIAN_MARKET -> {
                val monetaryRiskInr = monetaryRisk * PaperTradingEngine.DEFAULT_INR_PER_USD
                val riskPerShare = stopDistancePrice * instrument.contractSize
                if (riskPerShare > 0) {
                    val rawShares = monetaryRiskInr / riskPerShare
                    val step = instrument.quantityStep
                    val stepped = (round(rawShares / step) * step)
                    max(instrument.minQuantity, stepped)
                } else instrument.minQuantity
            }
            MarketType.CRYPTO -> {
                if (stopDistancePrice > 0) {
                    val rawCoins = monetaryRisk / stopDistancePrice
                    val step = instrument.quantityStep
                    val stepped = round(rawCoins / step) * step
                    max(instrument.minQuantity, stepped)
                } else instrument.minQuantity
            }
        }

        val potentialReward: Double = if (targetPrice != null && targetPrice > 0.0) {
            val rewardDist = abs(targetPrice - entryPrice)
            PaperTradingEngine.computeGrossPnL(instrument, direction, entryPrice, targetPrice, recommendedQuantity)
        } else 0.0

        val rewardToRiskRatio: Double = if (monetaryRisk > 0.0 && potentialReward > 0.0) {
            potentialReward / monetaryRisk
        } else 0.0

        val estimatedMargin = PaperTradingEngine.calculateRequiredMargin(instrument, entryPrice, recommendedQuantity)
        val estimatedFee = PaperTradingEngine.computeTransactionFee(instrument, entryPrice, recommendedQuantity) * 2.0 // Entry + exit

        return RiskCalculationResult(
            instrumentId = instrument.id,
            direction = direction,
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            takeProfitPrice = targetPrice,
            riskPercent = riskPercent,
            monetaryRisk = monetaryRisk,
            recommendedQuantityOrLots = recommendedQuantity,
            potentialReward = potentialReward,
            rewardToRiskRatio = rewardToRiskRatio,
            estimatedMargin = estimatedMargin,
            estimatedFee = estimatedFee,
            stopDistancePipsOrTicks = stopDistancePipsOrTicks,
            isValid = true,
            validationError = null
        )
    }

    private fun errorResult(
        instrument: Instrument,
        direction: TradeDirection,
        entryPrice: Double,
        stopLossPrice: Double,
        targetPrice: Double?,
        riskPercent: Double,
        error: String
    ): RiskCalculationResult {
        return RiskCalculationResult(
            instrumentId = instrument.id,
            direction = direction,
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            takeProfitPrice = targetPrice,
            riskPercent = riskPercent,
            monetaryRisk = 0.0,
            recommendedQuantityOrLots = 0.0,
            potentialReward = 0.0,
            rewardToRiskRatio = 0.0,
            estimatedMargin = 0.0,
            estimatedFee = 0.0,
            stopDistancePipsOrTicks = 0.0,
            isValid = false,
            validationError = error
        )
    }
}

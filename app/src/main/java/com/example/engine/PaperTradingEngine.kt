package com.example.engine

import com.example.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

object PaperTradingEngine {
    const val DEFAULT_INR_PER_USD = 86.50

    /**
     * Calculate pip value in USD for 1.0 standard lot (100,000 units)
     */
    fun calculateForexPipValueUsd(instrument: Instrument, currentPrice: Double): Double {
        if (instrument.marketType != MarketType.FOREX) return 1.0
        return when {
            instrument.quoteCurrency == "USD" -> 10.0 // EURUSD, GBPUSD, AUDUSD, NZDUSD: $10/pip per standard lot
            instrument.baseCurrency == "USD" -> {
                // USDJPY, USDCAD, USDCHF: (0.0001 or 0.01 / rate) * 100,000
                (instrument.pipOrTickSize / max(0.0001, currentPrice)) * instrument.contractSize
            }
            else -> 10.0
        }
    }

    /**
     * Compute gross P&L in USD given entry, exit, quantity, and instrument specs
     */
    fun computeGrossPnL(
        instrument: Instrument,
        direction: TradeDirection,
        entryPrice: Double,
        exitPrice: Double,
        quantity: Double
    ): Double {
        return when (instrument.marketType) {
            MarketType.FOREX -> {
                val priceDiff = if (direction == TradeDirection.BUY) exitPrice - entryPrice else entryPrice - exitPrice
                val pips = priceDiff / instrument.pipOrTickSize
                val pipValuePerLot = calculateForexPipValueUsd(instrument, exitPrice)
                pips * pipValuePerLot * quantity
            }
            MarketType.INDIAN_MARKET -> {
                val priceDiff = if (direction == TradeDirection.BUY) exitPrice - entryPrice else entryPrice - exitPrice
                val totalInrDiff = priceDiff * quantity * instrument.contractSize
                totalInrDiff / DEFAULT_INR_PER_USD
            }
            MarketType.CRYPTO -> {
                val priceDiff = if (direction == TradeDirection.BUY) exitPrice - entryPrice else entryPrice - exitPrice
                priceDiff * quantity
            }
        }
    }

    /**
     * Compute transaction fee in USD
     */
    fun computeTransactionFee(instrument: Instrument, executionPrice: Double, quantity: Double): Double {
        return when (instrument.marketType) {
            MarketType.FOREX -> {
                // $3.50 commission per standard lot turn
                quantity * 3.50
            }
            MarketType.INDIAN_MARKET -> {
                // 0.03% brokerage / STT / exchange fees
                val turnoverInr = executionPrice * quantity * instrument.contractSize
                (turnoverInr * 0.0003) / DEFAULT_INR_PER_USD
            }
            MarketType.CRYPTO -> {
                // 0.06% spot fee
                (executionPrice * quantity) * 0.0006
            }
        }
    }

    /**
     * Compute pips or points moved
     */
    fun computePipsOrPoints(instrument: Instrument, direction: TradeDirection, entryPrice: Double, exitPrice: Double): Double {
        val diff = if (direction == TradeDirection.BUY) exitPrice - entryPrice else entryPrice - exitPrice
        return diff / instrument.pipOrTickSize
    }

    /**
     * Determine execution entry price with bid/ask rules
     */
    fun getEntryExecutionPrice(quote: Quote, direction: TradeDirection): Double {
        return if (direction == TradeDirection.BUY) quote.ask else quote.bid
    }

    /**
     * Determine closing price with bid/ask rules
     */
    fun getCloseExecutionPrice(quote: Quote, direction: TradeDirection): Double {
        // Long closes at bid; Short closes at ask
        return if (direction == TradeDirection.BUY) quote.bid else quote.ask
    }

    /**
     * Calculate required margin in USD
     */
    fun calculateRequiredMargin(instrument: Instrument, price: Double, quantity: Double): Double {
        return when (instrument.marketType) {
            MarketType.FOREX -> {
                // 1:30 leverage -> 3.33% margin
                val notionalUsd = if (instrument.baseCurrency == "USD") {
                    quantity * instrument.contractSize
                } else {
                    quantity * instrument.contractSize * price
                }
                notionalUsd / 30.0
            }
            MarketType.INDIAN_MARKET -> {
                // 1:5 leverage
                val notionalInr = price * quantity * instrument.contractSize
                (notionalInr / 5.0) / DEFAULT_INR_PER_USD
            }
            MarketType.CRYPTO -> {
                // 1:2 leverage or 1:1 spot
                (price * quantity) / 2.0
            }
        }
    }
}

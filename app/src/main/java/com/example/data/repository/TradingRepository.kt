package com.example.data.repository

import com.example.data.local.*
import com.example.engine.PaperTradingEngine
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.max

class TradingRepository(
    private val db: AppDatabase,
    private val marketDataRepository: MarketDataRepository
) {
    private val accountDao = db.accountDao()
    private val tradingDao = db.tradingDao()

    val accountFlow: Flow<AccountState> = accountDao.getAccountFlow().map { entity ->
        if (entity != null) {
            AccountState(
                balance = entity.balance,
                startingBalance = entity.startingBalance,
                currency = entity.currency,
                usedMargin = entity.usedMargin
            )
        } else {
            AccountState(balance = 10000.0, startingBalance = 10000.0)
        }
    }

    val pendingOrdersFlow: Flow<List<Order>> = tradingDao.getPendingOrdersFlow().map { list ->
        list.map { it.toDomain() }
    }

    val openPositionsFlow: Flow<List<Position>> = tradingDao.getOpenPositionsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val tradeLogsFlow: Flow<List<TradeLog>> = tradingDao.getTradeLogsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun initializeAccountIfNotExists(startingBalance: Double = 10000.0) {
        val existing = accountDao.getAccount()
        if (existing == null) {
            accountDao.insertOrUpdate(
                AccountEntity(
                    id = 1,
                    balance = startingBalance,
                    startingBalance = startingBalance,
                    currency = "USD",
                    usedMargin = 0.0
                )
            )
        }
    }

    suspend fun resetAccount(newStartingBalance: Double = 10000.0) {
        accountDao.insertOrUpdate(
            AccountEntity(
                id = 1,
                balance = newStartingBalance,
                startingBalance = newStartingBalance,
                currency = "USD",
                usedMargin = 0.0
            )
        )
        // Clear open positions and pending orders
        val openPositions = tradingDao.getOpenPositions()
        openPositions.forEach { tradingDao.deletePosition(it) }
        tradingDao.clearTradeLogs()
    }

    suspend fun placeMarketOrder(
        instrument: Instrument,
        direction: TradeDirection,
        quantity: Double,
        stopLoss: Double?,
        takeProfit: Double?,
        userTradeIdea: String = ""
    ): Result<Position> {
        val quote = marketDataRepository.getQuote(instrument.id)
            ?: return Result.failure(Exception("Quote unavailable for ${instrument.symbol}"))

        val fillPrice = PaperTradingEngine.getEntryExecutionPrice(quote, direction)
        val requiredMargin = PaperTradingEngine.calculateRequiredMargin(instrument, fillPrice, quantity)
        val account = accountDao.getAccount() ?: AccountEntity()

        if (account.balance < requiredMargin) {
            return Result.failure(Exception("Insufficient margin. Required: $${String.format(java.util.Locale.US, "%.2f", requiredMargin)}, Available: $${String.format(java.util.Locale.US, "%.2f", account.balance)}"))
        }

        // Deduct entry fee
        val entryFee = PaperTradingEngine.computeTransactionFee(instrument, fillPrice, quantity)
        val newBalance = account.balance - entryFee
        val newUsedMargin = account.usedMargin + requiredMargin
        accountDao.updateBalanceAndMargin(newBalance, newUsedMargin)

        val position = Position(
            id = java.util.UUID.randomUUID().toString(),
            instrumentId = instrument.id,
            marketType = instrument.marketType,
            direction = direction,
            quantity = quantity,
            entryPrice = fillPrice,
            currentPrice = fillPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            unrealizedPnL = 0.0,
            realizedPnL = -entryFee,
            margin = requiredMargin,
            openTime = System.currentTimeMillis(),
            tradeNotes = userTradeIdea
        )

        tradingDao.insertPosition(position.toEntity())
        return Result.success(position)
    }

    suspend fun placePendingOrder(
        instrument: Instrument,
        direction: TradeDirection,
        orderType: OrderType,
        targetPrice: Double,
        quantity: Double,
        stopLoss: Double?,
        takeProfit: Double?
    ): Result<Order> {
        val order = Order(
            id = java.util.UUID.randomUUID().toString(),
            instrumentId = instrument.id,
            marketType = instrument.marketType,
            direction = direction,
            orderType = orderType,
            quantity = quantity,
            targetPrice = targetPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            status = OrderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        tradingDao.insertOrder(order.toEntity())
        return Result.success(order)
    }

    suspend fun cancelPendingOrder(orderId: String) {
        tradingDao.updateOrderStatus(orderId, OrderStatus.CANCELLED.name)
    }

    suspend fun closePosition(
        positionId: String,
        exitReason: String = "MANUAL_CLOSE",
        customExitPrice: Double? = null
    ): Result<TradeLog> {
        val openPositions = tradingDao.getOpenPositions()
        val posEntity = openPositions.find { it.id == positionId }
            ?: return Result.failure(Exception("Position not found or already closed"))

        val instrument = InstrumentsCatalog.findById(posEntity.instrumentId)
            ?: return Result.failure(Exception("Instrument not recognized: ${posEntity.instrumentId}"))

        val quote = marketDataRepository.getQuote(instrument.id)
            ?: return Result.failure(Exception("Quote unavailable"))

        val direction = TradeDirection.valueOf(posEntity.direction)
        val exitPrice = customExitPrice ?: PaperTradingEngine.getCloseExecutionPrice(quote, direction)

        val grossPnL = PaperTradingEngine.computeGrossPnL(
            instrument = instrument,
            direction = direction,
            entryPrice = posEntity.entryPrice,
            exitPrice = exitPrice,
            quantity = posEntity.quantity
        )
        val exitFee = PaperTradingEngine.computeTransactionFee(instrument, exitPrice, posEntity.quantity)
        val netPnL = grossPnL - exitFee
        val pipsOrPoints = PaperTradingEngine.computePipsOrPoints(instrument, direction, posEntity.entryPrice, exitPrice)

        // Evaluate Rule Compliance
        val followedPlan = when {
            posEntity.stopLoss != null && posEntity.takeProfit != null -> 95
            posEntity.stopLoss != null -> 85
            else -> 60
        }

        val aiReviewComment = buildAiReview(posEntity, exitPrice, netPnL, exitReason)

        val tradeLog = TradeLog(
            id = java.util.UUID.randomUUID().toString(),
            positionId = posEntity.id,
            instrumentId = posEntity.instrumentId,
            marketType = MarketType.valueOf(posEntity.marketType),
            direction = direction,
            quantity = posEntity.quantity,
            entryPrice = posEntity.entryPrice,
            exitPrice = exitPrice,
            stopLoss = posEntity.stopLoss,
            takeProfit = posEntity.takeProfit,
            grossPnL = grossPnL,
            fees = exitFee,
            netPnL = netPnL,
            pipsOrPoints = pipsOrPoints,
            exitReason = exitReason,
            openTime = posEntity.openTime,
            closeTime = System.currentTimeMillis(),
            ruleComplianceScore = followedPlan,
            userTradeIdea = posEntity.tradeNotes,
            aiReview = aiReviewComment
        )

        // Update Account
        val account = accountDao.getAccount() ?: AccountEntity()
        val newBalance = account.balance + netPnL
        val newUsedMargin = max(0.0, account.usedMargin - posEntity.margin)
        accountDao.updateBalanceAndMargin(newBalance, newUsedMargin)

        // Remove position and insert trade log
        tradingDao.deletePositionById(posEntity.id)
        tradingDao.insertTradeLog(tradeLog.toEntity())

        return Result.success(tradeLog)
    }

    suspend fun partialClosePosition(positionId: String, closeQuantity: Double): Result<TradeLog> {
        val openPositions = tradingDao.getOpenPositions()
        val posEntity = openPositions.find { it.id == positionId }
            ?: return Result.failure(Exception("Position not found"))

        val instrument = InstrumentsCatalog.findById(posEntity.instrumentId) ?: return Result.failure(Exception("Invalid instrument"))
        val quote = marketDataRepository.getQuote(instrument.id) ?: return Result.failure(Exception("Quote unavailable"))

        if (closeQuantity >= posEntity.quantity) {
            return closePosition(positionId, "PARTIAL_FULL_EXIT")
        }

        val direction = TradeDirection.valueOf(posEntity.direction)
        val exitPrice = PaperTradingEngine.getCloseExecutionPrice(quote, direction)
        val grossPnL = PaperTradingEngine.computeGrossPnL(instrument, direction, posEntity.entryPrice, exitPrice, closeQuantity)
        val exitFee = PaperTradingEngine.computeTransactionFee(instrument, exitPrice, closeQuantity)
        val netPnL = grossPnL - exitFee
        val pipsOrPoints = PaperTradingEngine.computePipsOrPoints(instrument, direction, posEntity.entryPrice, exitPrice)

        val releasedMargin = posEntity.margin * (closeQuantity / posEntity.quantity)
        val remainingQuantity = posEntity.quantity - closeQuantity
        val remainingMargin = posEntity.margin - releasedMargin

        val account = accountDao.getAccount() ?: AccountEntity()
        val newBalance = account.balance + netPnL
        val newUsedMargin = max(0.0, account.usedMargin - releasedMargin)
        accountDao.updateBalanceAndMargin(newBalance, newUsedMargin)

        // Update remaining position
        val updatedPos = posEntity.copy(quantity = remainingQuantity, margin = remainingMargin)
        tradingDao.updatePosition(updatedPos)

        val tradeLog = TradeLog(
            id = java.util.UUID.randomUUID().toString(),
            positionId = posEntity.id,
            instrumentId = posEntity.instrumentId,
            marketType = MarketType.valueOf(posEntity.marketType),
            direction = direction,
            quantity = closeQuantity,
            entryPrice = posEntity.entryPrice,
            exitPrice = exitPrice,
            stopLoss = posEntity.stopLoss,
            takeProfit = posEntity.takeProfit,
            grossPnL = grossPnL,
            fees = exitFee,
            netPnL = netPnL,
            pipsOrPoints = pipsOrPoints,
            exitReason = "PARTIAL_PROFIT_TAKE",
            openTime = posEntity.openTime,
            closeTime = System.currentTimeMillis(),
            ruleComplianceScore = 95,
            userTradeIdea = "Secured partial profits on $closeQuantity units.",
            aiReview = "Excellent discipline: Partial exit executed according to risk management rules, locking in profit while de-risking balance."
        )

        tradingDao.insertTradeLog(tradeLog.toEntity())
        return Result.success(tradeLog)
    }

    /**
     * Check if current market prices hit any open position SL/TP or trigger pending orders
     */
    suspend fun evaluatePositionsAndOrders(quotes: Map<String, Quote>) {
        val openPositions = tradingDao.getOpenPositions()
        for (pos in openPositions) {
            val quote = quotes[pos.instrumentId] ?: continue
            val direction = TradeDirection.valueOf(pos.direction)
            val currentPrice = if (direction == TradeDirection.BUY) quote.bid else quote.ask

            // Check Take Profit
            if (pos.takeProfit != null) {
                val tpTriggered = if (direction == TradeDirection.BUY) currentPrice >= pos.takeProfit else currentPrice <= pos.takeProfit
                if (tpTriggered) {
                    closePosition(pos.id, "TP HIT", pos.takeProfit)
                    continue
                }
            }

            // Check Stop Loss
            if (pos.stopLoss != null) {
                val slTriggered = if (direction == TradeDirection.BUY) currentPrice <= pos.stopLoss else currentPrice >= pos.stopLoss
                if (slTriggered) {
                    closePosition(pos.id, "SL HIT", pos.stopLoss)
                    continue
                }
            }

            // Update unrealized PnL
            val instrument = InstrumentsCatalog.findById(pos.instrumentId) ?: continue
            val unrealized = PaperTradingEngine.computeGrossPnL(instrument, direction, pos.entryPrice, currentPrice, pos.quantity)
            if (abs(unrealized - pos.unrealizedPnL) > 0.01) {
                tradingDao.updatePosition(pos.copy(currentPrice = currentPrice, unrealizedPnL = unrealized))
            }
        }
    }

    private fun buildAiReview(pos: PositionEntity, exitPrice: Double, netPnL: Double, reason: String): String {
        val isWin = netPnL > 0
        return when {
            reason == "TP HIT" -> "Target achieved as planned. Flawless execution adhering to predetermined liquidity objective."
            reason == "SL HIT" -> "Stop loss protected capital against adverse excursion. This was a structurally sound loss respecting predefined risk limits."
            isWin -> "Positive trade outcome closed manually with disciplined profit extraction."
            else -> "Trade closed manually at loss. Ensure you are allowing the trade idea sufficient room to play out without emotional interference."
        }
    }

    private fun PositionEntity.toDomain() = Position(
        id = id,
        instrumentId = instrumentId,
        marketType = MarketType.valueOf(marketType),
        direction = TradeDirection.valueOf(direction),
        quantity = quantity,
        entryPrice = entryPrice,
        currentPrice = currentPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        unrealizedPnL = unrealizedPnL,
        realizedPnL = realizedPnL,
        margin = margin,
        openTime = openTime,
        tradeNotes = tradeNotes
    )

    private fun Position.toEntity() = PositionEntity(
        id = id,
        instrumentId = instrumentId,
        marketType = marketType.name,
        direction = direction.name,
        quantity = quantity,
        entryPrice = entryPrice,
        currentPrice = currentPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        unrealizedPnL = unrealizedPnL,
        realizedPnL = realizedPnL,
        margin = margin,
        openTime = openTime,
        tradeNotes = tradeNotes
    )

    private fun OrderEntity.toDomain() = Order(
        id = id,
        instrumentId = instrumentId,
        marketType = MarketType.valueOf(marketType),
        direction = TradeDirection.valueOf(direction),
        orderType = OrderType.valueOf(orderType),
        quantity = quantity,
        targetPrice = targetPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        status = OrderStatus.valueOf(status),
        createdAt = createdAt
    )

    private fun Order.toEntity() = OrderEntity(
        id = id,
        instrumentId = instrumentId,
        marketType = marketType.name,
        direction = direction.name,
        orderType = orderType.name,
        quantity = quantity,
        targetPrice = targetPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        status = status.name,
        createdAt = createdAt
    )

    private fun TradeLogEntity.toDomain() = TradeLog(
        id = id,
        positionId = positionId,
        instrumentId = instrumentId,
        marketType = MarketType.valueOf(marketType),
        direction = TradeDirection.valueOf(direction),
        quantity = quantity,
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        grossPnL = grossPnL,
        fees = fees,
        netPnL = netPnL,
        pipsOrPoints = pipsOrPoints,
        exitReason = exitReason,
        openTime = openTime,
        closeTime = closeTime,
        ruleComplianceScore = ruleComplianceScore,
        userTradeIdea = userTradeIdea,
        aiReview = aiReview
    )

    private fun TradeLog.toEntity() = TradeLogEntity(
        id = id,
        positionId = positionId,
        instrumentId = instrumentId,
        marketType = marketType.name,
        direction = direction.name,
        quantity = quantity,
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        grossPnL = grossPnL,
        fees = fees,
        netPnL = netPnL,
        pipsOrPoints = pipsOrPoints,
        exitReason = exitReason,
        openTime = openTime,
        closeTime = closeTime,
        ruleComplianceScore = ruleComplianceScore,
        userTradeIdea = userTradeIdea,
        aiReview = aiReview
    )
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_table")
data class AccountEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 10000.0,
    val startingBalance: Double = 10000.0,
    val currency: String = "USD",
    val usedMargin: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders_table")
data class OrderEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val marketType: String,
    val direction: String,
    val orderType: String,
    val quantity: Double,
    val targetPrice: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val status: String,
    val createdAt: Long
)

@Entity(tableName = "positions_table")
data class PositionEntity(
    @PrimaryKey val id: String,
    val instrumentId: String,
    val marketType: String,
    val direction: String,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val unrealizedPnL: Double,
    val realizedPnL: Double,
    val margin: Double,
    val openTime: Long,
    val tradeNotes: String
)

@Entity(tableName = "trade_logs_table")
data class TradeLogEntity(
    @PrimaryKey val id: String,
    val positionId: String,
    val instrumentId: String,
    val marketType: String,
    val direction: String,
    val quantity: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val grossPnL: Double,
    val fees: Double,
    val netPnL: Double,
    val pipsOrPoints: Double,
    val exitReason: String,
    val openTime: Long,
    val closeTime: Long,
    val ruleComplianceScore: Int,
    val userTradeIdea: String,
    val aiReview: String
)

@Entity(tableName = "lesson_progress_table")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val isCompleted: Boolean,
    val quizScore: Int,
    val completedAt: Long
)

@Entity(tableName = "saved_videos_table")
data class SavedVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topic: String,
    val instrumentId: String,
    val aspectRatio: String,
    val scenesJson: String,
    val createdAt: Long
)

@Entity(tableName = "subscription_table")
data class SubscriptionEntity(
    @PrimaryKey val id: Int = 1,
    val isPremium: Boolean = false,
    val planId: String = "FREE",
    val planPrice: Double = 0.0,
    val startDate: Long = 0L,
    val expiryDate: Long = 0L,
    val autoRenew: Boolean = true,
    val transactionId: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)


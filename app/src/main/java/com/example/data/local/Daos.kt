package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account_table WHERE id = 1 LIMIT 1")
    fun getAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM account_table WHERE id = 1 LIMIT 1")
    suspend fun getAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: AccountEntity)

    @Query("UPDATE account_table SET balance = :balance, usedMargin = :usedMargin, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateBalanceAndMargin(balance: Double, usedMargin: Double, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface TradingDao {
    // Orders
    @Query("SELECT * FROM orders_table WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingOrdersFlow(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders_table SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    // Positions
    @Query("SELECT * FROM positions_table ORDER BY openTime DESC")
    fun getOpenPositionsFlow(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions_table")
    suspend fun getOpenPositions(): List<PositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity)

    @Update
    suspend fun updatePosition(position: PositionEntity)

    @Delete
    suspend fun deletePosition(position: PositionEntity)

    @Query("DELETE FROM positions_table WHERE id = :id")
    suspend fun deletePositionById(id: String)

    // Trade Logs (Closed trades)
    @Query("SELECT * FROM trade_logs_table ORDER BY closeTime DESC")
    fun getTradeLogsFlow(): Flow<List<TradeLogEntity>>

    @Query("SELECT * FROM trade_logs_table ORDER BY closeTime DESC")
    suspend fun getTradeLogs(): List<TradeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeLog(tradeLog: TradeLogEntity)

    @Query("DELETE FROM trade_logs_table")
    suspend fun clearTradeLogs()
}

@Dao
interface EducationDao {
    @Query("SELECT * FROM lesson_progress_table")
    fun getAllProgressFlow(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress_table WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getProgress(lessonId: String): LessonProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LessonProgressEntity)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM saved_videos_table ORDER BY createdAt DESC")
    fun getSavedVideosFlow(): Flow<List<SavedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: SavedVideoEntity)

    @Query("DELETE FROM saved_videos_table WHERE id = :id")
    suspend fun deleteVideo(id: String)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_table WHERE id = 1 LIMIT 1")
    fun getSubscriptionFlow(): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscription_table WHERE id = 1 LIMIT 1")
    suspend fun getSubscription(): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubscription(subscription: SubscriptionEntity)

    @Query("UPDATE subscription_table SET isPremium = 0 WHERE id = 1")
    suspend fun cancelSubscription()
}


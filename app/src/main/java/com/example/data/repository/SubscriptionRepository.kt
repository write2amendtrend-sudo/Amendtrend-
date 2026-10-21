package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SubscriptionEntity
import com.example.model.SubscriptionPlan
import com.example.model.UserSubscriptionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SubscriptionRepository(private val db: AppDatabase) {

    val subscriptionFlow: Flow<UserSubscriptionState> = db.subscriptionDao().getSubscriptionFlow().map { entity ->
        if (entity == null || !entity.isPremium || (entity.expiryDate > 0 && entity.expiryDate < System.currentTimeMillis())) {
            UserSubscriptionState(isPremium = false)
        } else {
            val plan = try {
                SubscriptionPlan.valueOf(entity.planId)
            } catch (e: Exception) {
                SubscriptionPlan.MONTHLY
            }
            UserSubscriptionState(
                isPremium = true,
                plan = plan,
                startDate = entity.startDate,
                expiryDate = entity.expiryDate,
                autoRenew = entity.autoRenew,
                transactionId = entity.transactionId
            )
        }
    }

    suspend fun getSubscription(): UserSubscriptionState {
        val entity = db.subscriptionDao().getSubscription()
        return if (entity == null || !entity.isPremium || (entity.expiryDate > 0 && entity.expiryDate < System.currentTimeMillis())) {
            UserSubscriptionState(isPremium = false)
        } else {
            val plan = try {
                SubscriptionPlan.valueOf(entity.planId)
            } catch (e: Exception) {
                SubscriptionPlan.MONTHLY
            }
            UserSubscriptionState(
                isPremium = true,
                plan = plan,
                startDate = entity.startDate,
                expiryDate = entity.expiryDate,
                autoRenew = entity.autoRenew,
                transactionId = entity.transactionId
            )
        }
    }

    suspend fun subscribe(plan: SubscriptionPlan): Result<UserSubscriptionState> {
        val now = System.currentTimeMillis()
        val durationMillis = plan.durationDays.toLong() * 24 * 60 * 60 * 1000
        val expiry = now + durationMillis
        val txId = "GPA." + (1000..9999).random() + "-" + (1000..9999).random() + "-" + (1000..9999).random() + "-" + (10000..99999).random()

        val entity = SubscriptionEntity(
            id = 1,
            isPremium = true,
            planId = plan.name,
            planPrice = plan.priceUsd,
            startDate = now,
            expiryDate = expiry,
            autoRenew = true,
            transactionId = txId,
            updatedAt = now
        )
        db.subscriptionDao().saveSubscription(entity)

        return Result.success(
            UserSubscriptionState(
                isPremium = true,
                plan = plan,
                startDate = now,
                expiryDate = expiry,
                autoRenew = true,
                transactionId = txId
            )
        )
    }

    suspend fun cancelSubscription(): Result<Unit> {
        db.subscriptionDao().cancelSubscription()
        return Result.success(Unit)
    }

    suspend fun restorePurchases(): Result<UserSubscriptionState> {
        val current = getSubscription()
        return if (current.isPremium) {
            Result.success(current)
        } else {
            // If user previously had purchased or wants to restore default yearly institutional tier
            subscribe(SubscriptionPlan.YEARLY)
        }
    }
}

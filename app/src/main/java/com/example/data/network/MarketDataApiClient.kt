package com.example.data.network

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class NetworkQuoteResult(
    val bid: Double,
    val ask: Double,
    val high24h: Double,
    val low24h: Double,
    val changePercent: Double,
    val volume: Double,
    val timestampUtc: Long,
    val dataSource: String
)

class MarketDataApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var lastRateLimitUtc: Long = 0
    private var isRateLimited: Boolean = false

    fun isCurrentlyRateLimited(): Boolean {
        if (isRateLimited && System.currentTimeMillis() - lastRateLimitUtc > 60_000L) {
            isRateLimited = false // Reset cooldown after 60s
        }
        return isRateLimited
    }

    /**
     * Fetch real 24h ticker for crypto from Binance Public API
     */
    suspend fun fetchCrypto24hTicker(symbol: String): Result<NetworkQuoteResult> = withContext(Dispatchers.IO) {
        if (isCurrentlyRateLimited()) {
            return@withContext Result.failure(Exception("API rate limit active (cooldown)."))
        }

        try {
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    isRateLimited = true
                    lastRateLimitUtc = System.currentTimeMillis()
                    return@withContext Result.failure(Exception("Rate limit 429 received from Binance API"))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(bodyStr)

                val bid = json.optDouble("bidPrice", 0.0)
                val ask = json.optDouble("askPrice", 0.0)
                val high = json.optDouble("highPrice", 0.0)
                val low = json.optDouble("lowPrice", 0.0)
                val change = json.optDouble("priceChangePercent", 0.0)
                val volume = json.optDouble("volume", 0.0)
                val time = json.optLong("closeTime", System.currentTimeMillis())

                Result.success(
                    NetworkQuoteResult(
                        bid = if (bid > 0) bid else json.optDouble("lastPrice", 0.0) * 0.9998,
                        ask = if (ask > 0) ask else json.optDouble("lastPrice", 0.0) * 1.0002,
                        high24h = high,
                        low24h = low,
                        changePercent = change,
                        volume = volume,
                        timestampUtc = time,
                        dataSource = "Binance Market Data API (Live Spot)"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch real candlesticks for crypto from Binance Public API
     */
    suspend fun fetchCryptoKlines(symbol: String, interval: String, limit: Int = 60): Result<List<Candle>> = withContext(Dispatchers.IO) {
        if (isCurrentlyRateLimited()) {
            return@withContext Result.failure(Exception("API rate limit active"))
        }

        try {
            val url = "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    isRateLimited = true
                    lastRateLimitUtc = System.currentTimeMillis()
                    return@withContext Result.failure(Exception("Rate limit 429"))
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonArray = JSONArray(bodyStr)
                val candles = mutableListOf<Candle>()

                for (i in 0 until jsonArray.length()) {
                    val kline = jsonArray.getJSONArray(i)
                    val openTimeUtc = kline.getLong(0)
                    val open = kline.getString(1).toDoubleOrNull() ?: 0.0
                    val high = kline.getString(2).toDoubleOrNull() ?: 0.0
                    val low = kline.getString(3).toDoubleOrNull() ?: 0.0
                    val close = kline.getString(4).toDoubleOrNull() ?: 0.0
                    val volume = kline.getString(5).toDoubleOrNull() ?: 0.0

                    candles.add(
                        Candle(
                            timestamp = openTimeUtc,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume,
                            dataSource = "Binance Market Data API (Live Spot)"
                        )
                    )
                }

                Result.success(candles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch real official reference Forex exchange rates from Frankfurter (ECB API)
     */
    suspend fun fetchForexRates(): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.frankfurter.dev/v1/latest?base=USD"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(bodyStr)
                val ratesObj = json.getJSONObject("rates")

                val map = mutableMapOf<String, Double>()
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = ratesObj.getDouble(key)
                }

                Result.success(map)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

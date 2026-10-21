package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class MarketType(val displayName: String) {
    FOREX("Forex"),
    INDIAN_MARKET("Indian Markets"),
    CRYPTO("Cryptocurrency")
}

enum class OrderType {
    MARKET,
    LIMIT,
    STOP
}

enum class TradeDirection {
    BUY,
    SELL
}

enum class OrderStatus {
    PENDING,
    FILLED,
    CANCELLED,
    REJECTED
}

enum class PositionStatus {
    OPEN,
    CLOSED
}

enum class Timeframe(val label: String, val minutes: Int, val binanceInterval: String) {
    M1("1m", 1, "1m"),
    M5("5m", 5, "5m"),
    M15("15m", 15, "15m"),
    H1("1h", 60, "1h"),
    H4("4h", 240, "4h"),
    D1("1D", 1440, "1d")
}

enum class DataType(val label: String) {
    LIVE("LIVE"),
    DELAYED("DELAYED 15M"),
    HISTORICAL("HISTORICAL REPLAY"),
    MARKET_CLOSED("MARKET CLOSED")
}

enum class ConnectionStatus(val label: String) {
    CONNECTED("Connected (Live Feed)"),
    RECONNECTING("Reconnecting..."),
    RATE_LIMITED("Rate Limited (Cached)"),
    FALLBACK_SIMULATION("Simulation Mode (Offline)")
}

enum class UserTimezone(val code: String, val displayName: String, val timeZoneId: String) {
    UTC("UTC", "UTC (Coordinated Universal Time)", "UTC"),
    IST("IST", "IST (India Standard Time UTC+5:30)", "Asia/Kolkata"),
    EST("EST", "EST (US Eastern Time UTC-5)", "America/New_York"),
    GMT("GMT", "GMT / London (UTC+0)", "Europe/London"),
    JST("JST", "JST (Tokyo Time UTC+9)", "Asia/Tokyo");

    fun formatTimestamp(utcMillis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(utcMillis))
    }

    fun formatTimeOnly(utcMillis: Long): String {
        return formatTimestamp(utcMillis, "HH:mm:ss")
    }

    companion object {
        fun fromCode(code: String): UserTimezone {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: UTC
        }
    }
}

data class Instrument(
    val id: String,
    val symbol: String,
    val name: String,
    val marketType: MarketType,
    val baseCurrency: String,
    val quoteCurrency: String,
    val pipOrTickSize: Double,
    val contractSize: Double, // Forex: 100,000 for standard lot; Equities: 1 share; Crypto: 1 coin
    val minQuantity: Double,  // e.g. 0.01 lot for Forex, 1 share for NSE, 0.001 for BTC
    val quantityStep: Double,
    val baseSpreadPipsOrTicks: Double,
    val exchange: String,     // e.g. "Global FX", "NSE", "Crypto Spot"
    val tradingSession: String, // e.g. "24/5", "09:15 - 15:30 IST", "24/7"
    val isIndex: Boolean = false,
    val isTradable: Boolean = true // Indices require simulated ETF/Future
) {
    fun formatPrice(price: Double): String {
        return when {
            pipOrTickSize <= 0.0001 -> String.format(Locale.US, "%.5f", price)
            pipOrTickSize <= 0.01 -> String.format(Locale.US, "%.2f", price)
            else -> String.format(Locale.US, "%.2f", price)
        }
    }
}

data class Quote(
    val instrumentId: String,
    val bid: Double,
    val ask: Double,
    val high24h: Double,
    val low24h: Double,
    val change24hPercent: Double,
    val volume24h: Double,
    val timestamp: Long, // Stored internally as UTC milliseconds
    val dataType: DataType = DataType.LIVE,
    val dataSource: String = "Licensed Market Provider",
    val isMarketOpen: Boolean = true,
    val sessionNotice: String = "Session Active",
    val lastSyncUtc: Long = System.currentTimeMillis()
) {
    val mid: Double get() = (bid + ask) / 2.0
    val spread: Double get() = ask - bid
}

data class Candle(
    val timestamp: Long, // UTC milliseconds
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val dataSource: String = "Market Feed"
) {
    val isBullish: Boolean get() = close >= open
}

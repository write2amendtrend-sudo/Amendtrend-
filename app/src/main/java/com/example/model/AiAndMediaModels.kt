package com.example.model

enum class TrendDirection {
    BULLISH,
    BEARISH,
    RANGING
}

enum class StructureType {
    HIGHER_HIGH,
    HIGHER_LOW,
    LOWER_HIGH,
    LOWER_LOW
}

data class StructurePoint(
    val index: Int,
    val timestamp: Long,
    val price: Double,
    val type: StructureType
)

data class SmcEvent(
    val type: String, // "BOS", "CHoCH", "LIQUIDITY_SWEEP", "ORDER_BLOCK", "FAIR_VALUE_GAP"
    val priceLevel: Double,
    val startIndex: Int,
    val endIndex: Int,
    val isBullish: Boolean,
    val label: String
)

data class MarketStructureAnalysis(
    val instrumentId: String,
    val timeframe: Timeframe,
    val trend: TrendDirection,
    val structurePoints: List<StructurePoint>,
    val smcEvents: List<SmcEvent>,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val setupRecommendation: String, // e.g. "BULLISH_ORDER_BLOCK_RETEST", "NO VALID SETUP"
    val setupConfidence: String, // "High", "Moderate", "None"
    val entryPrice: Double?,
    val stopLossPrice: Double?,
    val takeProfitPrice: Double?,
    val riskToReward: Double,
    val observedConditions: String,
    val supportingEvidence: List<String>,
    val invalidationCondition: String,
    val cautionReasons: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val isAiEnhanced: Boolean = false
)

// Education Models
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class Lesson(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val readTimeMinutes: Int,
    val contentMarkdown: String,
    val keyTakeaways: List<String>,
    val quiz: List<QuizQuestion>,
    val isVipOnly: Boolean = false
)

// Video Studio Models
data class VideoScene(
    val sceneIndex: Int,
    val sceneTitle: String,
    val visualType: String, // "INTRO_LOGO", "CHART_SETUP", "SMC_ANNOTATION", "ORDER_EXECUTION", "OUTRO"
    val narrationText: String,
    val durationSeconds: Int,
    val chartHighlight: String = ""
)

data class VideoProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val topic: String,
    val instrumentId: String,
    val aspectRatio: String = "16:9", // "16:9" or "9:16"
    val scenes: List<VideoScene>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Int get() = scenes.sumOf { it.durationSeconds }
    val fullTranscript: String get() = scenes.joinToString("\n\n") { "[Scene ${it.sceneIndex + 1} - ${it.sceneTitle}]\n${it.narrationText}" }
}

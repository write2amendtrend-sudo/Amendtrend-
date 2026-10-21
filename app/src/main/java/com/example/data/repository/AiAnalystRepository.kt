package com.example.data.repository

import com.example.BuildConfig
import com.example.engine.SmartMoneyConceptEngine
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiAnalystRepository(
    private val marketDataRepository: MarketDataRepository
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeInstrument(
        instrument: Instrument,
        timeframe: Timeframe
    ): MarketStructureAnalysis = withContext(Dispatchers.Default) {
        val candles = marketDataRepository.getCandles(instrument, timeframe, count = 60)
        val baseAnalysis = SmartMoneyConceptEngine.analyzeCandles(instrument, timeframe, candles)

        // Try AI enrichment if GEMINI_API_KEY is available
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && baseAnalysis.setupRecommendation != "NO VALID SETUP") {
            try {
                val prompt = """
                    You are an institutional ICT/SMC Trading Analyst for Amend Trend.
                    Analyze this setup:
                    Instrument: ${instrument.symbol} (${instrument.marketType.displayName})
                    Timeframe: ${timeframe.label}
                    Current Trend: ${baseAnalysis.trend}
                    Key Setup: ${baseAnalysis.setupRecommendation}
                    Support: ${instrument.formatPrice(baseAnalysis.supportLevel)}
                    Resistance: ${instrument.formatPrice(baseAnalysis.resistanceLevel)}
                    Provide 2 concise sentences of institutional commentary, psychological mindset advice, and session context.
                """.trimIndent()

                val enrichedText = queryGeminiApi(apiKey, prompt)
                if (enrichedText.isNotBlank()) {
                    val updatedEvidence = baseAnalysis.supportingEvidence.toMutableList()
                    updatedEvidence.add("Institutional Context: $enrichedText")
                    return@withContext baseAnalysis.copy(
                        supportingEvidence = updatedEvidence,
                        isAiEnhanced = true
                    )
                }
            } catch (_: Exception) {
                // Fall back gracefully to deterministic analysis
            }
        }

        return@withContext baseAnalysis
    }

    private suspend fun queryGeminiApi(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext ""
            val bodyString = response.body?.string() ?: return@withContext ""
            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates") ?: return@withContext ""
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "")
                }
            }
            ""
        }
    }
}

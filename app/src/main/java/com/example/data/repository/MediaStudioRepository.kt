package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SavedVideoEntity
import com.example.model.Instrument
import com.example.model.VideoProject
import com.example.model.VideoScene
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class MediaStudioRepository(private val db: AppDatabase) {
    private val videoDao = db.videoDao()

    val savedVideosFlow: Flow<List<VideoProject>> = videoDao.getSavedVideosFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveVideo(project: VideoProject) {
        videoDao.insertVideo(project.toEntity())
    }

    suspend fun deleteVideo(id: String) {
        videoDao.deleteVideo(id)
    }

    fun generateVideoProject(
        instrument: Instrument,
        topic: String,
        aspectRatio: String = "16:9"
    ): VideoProject {
        val title = when (topic) {
            "SMC_LIQUIDITY" -> "Mastering Liquidity Sweeps on ${instrument.symbol}"
            "BOS_CHOCH" -> "Break of Structure vs Change of Character on ${instrument.symbol}"
            "ORDER_BLOCKS" -> "Institutional Order Block Blueprint for ${instrument.symbol}"
            else -> "Style Shift: Price Action Masterclass on ${instrument.symbol}"
        }

        val scenes = listOf(
            VideoScene(
                sceneIndex = 0,
                sceneTitle = "Brand Intro: Amend Trend Style Shift",
                visualType = "INTRO_LOGO",
                narrationText = "Welcome to Amend Trend. In this breakdown, we examine how institutional order flow shifts market dynamics on ${instrument.symbol}.",
                durationSeconds = 5,
                chartHighlight = "Amend Trend Logo Intro"
            ),
            VideoScene(
                sceneIndex = 1,
                sceneTitle = "Market Context & Higher Timeframe Trend",
                visualType = "CHART_SETUP",
                narrationText = "Examining the market structure on ${instrument.symbol}, notice how price respect major session levels and consolidates before institutional displacement.",
                durationSeconds = 8,
                chartHighlight = "Key Higher Timeframe Swing Points"
            ),
            VideoScene(
                sceneIndex = 2,
                sceneTitle = "Identifying Liquidity & Structure Shift",
                visualType = "SMC_ANNOTATION",
                narrationText = "Notice the aggressive wick piercing buy-side liquidity, followed by an immediate displacement candle leaving a Fair Value Gap. This is the institutional footprint.",
                durationSeconds = 10,
                chartHighlight = "Liquidity Sweep & FVG Imbalance"
            ),
            VideoScene(
                sceneIndex = 3,
                sceneTitle = "Precision Execution & Risk Parameters",
                visualType = "ORDER_EXECUTION",
                narrationText = "We limit our risk strictly to 1% of account equity. The invalidation stop loss sits just beyond the order block, targeting external liquidity for a 3 to 1 reward-to-risk ratio.",
                durationSeconds = 9,
                chartHighlight = "Entry at Retest, SL below OB, TP at Liquidity Pool"
            ),
            VideoScene(
                sceneIndex = 4,
                sceneTitle = "Outro & Key Takeaways",
                visualType = "OUTRO",
                narrationText = "Remember: true trading profitability comes from risk discipline and patience, not chasing trades. Shift your style with Amend Trend.",
                durationSeconds = 6,
                chartHighlight = "Amend Trend Outro & Checklist"
            )
        )

        return VideoProject(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            topic = topic,
            instrumentId = instrument.id,
            aspectRatio = aspectRatio,
            scenes = scenes,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun SavedVideoEntity.toDomain(): VideoProject {
        val scenes = mutableListOf<VideoScene>()
        try {
            val jsonArray = JSONArray(scenesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                scenes.add(
                    VideoScene(
                        sceneIndex = obj.getInt("index"),
                        sceneTitle = obj.getString("title"),
                        visualType = obj.getString("visualType"),
                        narrationText = obj.getString("narration"),
                        durationSeconds = obj.getInt("duration"),
                        chartHighlight = obj.optString("highlight", "")
                    )
                )
            }
        } catch (_: Exception) { }

        return VideoProject(
            id = id,
            title = title,
            topic = topic,
            instrumentId = instrumentId,
            aspectRatio = aspectRatio,
            scenes = scenes,
            createdAt = createdAt
        )
    }

    private fun VideoProject.toEntity(): SavedVideoEntity {
        val jsonArray = JSONArray()
        scenes.forEach { scene ->
            val obj = JSONObject().apply {
                put("index", scene.sceneIndex)
                put("title", scene.sceneTitle)
                put("visualType", scene.visualType)
                put("narration", scene.narrationText)
                put("duration", scene.durationSeconds)
                put("highlight", scene.chartHighlight)
            }
            jsonArray.put(obj)
        }

        return SavedVideoEntity(
            id = id,
            title = title,
            topic = topic,
            instrumentId = instrumentId,
            aspectRatio = aspectRatio,
            scenesJson = jsonArray.toString(),
            createdAt = createdAt
        )
    }
}

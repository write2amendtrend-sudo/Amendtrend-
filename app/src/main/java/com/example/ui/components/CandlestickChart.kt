package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    instrument: Instrument,
    modifier: Modifier = Modifier,
    userTimezone: UserTimezone = UserTimezone.UTC,
    smcEvents: List<SmcEvent> = emptyList(),
    supportLevel: Double? = null,
    resistanceLevel: Double? = null,
    entryLevel: Double? = null,
    stopLossLevel: Double? = null,
    takeProfitLevel: Double? = null
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading market data...", color = SecondaryText, fontSize = 14.sp)
        }
        return
    }

    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }
    var selectedCandle by remember { mutableStateOf<Candle?>(null) }

    val visibleCandles = remember(candles) { candles.takeLast(45) }

    val minPrice = remember(visibleCandles, stopLossLevel, takeProfitLevel) {
        val candleMin = visibleCandles.minOfOrNull { it.low } ?: 1.0
        val slMin = stopLossLevel ?: candleMin
        min(candleMin, slMin) * 0.9985
    }
    val maxPrice = remember(visibleCandles, stopLossLevel, takeProfitLevel) {
        val candleMax = visibleCandles.maxOfOrNull { it.high } ?: 1.0
        val tpMax = takeProfitLevel ?: candleMax
        max(candleMax, tpMax) * 1.0015
    }

    val priceRange = remember(minPrice, maxPrice) { max(0.00001, maxPrice - minPrice) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(8.dp)
    ) {
        // Top Header info / Selected candle crosshair info
        val candleToShow = selectedCandle ?: visibleCandles.lastOrNull()
        if (candleToShow != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O: ${instrument.formatPrice(candleToShow.open)}", color = SecondaryText, fontSize = 11.sp)
                    Text("H: ${instrument.formatPrice(candleToShow.high)}", color = BullishGreen, fontSize = 11.sp)
                    Text("L: ${instrument.formatPrice(candleToShow.low)}", color = BearishRed, fontSize = 11.sp)
                    Text("C: ${instrument.formatPrice(candleToShow.close)}", color = PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${visibleCandles.size} Bars",
                    color = MintGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Timestamp in selected timezone and data source attribution
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${userTimezone.code}: ${userTimezone.formatTimestamp(candleToShow.timestamp)}",
                    color = MintGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = candleToShow.dataSource,
                    color = SecondaryText,
                    fontSize = 9.sp
                )
            }
        }

        // Main Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                crosshairOffset = offset
                                val candleWidth = size.width / visibleCandles.size
                                val index = (offset.x / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                                selectedCandle = visibleCandles[index]
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                crosshairOffset = change.position
                                val candleWidth = size.width / visibleCandles.size
                                val index = (change.position.x / candleWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                                selectedCandle = visibleCandles[index]
                            },
                            onDragEnd = {
                                crosshairOffset = null
                                selectedCandle = null
                            }
                        )
                    }
            ) {
                val chartWidth = size.width - 55.dp.toPx() // leave right margin for price scale
                val chartHeight = size.height
                val count = visibleCandles.size
                val candleSlot = chartWidth / count
                val candleWidth = max(2.5f, candleSlot * 0.7f)

                fun priceToY(price: Double): Float {
                    val normalized = ((maxPrice - price) / priceRange).toFloat()
                    return normalized * chartHeight
                }

                // 1. Draw Grid Lines
                val gridSteps = 4
                for (step in 0..gridSteps) {
                    val price = minPrice + (priceRange * (step.toDouble() / gridSteps))
                    val y = priceToY(price)
                    drawLine(
                        color = ChartGridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // 2. Draw Support and Resistance zones if provided
                supportLevel?.let { sup ->
                    val y = priceToY(sup)
                    drawLine(
                        color = BullishGreen.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }
                resistanceLevel?.let { res ->
                    val y = priceToY(res)
                    drawLine(
                        color = BearishRed.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                    )
                }

                // 3. Draw Active Trade Levels (Entry, SL, TP)
                entryLevel?.let { entry ->
                    val y = priceToY(entry)
                    drawLine(
                        color = AccentCyan,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 2.5f
                    )
                }
                stopLossLevel?.let { sl ->
                    val y = priceToY(sl)
                    drawLine(
                        color = BearishRed,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                    )
                }
                takeProfitLevel?.let { tp ->
                    val y = priceToY(tp)
                    drawLine(
                        color = BullishGreen,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                    )
                }

                // 4. Draw SMC FVGs & Events
                smcEvents.filter { it.type == "FAIR_VALUE_GAP" }.takeLast(2).forEach { fvg ->
                    val y = priceToY(fvg.priceLevel)
                    val boxHeight = (chartHeight * 0.04f).coerceAtLeast(8f)
                    val boxColor = if (fvg.isBullish) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                    drawRect(
                        color = boxColor,
                        topLeft = Offset(0f, y - boxHeight / 2f),
                        size = Size(chartWidth, boxHeight)
                    )
                }

                // 5. Draw Candlesticks
                visibleCandles.forEachIndexed { index, candle ->
                    val centerX = (index * candleSlot) + (candleSlot / 2f)
                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)

                    val isBullish = candle.close >= candle.open
                    val candleColor = if (isBullish) BullishGreen else BearishRed

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyBottom = max(openY, closeY)
                    val bodyHeight = max(2f, bodyBottom - bodyTop)

                    drawRect(
                        color = candleColor,
                        topLeft = Offset(centerX - candleWidth / 2f, bodyTop),
                        size = Size(candleWidth, bodyHeight)
                    )
                }

                // 6. Draw Crosshair if active
                crosshairOffset?.let { offset ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(0f, offset.y),
                        end = Offset(chartWidth, offset.y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(offset.x, 0f),
                        end = Offset(offset.x, chartHeight),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )
                }
            }

            // Price Scale Column on right
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(55.dp)
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(instrument.formatPrice(maxPrice), color = SecondaryText, fontSize = 9.sp)
                Text(instrument.formatPrice((maxPrice * 2 + minPrice) / 3), color = SecondaryText, fontSize = 9.sp)
                Text(instrument.formatPrice((maxPrice + minPrice * 2) / 3), color = SecondaryText, fontSize = 9.sp)
                Text(instrument.formatPrice(minPrice), color = SecondaryText, fontSize = 9.sp)
            }
        }
    }
}

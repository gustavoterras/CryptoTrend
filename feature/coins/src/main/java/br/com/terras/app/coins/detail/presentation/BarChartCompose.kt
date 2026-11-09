package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlin.math.max
import kotlin.math.min

data class CandlePoint(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val date: String,
    val priceLabel: String = ""
) {
    val isUp: Boolean get() = close >= open
}

fun List<DataPoint>.toCandlePoints(targetCount: Int = 16): List<CandlePoint> {
    if (isEmpty()) return emptyList()
    if (size <= targetCount) {
        return mapIndexed { index, dp ->
            val prevPrice = getOrNull(index - 1)?.y ?: dp.y
            val open = prevPrice
            val close = dp.y
            val high = maxOf(open, close) * 1.0025
            val low = minOf(open, close) * 0.9975
            CandlePoint(
                open = open,
                high = high,
                low = low,
                close = close,
                date = dp.date,
                priceLabel = dp.yLabel ?: dp.y.formatMoney()
            )
        }
    }

    val chunkSize = max(1, size / targetCount)
    return chunked(chunkSize).take(targetCount).map { chunk ->
        val open = chunk.first().y
        val close = chunk.last().y
        var maxPrice = chunk.maxOf { it.y }
        var minPrice = chunk.minOf { it.y }

        if (maxPrice == minPrice) {
            maxPrice *= 1.002
            minPrice *= 0.998
        } else {
            maxPrice = max(maxPrice, max(open, close))
            minPrice = min(minPrice, min(open, close))
        }

        CandlePoint(
            open = open,
            high = maxPrice,
            low = minPrice,
            close = close,
            date = chunk.last().date,
            priceLabel = chunk.last().yLabel ?: close.formatMoney()
        )
    }
}

@Composable
fun BarChartCompose(
    modifier: Modifier = Modifier,
    data: List<DataPoint>,
    coinInfo: CoinDetailVO? = null,
    upColor: Color = Color(0xFF00D09C),
    downColor: Color = Color(0xFFEB5757),
    gridLineColor: Color? = null,
    showFloatingCard: Boolean = true
) {
    if (data.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val effectiveGridLineColor = gridLineColor ?: if (isDark) Color(0x28FFFFFF) else Color(0x18000000)

    val candles = remember(data) { data.toCandlePoints(targetCount = 16) }
    if (candles.isEmpty()) return

    var selectedIndex by remember(candles) { mutableStateOf<Int?>(null) }
    val currentCandle = selectedIndex?.let { candles.getOrNull(it) }

    val overallMin = remember(candles) { candles.minOf { it.low } }
    val overallMax = remember(candles) { candles.maxOf { it.high } }
    val priceRange = remember(overallMin, overallMax) {
        val diff = overallMax - overallMin
        if (diff <= 0.0) 1.0 else diff
    }

    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(candles) {
                    detectTapGestures(
                        onTap = { offset ->
                            val slotWidth = size.width / candles.size
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }
                .pointerInput(candles) {
                    detectDragGestures { change, _ ->
                        val slotWidth = size.width / candles.size
                        val index = (change.position.x / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                        selectedIndex = index
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw horizontal dashed grid lines (5 levels)
            val gridLineCount = 5
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            val paddingVertical = 20.dp.toPx()
            val usableHeight = canvasHeight - (paddingVertical * 2)

            for (i in 0 until gridLineCount) {
                val y = paddingVertical + (usableHeight / (gridLineCount - 1)) * i
                val path = Path().apply {
                    moveTo(0f, y)
                    lineTo(canvasWidth, y)
                }
                drawPath(
                    path = path,
                    color = effectiveGridLineColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )
            }

            // 2. Draw Candlesticks (Bars + Wicks)
            val slotWidth = canvasWidth / candles.size
            val candleBodyWidth = slotWidth * 0.58f
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val wickStrokeWidth = 2.dp.toPx()

            fun priceToY(price: Double): Float {
                val ratio = ((price - overallMin) / priceRange).toFloat()
                return paddingVertical + usableHeight * (1f - ratio)
            }

            candles.forEachIndexed { index, candle ->
                val centerX = slotWidth * index + (slotWidth / 2f)
                val isSelected = selectedIndex == index

                val candleColor = if (candle.isUp) upColor else downColor

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                val bodyTopY = min(openY, closeY)
                val bodyBottomY = max(openY, closeY)
                val rawBodyHeight = bodyBottomY - bodyTopY
                val bodyHeight = max(rawBodyHeight, 4.dp.toPx())

                // Draw vertical indicator line for selected candle
                if (isSelected) {
                    drawLine(
                        color = candleColor.copy(alpha = 0.25f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, canvasHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Draw Wick (thin vertical line from high to low)
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = wickStrokeWidth
                )

                // Draw Rounded Candle Body
                drawRoundRect(
                    color = candleColor,
                    topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTopY),
                    size = Size(candleBodyWidth, bodyHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        // 3. Floating Info Card (appears ONLY when a candle is clicked, placed on opposite side to avoid obscuring selected candle)
        val isSelectedOnRightHalf = (selectedIndex ?: 0) >= (candles.size / 2)
        val cardAlignment = if (isSelectedOnRightHalf) Alignment.BottomStart else Alignment.BottomEnd

        AnimatedVisibility(
            visible = showFloatingCard && currentCandle != null,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(cardAlignment)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (currentCandle != null) {
                val variation = if (currentCandle.open != 0.0) {
                    ((currentCandle.close - currentCandle.open) / currentCandle.open) * 100.0
                } else 0.0

                val cardBgColor = if (isDark) Color(0xFF1E2333) else Color(0xFFFFFFFF)
                val cardBorderColor = if (isDark) Color(0xFF384055) else Color(0xFFE5E7EB)
                val labelColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                val valueColor = if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
                val variationColor = if (currentCandle.isUp) {
                    if (isDark) Color(0xFF00D09C) else Color(0xFF00A878)
                } else {
                    if (isDark) Color(0xFFFF5252) else Color(0xFFE02E49)
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, cardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .widthIn(min = 135.dp, max = 155.dp)
                        .clickable { selectedIndex = null }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CandleInfoRow(label = "Abertura:", value = currentCandle.open.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = "Máximo:", value = currentCandle.high.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = "Mínimo:", value = currentCandle.low.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = "Fechamento:", value = currentCandle.close.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(
                            label = "Variação:",
                            value = variation.formatPercentage(),
                            labelColor = labelColor,
                            valueColor = variationColor,
                            isBold = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandleInfoRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.Medium,
            fontSize = 9.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor,
            fontSize = 9.5.sp
        )
    }
}

@ThemePreviews
@Composable
private fun BarChartComposePreview() {
    DSMTheme {
        Surface {
            BarChartCompose(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                data = listOf(
                    DataPoint("10:00", 34100.0, null, "$34,100"),
                    DataPoint("11:00", 34450.0, null, "$34,450"),
                    DataPoint("12:00", 34200.0, null, "$34,200"),
                    DataPoint("13:00", 33900.0, null, "$33,900"),
                    DataPoint("14:00", 34300.0, null, "$34,300"),
                    DataPoint("15:00", 34800.0, null, "$34,800"),
                    DataPoint("16:00", 34600.0, null, "$34,600"),
                    DataPoint("17:00", 35200.0, null, "$35,200"),
                    DataPoint("18:00", 34950.0, null, "$34,950"),
                    DataPoint("19:00", 34700.0, null, "$34,700"),
                    DataPoint("20:00", 35100.0, null, "$35,100"),
                    DataPoint("21:00", 35400.0, null, "$35,400"),
                    DataPoint("22:00", 35050.0, null, "$35,050"),
                    DataPoint("23:00", 35300.0, null, "$35,300")
                )
            )
        }
    }
}

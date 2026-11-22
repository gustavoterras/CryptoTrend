package br.com.terras.app.coins.detail.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.ChartTradeMarkerVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatMoney
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlin.math.min

private class LineMarkerPositionHolder {
    var items: List<PositionedChartTradeMarker> = emptyList()
}

@Composable
fun LineChartCompose(
    modifier: Modifier = Modifier,
    data: List<DataPoint>,
    graphColor: Color,
    showDashedLine: Boolean,
    showYLabels: Boolean = true,
    showXLabels: Boolean = true,
    showHighLowCallouts: Boolean = true,
    pointFillColor: Color? = null,
    gridLineColor: Color? = null,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    tradeMarkers: List<ChartTradeMarkerVO> = emptyList(),
    selectedMarkerId: Long? = null,
    onMarkerClick: ((ChartTradeMarkerVO?) -> Unit)? = null
) {
    if (data.isEmpty()) {
        return
    }

    val isDark = isSystemInDarkTheme()
    val effectiveGridLineColor = gridLineColor ?: if (isDark) Color(0x1FFFFFFF) else Color(0x140F172A)
    val effectivePointFillColor = pointFillColor ?: colorScheme.primary
    val markerHolder = remember { LineMarkerPositionHolder() }

    val transparentGraphColor = remember(key1 = graphColor) {
        graphColor.copy(alpha = 0.28f)
    }

    val maxIndex = remember(key1 = data) {
        data.indices.maxByOrNull { data[it].y } ?: 0
    }
    val minIndex = remember(key1 = data) {
        data.indices.minByOrNull { data[it].y } ?: 0
    }
    val lowerValue = remember(key1 = data, minIndex) { data[minIndex] }
    val upperValue = remember(key1 = data, maxIndex) { data[maxIndex] }
    val actualDiff = remember(lowerValue, upperValue) { upperValue.y - lowerValue.y }
    val safeRange = remember(actualDiff) { if (actualDiff <= 0.0) 1.0 else actualDiff }

    val gridLineCount = 5
    val yTickLabels = remember(lowerValue, actualDiff, showYLabels) {
        if (!showYLabels) emptyList()
        else (0 until gridLineCount).map { i ->
            val fraction = 1.0 - (i.toDouble() / (gridLineCount - 1))
            (lowerValue.y + actualDiff * fraction).formatAxisMoney()
        }
    }

    val xTickIndices = remember(data, showXLabels) {
        if (!showXLabels) emptyList()
        else {
            val count = data.size
            if (count <= 1) listOf(0)
            else {
                val tickCount = min(4, count)
                (0 until tickCount).map { tick ->
                    ((tick.toFloat() / (tickCount - 1)) * (count - 1)).toInt()
                }.distinct()
            }
        }
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = colorScheme.onSurfaceVariant
    val axisTextStyle = remember(axisLabelColor) {
        TextStyle(
            color = axisLabelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
    val calloutLabelColor = colorScheme.surface
    val calloutTextStyle = remember(calloutLabelColor) {
        TextStyle(
            color = calloutLabelColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }

    val yLabelLayouts = remember(yTickLabels, axisTextStyle) {
        yTickLabels.map { label -> textMeasurer.measure(text = label, style = axisTextStyle) }
    }
    val xLabelLayouts = remember(data, xTickIndices, axisTextStyle) {
        xTickIndices.mapNotNull { index ->
            val dp = data[index]
            val label = dp.xLabel?.takeIf { it.isNotBlank() } ?: dp.date
            if (label.isBlank()) null
            else index to textMeasurer.measure(text = label, style = axisTextStyle)
        }
    }

    val maxPriceText = remember(upperValue) {
        upperValue.yLabel?.takeIf { it.isNotBlank() } ?: upperValue.y.formatMoney()
    }
    val minPriceText = remember(lowerValue) {
        lowerValue.yLabel?.takeIf { it.isNotBlank() } ?: lowerValue.y.formatMoney()
    }
    val maxCalloutLayout = remember(maxPriceText, calloutTextStyle, showHighLowCallouts) {
        if (showHighLowCallouts) textMeasurer.measure(text = maxPriceText, style = calloutTextStyle)
        else null
    }
    val minCalloutLayout = remember(minPriceText, calloutTextStyle, showHighLowCallouts) {
        if (showHighLowCallouts) textMeasurer.measure(text = minPriceText, style = calloutTextStyle)
        else null
    }

    val leftPaddingPx = with(density) { 10.dp.toPx() }
    val yAxisGapPx = with(density) { 8.dp.toPx() }
    val rightPaddingPx = with(density) { 12.dp.toPx() }
    val maxYLabelWidthPx = yLabelLayouts.maxOfOrNull { it.size.width }?.toFloat() ?: 0f
    val chartLeftPx = if (showYLabels) leftPaddingPx + maxYLabelWidthPx + yAxisGapPx else leftPaddingPx

    val tapThresholdPx = with(density) { 26.dp.toPx() }
    val canvasModifier = if (tradeMarkers.isNotEmpty() && onMarkerClick != null) {
        modifier.pointerInput(tradeMarkers, selectedMarkerId, tapThresholdPx) {
            detectTapGestures(
                onTap = { tapOffset ->
                    val hit = findNearestTradeMarker(
                        tapOffset = tapOffset,
                        positionedMarkers = markerHolder.items,
                        maxDistancePx = tapThresholdPx
                    )
                    if (hit != null) {
                        onMarkerClick(if (hit.transactionId == selectedMarkerId) null else hit)
                    } else if (selectedMarkerId != null) {
                        onMarkerClick(null)
                    }
                }
            )
        }
    } else {
        modifier
    }

    Canvas(modifier = canvasModifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val chartRight = (canvasWidth - rightPaddingPx).coerceAtLeast(chartLeftPx + 1f)
        val chartWidth = chartRight - chartLeftPx

        val dotRadiusPx = 4.5.dp.toPx()
        val calloutGapPx = 5.dp.toPx()
        val paddingTop = if (showHighLowCallouts && maxCalloutLayout != null) {
            maxCalloutLayout.size.height + dotRadiusPx + calloutGapPx + 4.dp.toPx()
        } else {
            12.dp.toPx()
        }

        val xAxisTopGap = 6.dp.toPx()
        val bottomPadding = 4.dp.toPx()
        val xLabelHeight = if (showXLabels) {
            xLabelLayouts.maxOfOrNull { it.second.size.height }?.toFloat() ?: 14.dp.toPx()
        } else 0f
        val reservedXAxisBottom = if (showXLabels) xAxisTopGap + xLabelHeight + bottomPadding else bottomPadding
        val bottomCalloutReserve = if (showHighLowCallouts && minCalloutLayout != null && minIndex != maxIndex) {
            minCalloutLayout.size.height + dotRadiusPx + calloutGapPx + 4.dp.toPx()
        } else {
            0f
        }

        val plotBottom = (canvasHeight - reservedXAxisBottom - bottomCalloutReserve)
            .coerceAtLeast(paddingTop + 1f)
        val usableHeight = plotBottom - paddingTop
        val gradientBottom = (canvasHeight - reservedXAxisBottom).coerceAtLeast(plotBottom)

        // 1. Draw subtle horizontal dashed grid lines and left Y-axis labels
        val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        for (i in 0 until gridLineCount) {
            val y = paddingTop + (usableHeight / (gridLineCount - 1)) * i
            val gridPath = Path().apply {
                moveTo(chartLeftPx, y)
                lineTo(chartRight, y)
            }
            drawPath(
                path = gridPath,
                color = effectiveGridLineColor,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = gridDashEffect
                )
            )

            if (showYLabels && i < yLabelLayouts.size) {
                val layout = yLabelLayouts[i]
                val labelX = chartLeftPx - yAxisGapPx - layout.size.width
                val labelY = (y - layout.size.height / 2f).coerceIn(0f, canvasHeight - layout.size.height)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelX, labelY)
                )
            }
        }

        // 2. Compute smooth cubic Bezier price curve passing directly through each point
        fun priceToY(price: Double): Float {
            val ratio = if (actualDiff <= 0.0) 0.5f else ((price - lowerValue.y) / safeRange).toFloat()
            return paddingTop + usableHeight * (1f - ratio)
        }

        fun indexToX(index: Int): Float {
            return if (data.size <= 1) {
                chartLeftPx + chartWidth / 2f
            } else {
                chartLeftPx + (index.toFloat() / data.lastIndex) * chartWidth
            }
        }

        val firstY = priceToY(data.first().y)

        if (data.size == 1) {
            drawCircle(
                color = graphColor,
                radius = 4.dp.toPx(),
                center = Offset(indexToX(0), firstY)
            )
        } else {
            val strokePath = Path().apply {
                moveTo(indexToX(0), firstY)
                for (i in 0 until data.lastIndex) {
                    val x1 = indexToX(i)
                    val y1 = priceToY(data[i].y)
                    val x2 = indexToX(i + 1)
                    val y2 = priceToY(data[i + 1].y)
                    val midX = (x1 + x2) / 2f
                    cubicTo(midX, y1, midX, y2, x2, y2)
                }
            }

            val fillPath = android.graphics.Path(strokePath.asAndroidPath())
                .asComposePath()
                .apply {
                    lineTo(chartRight, gradientBottom)
                    lineTo(chartLeftPx, gradientBottom)
                    close()
                }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        transparentGraphColor,
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = gradientBottom
                )
            )

            drawPath(
                path = strokePath,
                color = graphColor,
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        if (showDashedLine) {
            val dottedPath = Path().apply {
                moveTo(chartLeftPx, firstY)
                lineTo(chartRight, firstY)
            }

            drawPath(
                path = dottedPath,
                color = graphColor.copy(alpha = .8f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 0f)
                )
            )
        }

        // 3. Draw Peak (High) & Trough (Low) hollow emerald ring markers and bold price callouts
        if (showHighLowCallouts && data.isNotEmpty()) {
            val maxX = indexToX(maxIndex)
            val maxY = priceToY(upperValue.y)

            drawCircle(
                color = effectivePointFillColor,
                radius = dotRadiusPx,
                center = Offset(maxX, maxY),
                style = Fill
            )
            drawCircle(
                color = graphColor,
                radius = dotRadiusPx,
                center = Offset(maxX, maxY),
                style = Stroke(width = 2.2.dp.toPx())
            )

            maxCalloutLayout?.let { layout ->
                val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                val labelX = (maxX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)
                val labelY = (maxY - dotRadiusPx - calloutGapPx - layout.size.height)
                    .coerceAtLeast(2.dp.toPx())
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelX, labelY)
                )
            }

            if (minIndex != maxIndex) {
                val minX = indexToX(minIndex)
                val minY = priceToY(lowerValue.y)

                drawCircle(
                    color = effectivePointFillColor,
                    radius = dotRadiusPx,
                    center = Offset(minX, minY),
                    style = Fill
                )
                drawCircle(
                    color = graphColor,
                    radius = dotRadiusPx,
                    center = Offset(minX, minY),
                    style = Stroke(width = 2.2.dp.toPx())
                )

                minCalloutLayout?.let { layout ->
                    val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                    val labelX = (minX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)
                    val maxLabelY = (canvasHeight - reservedXAxisBottom - layout.size.height - 2.dp.toPx())
                        .coerceAtLeast(minY)
                    val labelY = (minY + dotRadiusPx + calloutGapPx).coerceAtMost(maxLabelY)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(labelX, labelY)
                    )
                }
            }
        }

        // 3b. Draw portfolio entry/exit markers mapped by timestamp & execution price
        val positionedMarkers = resolveLineTradeMarkers(
            data = data,
            markers = tradeMarkers,
            chartLeftPx = chartLeftPx,
            chartWidth = chartWidth,
            priceToY = ::priceToY,
            paddingTop = paddingTop,
            plotBottom = plotBottom,
            markerRadiusPx = 6.dp.toPx()
        )
        markerHolder.items = positionedMarkers
        drawTradeMarkers(
            positionedMarkers = positionedMarkers,
            plotBottom = plotBottom,
            pointFillColor = effectivePointFillColor,
            isDark = isDark,
            selectedMarkerId = selectedMarkerId
        )

        // 4. Draw X-axis timestamps at the base of the chart
        if (showXLabels) {
            val xLabelY = (canvasHeight - reservedXAxisBottom) + xAxisTopGap
            val minLabelGapPx = 8.dp.toPx()
            var lastLabelEndX = Float.NEGATIVE_INFINITY

            xLabelLayouts.forEach { (index, layout) ->
                val centerX = indexToX(index)
                val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                val labelX = (centerX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)

                if (labelX >= lastLabelEndX + minLabelGapPx) {
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(labelX, xLabelY)
                    )
                    lastLabelEndX = labelX + layout.size.width
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun LineChartComposePreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            LineChartCompose(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                data = listOf(
                    DataPoint("10/10/2010", 34100.0, "10:00", "$34,100.00"),
                    DataPoint("10/10/2010", 34450.0, "12:00", "$34,450.00"),
                    DataPoint("10/10/2010", 34200.0, "14:00", "$34,200.00"),
                    DataPoint("10/10/2010", 33900.0, "16:00", "$33,900.00"),
                    DataPoint("10/10/2010", 34300.0, "18:00", "$34,300.00"),
                    DataPoint("10/10/2010", 34800.0, "20:00", "$34,800.00"),
                    DataPoint("10/10/2010", 34600.0, "22:00", "$34,600.00"),
                    DataPoint("10/10/2010", 35200.0, "00:00", "$35,200.00"),
                ),
                graphColor = Color(0xFF12A86E),
                showDashedLine = false,
                showYLabels = true
            )
        }
    }
}

package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.coins.detail.domain.ChartTradeMarkerVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class CandleMarkerPositionHolder {
    var items: List<PositionedChartTradeMarker> = emptyList()
}

data class CandlePoint(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val date: String,
    val priceLabel: String = "",
    val xLabel: String = "",
    val hourLabel: String = "",
    val dayLabel: String = "",
    val startTimestampMillis: Long = 0L,
    val endTimestampMillis: Long = 0L
) {
    val isUp: Boolean get() = close >= open
}

fun List<DataPoint>.toCandlePoints(targetCount: Int = 48): List<CandlePoint> {
    if (isEmpty()) return emptyList()
    if (size <= targetCount) {
        return mapIndexed { index, dp ->
            val prev = getOrNull(index - 1)
            val prevPrice = prev?.y ?: dp.y
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
                priceLabel = dp.yLabel ?: dp.y.formatMoney(),
                xLabel = dp.xLabel?.takeIf { it.isNotBlank() } ?: dp.date,
                hourLabel = dp.hourLabel.ifBlank { dp.xLabel.orEmpty() }.ifBlank { dp.date },
                dayLabel = dp.dayLabel.ifBlank { dp.date },
                startTimestampMillis = prev?.timestampMillis ?: dp.timestampMillis,
                endTimestampMillis = dp.timestampMillis
            )
        }
    }

    val chunkSize = max(1, size / targetCount)
    return chunked(chunkSize).take(targetCount).map { chunk ->
        val open = chunk.first().y
        val close = chunk.last().y
        val lastPoint = chunk.last()
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
            date = lastPoint.date,
            priceLabel = lastPoint.yLabel ?: close.formatMoney(),
            xLabel = lastPoint.xLabel?.takeIf { it.isNotBlank() } ?: lastPoint.date,
            hourLabel = lastPoint.hourLabel.ifBlank { lastPoint.xLabel.orEmpty() }.ifBlank { lastPoint.date },
            dayLabel = lastPoint.dayLabel.ifBlank { lastPoint.date },
            startTimestampMillis = chunk.first().timestampMillis,
            endTimestampMillis = lastPoint.timestampMillis
        )
    }
}

@Composable
fun BarChartCompose(
    modifier: Modifier = Modifier,
    data: List<DataPoint>,
    coinInfo: CoinDetailVO? = null,
    selectedDays: Int = DaysOfChart.DAY.value,
    upColor: Color? = null,
    downColor: Color? = null,
    gridLineColor: Color? = null,
    axisLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    calloutColor: Color = MaterialTheme.colorScheme.surface,
    pointFillColor: Color = MaterialTheme.colorScheme.primary,
    showHighLowCallouts: Boolean = true,
    showFloatingCard: Boolean = true,
    tradeMarkers: List<ChartTradeMarkerVO> = emptyList(),
    selectedMarkerId: Long? = null,
    onMarkerClick: ((ChartTradeMarkerVO?) -> Unit)? = null
) {
    if (data.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val effectiveUpColor = upColor ?: if (isDark) Color(0xFF1ED788) else Color(0xFF12A86E)
    val effectiveDownColor = downColor ?: if (isDark) Color(0xFFFF5A52) else Color(0xFFE04838)
    val effectiveGridLineColor = gridLineColor ?: if (isDark) Color(0x1FFFFFFF) else Color(0x140F172A)
    val markerHolder = remember { CandleMarkerPositionHolder() }

    val candles = remember(data) { data.toCandlePoints(targetCount = 48) }
    if (candles.isEmpty()) return

    var selectedIndex by remember(candles) { mutableStateOf<Int?>(null) }
    val currentCandle = selectedIndex?.let { candles.getOrNull(it) }

    val allCandlesMin = remember(candles) { candles.minOf { it.low } }
    val allCandlesMax = remember(candles) { candles.maxOf { it.high } }
    val overallRange = remember(allCandlesMin, allCandlesMax) {
        (allCandlesMax - allCandlesMin).let { if (it <= 0.0) 1.0 else it }
    }

    val candlesPerViewport = 16
    val contentScaleFactor = remember(candles.size) {
        if (candles.size <= candlesPerViewport) 1f
        else candles.size.toFloat() / candlesPerViewport.toFloat()
    }

    val gridLineCount = 5
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val axisTextStyle = remember(axisLabelColor) {
        TextStyle(
            color = axisLabelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
    val calloutTextStyle = remember(calloutColor) {
        TextStyle(
            color = calloutColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }

    val rightReferenceLabels = remember(allCandlesMin, allCandlesMax) {
        (0 until gridLineCount).map { i ->
            val fraction = 1.0 - (i.toDouble() / (gridLineCount - 1))
            (allCandlesMin + (allCandlesMax - allCandlesMin) * fraction).formatAxisMoney()
        }
    }
    val maxYLabelWidthPx = remember(rightReferenceLabels, axisTextStyle) {
        rightReferenceLabels.maxOfOrNull { label ->
            textMeasurer.measure(text = label, style = axisTextStyle).size.width
        }?.toFloat() ?: 0f
    }

    val leftPaddingPx = with(density) { 12.dp.toPx() }
    val yAxisGapPx = with(density) { 8.dp.toPx() }
    val rightOuterPaddingPx = with(density) { 10.dp.toPx() }
    val chartLeftPx = leftPaddingPx
    val reservedRightPx = yAxisGapPx + maxYLabelWidthPx + rightOuterPaddingPx
    val reservedRightDp = with(density) { reservedRightPx.toDp() }

    var canvasWidthPx by remember { mutableIntStateOf(0) }
    val viewportWidthPx = (canvasWidthPx - chartLeftPx - reservedRightPx).coerceAtLeast(1f)
    val totalContentWidthPx = viewportWidthPx * contentScaleFactor
    val maxScrollPx = (totalContentWidthPx - viewportWidthPx).coerceAtLeast(0f)
    val currentMaxScrollPx by rememberUpdatedState(maxScrollPx)

    // scrollBackPx = 0f anchors newest candles at chartRight; positive values scroll backward into older candles on the left
    var scrollBackPx by remember(candles) { mutableFloatStateOf(0f) }

    LaunchedEffect(maxScrollPx) {
        if (scrollBackPx > maxScrollPx) {
            scrollBackPx = maxScrollPx
        }
    }

    val scrollableState = rememberScrollableState { delta ->
        val limit = currentMaxScrollPx
        if (limit <= 0f) return@rememberScrollableState 0f
        val next = (scrollBackPx + delta).coerceIn(0f, limit)
        val consumed = next - scrollBackPx
        scrollBackPx = next
        consumed
    }

    val visibleCandleRange by remember(candles, totalContentWidthPx, viewportWidthPx, maxScrollPx) {
        derivedStateOf {
            if (candles.size <= 1 || totalContentWidthPx <= 0f) {
                0..candles.lastIndex
            } else {
                val effectiveBack = scrollBackPx.coerceIn(0f, maxScrollPx)
                val scrollFromLeft = (maxScrollPx - effectiveBack).coerceIn(0f, maxScrollPx)
                val slot = totalContentWidthPx / candles.size
                val startIdx = (scrollFromLeft / slot).toInt().coerceIn(0, candles.lastIndex)
                val endIdx = ((scrollFromLeft + viewportWidthPx) / slot).toInt().coerceIn(startIdx, candles.lastIndex)
                startIdx..endIdx
            }
        }
    }

    val maxCandleIndex = remember(candles, visibleCandleRange) {
        visibleCandleRange.maxByOrNull { candles[it].high } ?: 0
    }
    val minCandleIndex = remember(candles, visibleCandleRange) {
        visibleCandleRange.minByOrNull { candles[it].low } ?: 0
    }
    val overallMin = remember(candles, minCandleIndex) { candles[minCandleIndex].low }
    val overallMax = remember(candles, maxCandleIndex) { candles[maxCandleIndex].high }

    val targetMinNorm = ((overallMin - allCandlesMin) / overallRange).toFloat().coerceIn(0f, 1f)
    val targetMaxNorm = ((overallMax - allCandlesMin) / overallRange).toFloat().coerceIn(0f, 1f)

    val animatedMinNorm by animateFloatAsState(
        targetValue = targetMinNorm,
        animationSpec = tween(durationMillis = 160),
        visibilityThreshold = 0.0005f,
        label = "candleYMinNorm"
    )
    val animatedMaxNorm by animateFloatAsState(
        targetValue = targetMaxNorm,
        animationSpec = tween(durationMillis = 160),
        visibilityThreshold = 0.0005f,
        label = "candleYMaxNorm"
    )

    val displayMin = allCandlesMin + animatedMinNorm * overallRange
    val displayMax = allCandlesMin + animatedMaxNorm * overallRange
    val actualDiff = (displayMax - displayMin).coerceAtLeast(0.0)
    val priceRange = if (actualDiff <= 0.0) 1.0 else actualDiff

    val yTickLabels = remember(displayMin, actualDiff) {
        (0 until gridLineCount).map { i ->
            val fraction = 1.0 - (i.toDouble() / (gridLineCount - 1))
            (displayMin + actualDiff * fraction).formatAxisMoney()
        }
    }

    val xTickIndices = remember(candles, contentScaleFactor) {
        val count = candles.size
        if (count <= 1) listOf(0)
        else {
            val tickCount = min(count, (4 * contentScaleFactor).roundToInt().coerceAtLeast(4))
            (0 until tickCount).map { tick ->
                ((tick.toFloat() / (tickCount - 1)) * (count - 1)).toInt()
            }.distinct()
        }
    }

    val yLabelLayouts = yTickLabels.map { label ->
        textMeasurer.measure(text = label, style = axisTextStyle)
    }
    val xLabelLayouts = remember(candles, xTickIndices, axisTextStyle) {
        xTickIndices.mapNotNull { index ->
            val label = candles[index].xLabel
            if (label.isBlank()) null
            else index to textMeasurer.measure(text = label, style = axisTextStyle)
        }
    }
    val maxCalloutLayout = remember(overallMax, calloutTextStyle, showHighLowCallouts) {
        if (showHighLowCallouts) textMeasurer.measure(text = overallMax.formatMoney(), style = calloutTextStyle)
        else null
    }
    val minCalloutLayout = remember(overallMin, calloutTextStyle, showHighLowCallouts) {
        if (showHighLowCallouts) textMeasurer.measure(text = overallMin.formatMoney(), style = calloutTextStyle)
        else null
    }

    val tapThresholdPx = with(density) { 24.dp.toPx() }

    BoxWithConstraints(modifier = modifier) {
        val scrollModifier = if (maxScrollPx > 0f) {
            Modifier
                .fillMaxSize()
                .onSizeChanged { canvasWidthPx = it.width }
                .scrollable(
                    state = scrollableState,
                    orientation = Orientation.Horizontal,
                    reverseDirection = false,
                    flingBehavior = ScrollableDefaults.flingBehavior()
                )
        } else {
            Modifier
                .fillMaxSize()
                .onSizeChanged { canvasWidthPx = it.width }
        }

        Canvas(
            modifier = scrollModifier
                .pointerInput(candles, chartLeftPx, reservedRightPx, contentScaleFactor, tradeMarkers, selectedMarkerId, tapThresholdPx) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (tradeMarkers.isNotEmpty() && onMarkerClick != null) {
                                val hit = findNearestTradeMarker(
                                    tapOffset = offset,
                                    positionedMarkers = markerHolder.items,
                                    maxDistancePx = tapThresholdPx
                                )
                                if (hit != null) {
                                    selectedIndex = null
                                    onMarkerClick(if (hit.transactionId == selectedMarkerId) null else hit)
                                    return@detectTapGestures
                                } else if (selectedMarkerId != null) {
                                    onMarkerClick(null)
                                }
                            }
                            val chartRightPx = (size.width - reservedRightPx).coerceAtLeast(chartLeftPx + 1f)
                            val vpWidth = chartRightPx - chartLeftPx
                            val virtualWidth = vpWidth * contentScaleFactor
                            val canvasMaxScroll = (virtualWidth - vpWidth).coerceAtLeast(0f)
                            val effectiveBack = scrollBackPx.coerceIn(0f, canvasMaxScroll)
                            val virtualLeft = chartRightPx + effectiveBack - virtualWidth
                            val slotWidth = virtualWidth / candles.size
                            val contentX = (offset.x - virtualLeft).coerceIn(0f, virtualWidth - 1f)
                            val index = (contentX / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val chartRight = (canvasWidth - reservedRightPx).coerceAtLeast(chartLeftPx + 1f)
            val viewportWidth = chartRight - chartLeftPx
            val virtualWidth = viewportWidth * contentScaleFactor
            val canvasMaxScroll = (virtualWidth - viewportWidth).coerceAtLeast(0f)
            val effectiveBack = scrollBackPx.coerceIn(0f, canvasMaxScroll)
            val virtualLeftPx = chartRight + effectiveBack - virtualWidth

            val dotRadiusPx = 3.5.dp.toPx()
            val calloutGapPx = 4.dp.toPx()
            val paddingTop = if (showHighLowCallouts && maxCalloutLayout != null) {
                maxCalloutLayout.size.height + dotRadiusPx + calloutGapPx + 4.dp.toPx()
            } else {
                12.dp.toPx()
            }
            val xAxisTopGap = 6.dp.toPx()
            val scrollTrackReserve = if (maxScrollPx > 1f) 8.dp.toPx() else 0f
            val bottomPadding = 4.dp.toPx() + scrollTrackReserve
            val xLabelHeight = xLabelLayouts.maxOfOrNull { it.second.size.height }?.toFloat() ?: 14.dp.toPx()
            val reservedXAxisBottom = xAxisTopGap + xLabelHeight + bottomPadding
            val showMinCallout = showHighLowCallouts && minCalloutLayout != null && (overallMax != overallMin || minCandleIndex != maxCandleIndex)
            val bottomCalloutReserve = if (showMinCallout && minCalloutLayout != null) {
                minCalloutLayout.size.height + dotRadiusPx + calloutGapPx + 4.dp.toPx()
            } else {
                0f
            }
            val chartBottom = (canvasHeight - reservedXAxisBottom - bottomCalloutReserve).coerceAtLeast(paddingTop + 1f)
            val usableHeight = chartBottom - paddingTop

            // 1. Draw horizontal dashed grid lines and pinned right Y-axis price labels
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

            for (i in 0 until gridLineCount) {
                val y = paddingTop + (usableHeight / (gridLineCount - 1)) * i
                val path = Path().apply {
                    moveTo(chartLeftPx, y)
                    lineTo(chartRight, y)
                }
                drawPath(
                    path = path,
                    color = effectiveGridLineColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )

                val layout = yLabelLayouts[i]
                val labelX = chartRight + yAxisGapPx
                val labelY = (y - layout.size.height / 2f).coerceIn(0f, canvasHeight - layout.size.height)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelX, labelY)
                )
            }

            // 2. Draw clipped horizontally scrollable candles, callouts, markers, and X-axis timestamps
            val slotWidth = virtualWidth / candles.size
            val candleBodyWidth = slotWidth * 0.58f
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val wickStrokeWidth = 2.dp.toPx()

            fun priceToY(price: Double): Float {
                val ratio = if (actualDiff <= 0.0) 0.5f else ((price - displayMin) / priceRange).toFloat()
                return paddingTop + usableHeight * (1f - ratio)
            }

            clipRect(
                left = chartLeftPx,
                top = 0f,
                right = chartRight,
                bottom = canvasHeight
            ) {
                candles.forEachIndexed { index, candle ->
                    val centerX = virtualLeftPx + slotWidth * index + (slotWidth / 2f)
                    if (centerX + slotWidth < chartLeftPx || centerX - slotWidth > chartRight) {
                        return@forEachIndexed
                    }
                    val isSelected = selectedIndex == index
                    val candleColor = if (candle.isUp) effectiveUpColor else effectiveDownColor

                    val openY = priceToY(candle.open)
                    val closeY = priceToY(candle.close)
                    val highY = priceToY(candle.high)
                    val lowY = priceToY(candle.low)

                    val bodyTopY = min(openY, closeY)
                    val bodyBottomY = max(openY, closeY)
                    val rawBodyHeight = bodyBottomY - bodyTopY
                    val bodyHeight = max(rawBodyHeight, 4.dp.toPx())

                    if (isSelected) {
                        drawLine(
                            color = candleColor.copy(alpha = 0.25f),
                            start = Offset(centerX, paddingTop),
                            end = Offset(centerX, chartBottom),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = wickStrokeWidth
                    )

                    drawRoundRect(
                        color = candleColor,
                        topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTopY),
                        size = Size(candleBodyWidth, bodyHeight),
                        cornerRadius = cornerRadius
                    )
                }

                if (showHighLowCallouts && candles.isNotEmpty()) {
                    val maxCandle = candles[maxCandleIndex]
                    val maxCenterX = virtualLeftPx + slotWidth * maxCandleIndex + (slotWidth / 2f)
                    val maxY = priceToY(maxCandle.high)
                    val maxRingColor = if (maxCandle.isUp) effectiveUpColor else effectiveDownColor

                    drawCircle(
                        color = pointFillColor,
                        radius = dotRadiusPx,
                        center = Offset(maxCenterX, maxY),
                        style = Fill
                    )
                    drawCircle(
                        color = maxRingColor,
                        radius = dotRadiusPx,
                        center = Offset(maxCenterX, maxY),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    maxCalloutLayout?.let { layout ->
                        val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                        val labelX = (maxCenterX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)
                        val labelY = (maxY - dotRadiusPx - calloutGapPx - layout.size.height)
                            .coerceAtLeast(2.dp.toPx())
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(labelX, labelY)
                        )
                    }

                    if (showMinCallout) {
                        val minCandle = candles[minCandleIndex]
                        val minCenterX = virtualLeftPx + slotWidth * minCandleIndex + (slotWidth / 2f)
                        val minY = priceToY(minCandle.low)
                        val minRingColor = if (minCandle.isUp) effectiveUpColor else effectiveDownColor

                        drawCircle(
                            color = pointFillColor,
                            radius = dotRadiusPx,
                            center = Offset(minCenterX, minY),
                            style = Fill
                        )
                        drawCircle(
                            color = minRingColor,
                            radius = dotRadiusPx,
                            center = Offset(minCenterX, minY),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        minCalloutLayout?.let { layout ->
                            val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                            val labelX = (minCenterX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)
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

                val positionedMarkers = resolveCandleTradeMarkers(
                    candles = candles,
                    markers = tradeMarkers,
                    chartLeftPx = virtualLeftPx,
                    chartWidth = virtualWidth,
                    priceToY = ::priceToY,
                    paddingTop = paddingTop,
                    chartBottom = chartBottom,
                    markerRadiusPx = 6.dp.toPx(),
                    viewportLeftPx = chartLeftPx,
                    viewportRightPx = chartRight
                )
                markerHolder.items = positionedMarkers
                drawTradeMarkers(
                    positionedMarkers = positionedMarkers,
                    plotBottom = chartBottom,
                    pointFillColor = pointFillColor,
                    isDark = isDark,
                    selectedMarkerId = selectedMarkerId
                )

                val xLabelY = (canvasHeight - reservedXAxisBottom) + xAxisTopGap
                val minLabelGapPx = 10.dp.toPx()
                var lastLabelEndX = Float.NEGATIVE_INFINITY

                xLabelLayouts.forEach { (index, layout) ->
                    val centerX = virtualLeftPx + slotWidth * index + (slotWidth / 2f)
                    val maxAllowedX = (chartRight - layout.size.width).coerceAtLeast(chartLeftPx)
                    val labelX = (centerX - layout.size.width / 2f).coerceIn(chartLeftPx, maxAllowedX)
                    val labelEnd = labelX + layout.size.width

                    if (centerX >= chartLeftPx - layout.size.width &&
                        centerX <= chartRight + layout.size.width &&
                        labelX >= lastLabelEndX + minLabelGapPx
                    ) {
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(labelX, xLabelY)
                        )
                        lastLabelEndX = labelEnd
                    }
                }
            }

            // 3. Subtle horizontal scroll progress indicator along bottom of plot viewport
            if (canvasMaxScroll > 1f) {
                val trackHeight = 2.5.dp.toPx()
                val trackY = canvasHeight - trackHeight - 1.dp.toPx()
                val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                drawRoundRect(
                    color = effectiveGridLineColor,
                    topLeft = Offset(chartLeftPx, trackY),
                    size = Size(viewportWidth, trackHeight),
                    cornerRadius = corner
                )
                val thumbWidth = (viewportWidth / contentScaleFactor).coerceAtLeast(24.dp.toPx())
                val thumbFractionFromLeft = ((canvasMaxScroll - effectiveBack) / canvasMaxScroll).coerceIn(0f, 1f)
                val thumbX = chartLeftPx + (viewportWidth - thumbWidth) * thumbFractionFromLeft
                drawRoundRect(
                    color = effectiveUpColor.copy(alpha = 0.65f),
                    topLeft = Offset(thumbX, trackY),
                    size = Size(thumbWidth, trackHeight),
                    cornerRadius = corner
                )
            }
        }

        // Floating Info Card positioned on opposite side of the tapped candle's on-screen position
        val selectedCenterXOnScreen = selectedIndex?.let { idx ->
            val slot = totalContentWidthPx / candles.size
            val effectiveBack = scrollBackPx.coerceIn(0f, maxScrollPx)
            val virtualLeft = chartLeftPx + viewportWidthPx + effectiveBack - totalContentWidthPx
            virtualLeft + slot * idx + (slot / 2f)
        } ?: 0f
        val viewportCenterX = chartLeftPx + viewportWidthPx / 2f
        val isSelectedOnRightHalf = selectedCenterXOnScreen >= viewportCenterX
        val cardAlignment = if (isSelectedOnRightHalf) Alignment.BottomStart else Alignment.BottomEnd

        AnimatedVisibility(
            visible = showFloatingCard && currentCandle != null,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(cardAlignment)
                .padding(start = 12.dp, end = reservedRightDp + 4.dp, top = 8.dp, bottom = 36.dp)
        ) {
            if (currentCandle != null) {
                val variation = if (currentCandle.open != 0.0) {
                    ((currentCandle.close - currentCandle.open) / currentCandle.open) * 100.0
                } else 0.0

                val cardBgColor = Color(0xFF1E222B)
                val cardBorderColor = Color(0xFF323946)
                val labelColor = Color(0xFF9CA3AF)
                val valueColor = Color(0xFFF9FAFB)
                val variationColor = if (currentCandle.isUp) {
                    Color(0xFF22E588)
                } else {
                    Color(0xFFFF6B63)
                }

                val isDayPeriod = selectedDays <= DaysOfChart.DAY.value
                val occurrenceLabel = if (isDayPeriod) {
                    stringResource(R.string.chart_label_time)
                } else {
                    stringResource(R.string.chart_label_date)
                }
                val occurrenceValue = if (isDayPeriod) currentCandle.hourLabel else currentCandle.dayLabel

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, cardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .widthIn(min = 145.dp, max = 172.dp)
                        .clickable { selectedIndex = null }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CandleInfoRow(
                            label = occurrenceLabel,
                            value = occurrenceValue,
                            labelColor = labelColor,
                            valueColor = valueColor,
                            isBold = true
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = cardBorderColor,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                        CandleInfoRow(label = stringResource(R.string.chart_label_open), value = currentCandle.open.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = stringResource(R.string.chart_label_high), value = currentCandle.high.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = stringResource(R.string.chart_label_low), value = currentCandle.low.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(label = stringResource(R.string.chart_label_close), value = currentCandle.close.formatMoney(), labelColor = labelColor, valueColor = valueColor)
                        CandleInfoRow(
                            label = stringResource(R.string.chart_label_change),
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
        Surface(color = MaterialTheme.colorScheme.primary) {
            BarChartCompose(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                data = listOf(
                    DataPoint("10:00", 34100.0, "10:00", "$34,100"),
                    DataPoint("11:00", 34450.0, "11:00", "$34,450"),
                    DataPoint("12:00", 34200.0, "12:00", "$34,200"),
                    DataPoint("13:00", 33900.0, "13:00", "$33,900"),
                    DataPoint("14:00", 34300.0, "14:00", "$34,300"),
                    DataPoint("15:00", 34800.0, "15:00", "$34,800"),
                    DataPoint("16:00", 34600.0, "16:00", "$34,600"),
                    DataPoint("17:00", 35200.0, "17:00", "$35,200"),
                    DataPoint("18:00", 34950.0, "18:00", "$34,950"),
                    DataPoint("19:00", 34700.0, "19:00", "$34,700"),
                    DataPoint("20:00", 35100.0, "20:00", "$35,100"),
                    DataPoint("21:00", 35400.0, "21:00", "$35,400"),
                    DataPoint("22:00", 35050.0, "22:00", "$35,050"),
                    DataPoint("23:00", 35300.0, "23:00", "$35,300")
                )
            )
        }
    }
}

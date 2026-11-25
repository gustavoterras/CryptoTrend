package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import kotlin.math.roundToInt

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
    showLatestPointPulse: Boolean = true,
    scrollable: Boolean = true,
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

    val overallMin = remember(data) { data.minOf { it.y } }
    val overallMax = remember(data) { data.maxOf { it.y } }
    val overallRange = remember(overallMin, overallMax) {
        (overallMax - overallMin).let { if (it <= 0.0) 1.0 else it }
    }

    val contentScaleFactor = remember(data.size, scrollable) {
        if (!scrollable) 1f
        else when {
            data.size >= 40 -> 3.0f
            data.size >= 16 -> 2.0f
            else -> 1.0f
        }
    }

    val gridLineCount = 5
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
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

    // Estimate right Y-axis width from full dataset range so viewport width stays rock-steady while panning
    val rightReferenceLabels = remember(overallMin, overallRange, showYLabels) {
        if (!showYLabels) emptyList()
        else (0 until gridLineCount).map { i ->
            val fraction = 1.0 - (i.toDouble() / (gridLineCount - 1))
            (overallMin + (overallMax - overallMin) * fraction).formatAxisMoney()
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
    val trailingPointMarginPx = with(density) { 16.dp.toPx() }
    val chartLeftPx = leftPaddingPx
    val reservedRightPx = if (showYLabels) yAxisGapPx + maxYLabelWidthPx + rightOuterPaddingPx else rightOuterPaddingPx

    var canvasWidthPx by remember { mutableIntStateOf(0) }
    val viewportWidthPx = (canvasWidthPx - chartLeftPx - reservedRightPx).coerceAtLeast(1f)
    val usableSpanPx = (viewportWidthPx - trailingPointMarginPx).coerceAtLeast(1f)
    val totalContentWidthPx = usableSpanPx * contentScaleFactor
    val maxScrollPx = (totalContentWidthPx - usableSpanPx).coerceAtLeast(0f)
    val currentMaxScrollPx by rememberUpdatedState(maxScrollPx)

    val pulseTransition = rememberInfiniteTransition(label = "latestPointPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "latestPointPulseRipple"
    )
    val pulseCoreAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "latestPointPulseCore"
    )

    // scrollBackPx = 0f anchors newest prices at chartRight; positive values scroll backward into older history on the left
    var scrollBackPx by remember(data) { mutableFloatStateOf(0f) }

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

    val visibleIndexRange by remember(data, totalContentWidthPx, viewportWidthPx, maxScrollPx) {
        derivedStateOf {
            if (data.size <= 1 || totalContentWidthPx <= 0f) {
                0..data.lastIndex
            } else {
                val effectiveBack = scrollBackPx.coerceIn(0f, maxScrollPx)
                val scrollFromLeft = (maxScrollPx - effectiveBack).coerceIn(0f, maxScrollPx)
                val startIdx = ((scrollFromLeft / totalContentWidthPx) * data.lastIndex)
                    .toInt()
                    .coerceIn(0, data.lastIndex)
                val endIdx = (((scrollFromLeft + viewportWidthPx) / totalContentWidthPx) * data.lastIndex)
                    .toInt()
                    .plus(1)
                    .coerceIn(startIdx, data.lastIndex)
                startIdx..endIdx
            }
        }
    }

    val maxIndex = remember(data, visibleIndexRange) {
        visibleIndexRange.maxByOrNull { data[it].y } ?: 0
    }
    val minIndex = remember(data, visibleIndexRange) {
        visibleIndexRange.minByOrNull { data[it].y } ?: 0
    }
    val lowerValue = remember(data, minIndex) { data[minIndex] }
    val upperValue = remember(data, maxIndex) { data[maxIndex] }

    val targetMinNorm = ((lowerValue.y - overallMin) / overallRange).toFloat().coerceIn(0f, 1f)
    val targetMaxNorm = ((upperValue.y - overallMin) / overallRange).toFloat().coerceIn(0f, 1f)

    val animatedMinNorm by animateFloatAsState(
        targetValue = targetMinNorm,
        animationSpec = tween(durationMillis = 160),
        visibilityThreshold = 0.0005f,
        label = "lineYMinNorm"
    )
    val animatedMaxNorm by animateFloatAsState(
        targetValue = targetMaxNorm,
        animationSpec = tween(durationMillis = 160),
        visibilityThreshold = 0.0005f,
        label = "lineYMaxNorm"
    )

    val displayMin = overallMin + animatedMinNorm * overallRange
    val displayMax = overallMin + animatedMaxNorm * overallRange
    val actualDiff = (displayMax - displayMin).coerceAtLeast(0.0)
    val safeRange = if (actualDiff <= 0.0) 1.0 else actualDiff

    val yTickLabels = remember(displayMin, actualDiff, showYLabels) {
        if (!showYLabels) emptyList()
        else (0 until gridLineCount).map { i ->
            val fraction = 1.0 - (i.toDouble() / (gridLineCount - 1))
            (displayMin + actualDiff * fraction).formatAxisMoney()
        }
    }

    val xTickIndices = remember(data, showXLabels, contentScaleFactor) {
        if (!showXLabels) emptyList()
        else {
            val count = data.size
            if (count <= 1) listOf(0)
            else {
                val tickCount = min(count, (4 * contentScaleFactor).roundToInt().coerceAtLeast(4))
                (0 until tickCount).map { tick ->
                    ((tick.toFloat() / (tickCount - 1)) * (count - 1)).toInt()
                }.distinct()
            }
        }
    }

    val yLabelLayouts = yTickLabels.map { label ->
        textMeasurer.measure(text = label, style = axisTextStyle)
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

    val tapThresholdPx = with(density) { 26.dp.toPx() }
    val scrollModifier = if (maxScrollPx > 0f) {
        modifier
            .onSizeChanged { canvasWidthPx = it.width }
            .scrollable(
                state = scrollableState,
                orientation = Orientation.Horizontal,
                reverseDirection = false,
                flingBehavior = ScrollableDefaults.flingBehavior()
            )
    } else {
        modifier.onSizeChanged { canvasWidthPx = it.width }
    }

    val canvasModifier = if (tradeMarkers.isNotEmpty() && onMarkerClick != null) {
        scrollModifier.pointerInput(tradeMarkers, selectedMarkerId, tapThresholdPx) {
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
        scrollModifier
    }

    Canvas(modifier = canvasModifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val chartRight = (canvasWidth - reservedRightPx).coerceAtLeast(chartLeftPx + 1f)
        val viewportWidth = chartRight - chartLeftPx
        val usableSpan = (viewportWidth - trailingPointMarginPx).coerceAtLeast(1f)
        val virtualWidth = usableSpan * contentScaleFactor
        val canvasMaxScroll = (virtualWidth - usableSpan).coerceAtLeast(0f)
        val effectiveBack = scrollBackPx.coerceIn(0f, canvasMaxScroll)
        val virtualLeftPx = (chartRight - trailingPointMarginPx) + effectiveBack - virtualWidth

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

        // 1. Draw subtle horizontal dashed grid lines and pinned right Y-axis labels
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
                val labelX = chartRight + yAxisGapPx
                val labelY = (y - layout.size.height / 2f).coerceIn(0f, canvasHeight - layout.size.height)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(labelX, labelY)
                )
            }
        }

        fun priceToY(price: Double): Float {
            val ratio = if (actualDiff <= 0.0) 0.5f else ((price - displayMin) / safeRange).toFloat()
            return paddingTop + usableHeight * (1f - ratio)
        }

        fun indexToX(index: Int): Float {
            return if (data.size <= 1) {
                chartLeftPx + viewportWidth / 2f
            } else {
                virtualLeftPx + (index.toFloat() / data.lastIndex) * virtualWidth
            }
        }

        // 2. Draw clipped scrollable curve, gradient fill, high/low callouts, markers, and X labels
        clipRect(
            left = chartLeftPx,
            top = 0f,
            right = chartRight,
            bottom = canvasHeight
        ) {
            val firstY = priceToY(data.first().y)

            if (data.size == 1) {
                drawCircle(
                    color = graphColor,
                    radius = 4.dp.toPx(),
                    center = Offset(indexToX(0), firstY)
                )
            } else {
                val drawStart = (visibleIndexRange.first - 1).coerceAtLeast(0)
                val drawEnd = (visibleIndexRange.last + 1).coerceAtMost(data.lastIndex)
                val startX = indexToX(drawStart)
                val endX = indexToX(drawEnd)

                val strokePath = Path().apply {
                    moveTo(startX, priceToY(data[drawStart].y))
                    for (i in drawStart until drawEnd) {
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
                        lineTo(endX, gradientBottom)
                        lineTo(startX, gradientBottom)
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

            val positionedMarkers = resolveLineTradeMarkers(
                data = data,
                markers = tradeMarkers,
                chartLeftPx = virtualLeftPx,
                chartWidth = virtualWidth,
                priceToY = ::priceToY,
                paddingTop = paddingTop,
                plotBottom = plotBottom,
                markerRadiusPx = 6.dp.toPx(),
                viewportLeftPx = chartLeftPx,
                viewportRightPx = chartRight
            )
            markerHolder.items = positionedMarkers
            drawTradeMarkers(
                positionedMarkers = positionedMarkers,
                plotBottom = plotBottom,
                pointFillColor = effectivePointFillColor,
                isDark = isDark,
                selectedMarkerId = selectedMarkerId
            )

            if (showLatestPointPulse && data.isNotEmpty()) {
                val latestX = indexToX(data.lastIndex)
                val latestY = priceToY(data.last().y)
                val haloMaxRadius = 9.5.dp.toPx()
                val haloMinRadius = 3.5.dp.toPx()
                if (latestX in (chartLeftPx - haloMaxRadius)..(chartRight + haloMaxRadius)) {
                    val currentHaloRadius = haloMinRadius + (haloMaxRadius - haloMinRadius) * pulseProgress
                    val currentHaloAlpha = (1f - pulseProgress) * 0.36f
                    drawCircle(
                        color = graphColor.copy(alpha = currentHaloAlpha),
                        radius = currentHaloRadius,
                        center = Offset(latestX, latestY),
                        style = Fill
                    )
                    drawCircle(
                        color = effectivePointFillColor,
                        radius = 4.2.dp.toPx(),
                        center = Offset(latestX, latestY),
                        style = Fill
                    )
                    drawCircle(
                        color = graphColor.copy(alpha = pulseCoreAlpha),
                        radius = 3.0.dp.toPx(),
                        center = Offset(latestX, latestY),
                        style = Fill
                    )
                }
            }

            if (showXLabels) {
                val xLabelY = (canvasHeight - reservedXAxisBottom) + xAxisTopGap
                val minLabelGapPx = 10.dp.toPx()
                var lastLabelEndX = Float.NEGATIVE_INFINITY

                xLabelLayouts.forEach { (index, layout) ->
                    val centerX = indexToX(index)
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

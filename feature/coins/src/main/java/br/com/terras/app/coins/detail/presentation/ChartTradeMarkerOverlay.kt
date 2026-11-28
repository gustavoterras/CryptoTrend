package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.ChartTradeMarkerVO
import br.com.terras.app.coins.detail.domain.CoinPortfolioInsightVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import br.com.terras.app.dsm.ui.component.OdometerText
import br.com.terras.app.dsm.ui.theme.MoneyFontFamily
import br.com.terras.app.dsm.ui.theme.asMoneyStyle
import kotlin.math.hypot
import kotlin.math.max

data class PositionedChartTradeMarker(
    val marker: ChartTradeMarkerVO,
    val center: Offset
)

internal fun buyMarkerColor(isDark: Boolean): Color =
    if (isDark) Color(0xFF1ED788) else Color(0xFF0D8756)

internal fun sellMarkerColor(isDark: Boolean): Color =
    if (isDark) Color(0xFFFF5A52) else Color(0xFFE04838)

private fun trailingClockToleranceMillis(minTs: Long, maxTs: Long): Long {
    val span = (maxTs - minTs).coerceAtLeast(0L)
    return max((span * 0.02).toLong(), 15L * 60L * 1000L)
}

fun filterVisibleTradeMarkers(
    markers: List<ChartTradeMarkerVO>,
    minTimestampMillis: Long,
    maxTimestampMillis: Long
): List<ChartTradeMarkerVO> {
    if (markers.isEmpty() || minTimestampMillis <= 0L || maxTimestampMillis <= minTimestampMillis) {
        return emptyList()
    }
    val upperBound = maxTimestampMillis + trailingClockToleranceMillis(minTimestampMillis, maxTimestampMillis)
    return markers.filter { it.timestampMillis in minTimestampMillis..upperBound }
}

fun resolveLineTradeMarkers(
    data: List<DataPoint>,
    markers: List<ChartTradeMarkerVO>,
    chartLeftPx: Float,
    chartWidth: Float,
    priceToY: (Double) -> Float,
    paddingTop: Float,
    plotBottom: Float,
    markerRadiusPx: Float,
    viewportLeftPx: Float = chartLeftPx,
    viewportRightPx: Float = chartLeftPx + chartWidth
): List<PositionedChartTradeMarker> {
    if (data.size < 2 || markers.isEmpty() || chartWidth <= 0f) return emptyList()

    val minTs = data.first().timestampMillis
    val maxTs = data.last().timestampMillis
    val visible = filterVisibleTradeMarkers(markers, minTs, maxTs)
    if (visible.isEmpty()) return emptyList()

    val minY = paddingTop + markerRadiusPx
    val maxY = (plotBottom - markerRadiusPx).coerceAtLeast(minY)
    val allowedLeft = viewportLeftPx - markerRadiusPx
    val allowedRight = viewportRightPx + markerRadiusPx

    return visible.mapNotNull { marker ->
        val clampedTs = marker.timestampMillis.coerceIn(minTs, maxTs)
        var fraction = 1f
        for (i in 0 until data.lastIndex) {
            val tsA = data[i].timestampMillis
            val tsB = data[i + 1].timestampMillis
            if (clampedTs <= tsB || i == data.lastIndex - 1) {
                val segSpan = tsB - tsA
                val localT = if (segSpan > 0L) {
                    ((clampedTs - tsA).toDouble() / segSpan.toDouble()).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                fraction = ((i + localT) / data.lastIndex.toFloat()).coerceIn(0f, 1f)
                break
            }
        }
        val rawX = chartLeftPx + fraction * chartWidth
        if (rawX < allowedLeft || rawX > allowedRight) return@mapNotNull null
        val x = rawX.coerceIn(viewportLeftPx + markerRadiusPx, (viewportRightPx - markerRadiusPx).coerceAtLeast(viewportLeftPx + markerRadiusPx))
        val y = priceToY(marker.unitPrice).coerceIn(minY, maxY)
        PositionedChartTradeMarker(marker = marker, center = Offset(x, y))
    }
}

fun resolveCandleTradeMarkers(
    candles: List<CandlePoint>,
    markers: List<ChartTradeMarkerVO>,
    chartLeftPx: Float,
    chartWidth: Float,
    priceToY: (Double) -> Float,
    paddingTop: Float,
    chartBottom: Float,
    markerRadiusPx: Float,
    viewportLeftPx: Float = chartLeftPx,
    viewportRightPx: Float = chartLeftPx + chartWidth
): List<PositionedChartTradeMarker> {
    if (candles.isEmpty() || markers.isEmpty() || chartWidth <= 0f) return emptyList()

    val minTs = candles.first().startTimestampMillis
    val maxTs = candles.last().endTimestampMillis
    val visible = filterVisibleTradeMarkers(markers, minTs, maxTs)
    if (visible.isEmpty()) return emptyList()

    val slotWidth = chartWidth / candles.size
    val minY = paddingTop + markerRadiusPx
    val maxY = (chartBottom - markerRadiusPx).coerceAtLeast(minY)
    val allowedLeft = viewportLeftPx - markerRadiusPx
    val allowedRight = viewportRightPx + markerRadiusPx

    return visible.mapNotNull { marker ->
        val clampedTs = marker.timestampMillis.coerceIn(minTs, maxTs)
        val candleIndex = candles.indexOfFirst { clampedTs <= it.endTimestampMillis }
            .let { if (it < 0) candles.lastIndex else it }
        val candle = candles[candleIndex]
        val span = (candle.endTimestampMillis - candle.startTimestampMillis).coerceAtLeast(1L)
        val localT = if (candle.endTimestampMillis > candle.startTimestampMillis) {
            ((clampedTs - candle.startTimestampMillis).toDouble() / span.toDouble())
                .toFloat()
                .coerceIn(0.15f, 0.85f)
        } else {
            0.5f
        }
        val rawX = chartLeftPx + slotWidth * candleIndex + slotWidth * localT
        if (rawX < allowedLeft || rawX > allowedRight) return@mapNotNull null
        val x = rawX.coerceIn(viewportLeftPx + markerRadiusPx, (viewportRightPx - markerRadiusPx).coerceAtLeast(viewportLeftPx + markerRadiusPx))
        val y = priceToY(marker.unitPrice).coerceIn(minY, maxY)
        PositionedChartTradeMarker(marker = marker, center = Offset(x, y))
    }
}

fun findNearestTradeMarker(
    tapOffset: Offset,
    positionedMarkers: List<PositionedChartTradeMarker>,
    maxDistancePx: Float
): ChartTradeMarkerVO? {
    var bestMarker: ChartTradeMarkerVO? = null
    var bestDistance = maxDistancePx
    positionedMarkers.forEach { positioned ->
        val dist = hypot(
            (tapOffset.x - positioned.center.x).toDouble(),
            (tapOffset.y - positioned.center.y).toDouble()
        ).toFloat()
        if (dist <= bestDistance) {
            bestDistance = dist
            bestMarker = positioned.marker
        }
    }
    return bestMarker
}

fun DrawScope.drawTradeMarkers(
    positionedMarkers: List<PositionedChartTradeMarker>,
    plotBottom: Float,
    pointFillColor: Color,
    isDark: Boolean,
    selectedMarkerId: Long? = null,
    plotTop: Float = 0f
) {
    if (positionedMarkers.isEmpty()) return

    val stemDashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
    val arrowOffsetPx = 7.dp.toPx()

    positionedMarkers.forEach { positioned ->
        val marker = positioned.marker
        val cx = positioned.center.x
        val cy = positioned.center.y
        val isBuy = marker.type == PortfolioTransactionType.BUY
        val markerColor = if (isBuy) buyMarkerColor(isDark) else sellMarkerColor(isDark)
        val isSelected = selectedMarkerId != null && selectedMarkerId == marker.transactionId

        val arrowHalfSize = if (isSelected) 6.6.dp.toPx() else 5.4.dp.toPx()
        val minCenterY = plotTop + arrowHalfSize + 3.dp.toPx()
        val maxCenterY = (plotBottom - arrowHalfSize - 3.dp.toPx()).coerceAtLeast(minCenterY)
        val preferredArrowY = if (isBuy) cy + arrowOffsetPx else cy - arrowOffsetPx
        val arrowCenterY = when {
            isBuy && preferredArrowY > maxCenterY -> (cy - arrowOffsetPx).coerceIn(minCenterY, maxCenterY)
            !isBuy && preferredArrowY < minCenterY -> (cy + arrowOffsetPx).coerceIn(minCenterY, maxCenterY)
            else -> preferredArrowY.coerceIn(minCenterY, maxCenterY)
        }

        // Subtle vertical guide to the time axis starting cleanly below the marker
        val stemStartY = if (arrowCenterY > cy) {
            arrowCenterY + arrowHalfSize * 0.45f
        } else {
            cy
        }
        if (plotBottom > stemStartY) {
            drawLine(
                color = markerColor.copy(alpha = if (isSelected) 0.78f else 0.36f),
                start = Offset(cx, stemStartY),
                end = Offset(cx, plotBottom),
                strokeWidth = if (isSelected) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = stemDashEffect
            )
        }

        val chevronPath = buildBadgeArrowPath(
            cx = cx,
            cy = arrowCenterY,
            size = arrowHalfSize,
            pointsUp = isBuy
        )

        // Soft shaped aura around the chevron so it pops cleanly without being large
        drawPath(
            path = chevronPath,
            color = markerColor.copy(alpha = if (isSelected) 0.34f else 0.22f),
            style = Stroke(
                width = if (isSelected) 6.5.dp.toPx() else 4.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // High-contrast cutout outline separating the chevron from candles/lines
        drawPath(
            path = chevronPath,
            color = pointFillColor,
            style = Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Solid vibrant fill
        drawPath(
            path = chevronPath,
            color = markerColor,
            style = Fill
        )
    }
}

private fun buildBadgeArrowPath(
    cx: Float,
    cy: Float,
    size: Float,
    pointsUp: Boolean
): Path = Path().apply {
    val halfWidth = size * 0.96f
    if (pointsUp) {
        moveTo(cx, cy - size)
        lineTo(cx + halfWidth, cy + size * 0.78f)
        lineTo(cx, cy + size * 0.24f)
        lineTo(cx - halfWidth, cy + size * 0.78f)
        close()
    } else {
        moveTo(cx, cy + size)
        lineTo(cx + halfWidth, cy - size * 0.78f)
        lineTo(cx, cy - size * 0.24f)
        lineTo(cx - halfWidth, cy - size * 0.78f)
        close()
    }
}

@Composable
fun ChartTradeMarkersFooter(
    markers: List<ChartTradeMarkerVO>,
    minChartTimestampMillis: Long,
    maxChartTimestampMillis: Long,
    selectedMarker: ChartTradeMarkerVO?,
    onSelectMarker: (ChartTradeMarkerVO?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (markers.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val buyColor = buyMarkerColor(isDark)
    val sellColor = sellMarkerColor(isDark)

    val visibleMarkers = filterVisibleTradeMarkers(
        markers = markers,
        minTimestampMillis = minChartTimestampMillis,
        maxTimestampMillis = maxChartTimestampMillis
    )
    val buyMarkers = visibleMarkers.filter { it.type == PortfolioTransactionType.BUY }
    val sellMarkers = visibleMarkers.filter { it.type == PortfolioTransactionType.SELL }
    val hiddenCount = (markers.size - visibleMarkers.size).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("chart_trade_markers_footer"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (buyMarkers.isNotEmpty()) {
                    val isBuySelected = selectedMarker?.type == PortfolioTransactionType.BUY
                    TradeMarkerVisualPill(
                        label = "${stringResource(R.string.chart_marker_buy)} (${buyMarkers.size})",
                        isBuy = true,
                        accentColor = buyColor,
                        isSelected = isBuySelected,
                        onClick = {
                            val currentIdx = buyMarkers.indexOfFirst { it.transactionId == selectedMarker?.transactionId }
                            val next = buyMarkers[(currentIdx + 1) % buyMarkers.size]
                            onSelectMarker(if (currentIdx >= 0 && buyMarkers.size == 1) null else next)
                        }
                    )
                }
                if (sellMarkers.isNotEmpty()) {
                    val isSellSelected = selectedMarker?.type == PortfolioTransactionType.SELL
                    TradeMarkerVisualPill(
                        label = "${stringResource(R.string.chart_marker_sell)} (${sellMarkers.size})",
                        isBuy = false,
                        accentColor = sellColor,
                        isSelected = isSellSelected,
                        onClick = {
                            val currentIdx = sellMarkers.indexOfFirst { it.transactionId == selectedMarker?.transactionId }
                            val next = sellMarkers[(currentIdx + 1) % sellMarkers.size]
                            onSelectMarker(if (currentIdx >= 0 && sellMarkers.size == 1) null else next)
                        }
                    )
                }
                if (visibleMarkers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chart_marker_none_in_period, markers.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hiddenCount > 0 && visibleMarkers.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chart_marker_hidden_count, hiddenCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedMarker != null) {
            val isBuy = selectedMarker.type == PortfolioTransactionType.BUY
            val accent = if (isBuy) buyColor else sellColor
            val typeLabel = if (isBuy) {
                stringResource(R.string.chart_marker_buy)
            } else {
                stringResource(R.string.chart_marker_sell)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .clickable { onSelectMarker(null) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("chart_selected_marker_callout"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TradeDirectionArrowIcon(
                    pointsUp = isBuy,
                    color = accent,
                    modifier = Modifier.size(16.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("$typeLabel · ${selectedMarker.quantityFormatted} @ ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = MoneyFontFamily,
                                    fontFeatureSettings = "tnum"
                                )
                            ) {
                                append(selectedMarker.unitPriceFormatted)
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.surface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedMarker.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TradeMarkerVisualPill(
    label: String,
    isBuy: Boolean,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = if (isSelected) 0.24f else 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        TradeDirectionArrowIcon(
            pointsUp = isBuy,
            color = accentColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
internal fun TradeDirectionArrowIcon(
    pointsUp: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val arrowSize = (size.minDimension / 2f) * 0.88f
        val path = buildBadgeArrowPath(
            cx = cx,
            cy = cy,
            size = arrowSize,
            pointsUp = pointsUp
        )
        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}

@Composable
fun CoinPortfolioSummaryCard(
    insight: CoinPortfolioInsightVO,
    onNavigateToPortfolio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToPortfolio)
            .padding(horizontal = 18.dp)
            .testTag("detail_portfolio_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_portfolio_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_portfolio_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onNavigateToPortfolio)
                    .padding(start = 8.dp)
                    .testTag("detail_portfolio_link"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_portfolio_view),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.scrim
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.detail_portfolio_open_cd),
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val isDark = isSystemInDarkTheme()
            val upFlashColor = MaterialTheme.colorScheme.scrim
            val downFlashColor = if (isDark) Color(0xFFFF6B63) else Color(0xFFE04838)
            val defaultPnLColor = insight.totalPnLTrend.textColor
            var pnlFlashColor by remember(insight.coinId) {
                mutableStateOf<Color?>(null)
            }
            var lastObservedPnL by remember(insight.coinId) {
                mutableStateOf<Double?>(null)
            }

            LaunchedEffect(insight.rawTotalPnL) {
                val current = insight.rawTotalPnL
                val previous = lastObservedPnL
                if (current.isFinite()) {
                    if (previous != null && current != previous) {
                        pnlFlashColor = if (current > previous) upFlashColor else downFlashColor
                        lastObservedPnL = current
                        delay(750L)
                        pnlFlashColor = null
                    } else {
                        lastObservedPnL = current
                    }
                }
            }

            val animatedPnLColor by animateColorAsState(
                targetValue = pnlFlashColor ?: defaultPnLColor,
                animationSpec = tween(durationMillis = if (pnlFlashColor != null) 140 else 550),
                label = "portfolioTotalPnLFlashColor"
            )
            val totalPnLFontSize = if (insight.totalPnLFormatted.length >= 14) 20.sp else 22.sp

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_portfolio_total_pnl),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OdometerText(
                        text = insight.totalPnLFormatted,
                        rawValue = insight.rawTotalPnL,
                        animateChanges = true,
                        style = MaterialTheme.typography.titleLarge.asMoneyStyle(),
                        fontSize = totalPnLFontSize,
                        fontWeight = FontWeight.Bold,
                        color = animatedPnLColor
                    )
                    OdometerText(
                        text = "(${insight.totalPnLPercentageFormatted})",
                        rawValue = insight.rawTotalPnLPercentage,
                        animateChanges = true,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = insight.totalPnLTrend.textColor
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (insight.hasOpenPosition) {
                        stringResource(R.string.detail_portfolio_current_value)
                    } else {
                        stringResource(R.string.detail_portfolio_status)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (insight.hasOpenPosition) {
                        insight.currentValueFormatted
                    } else {
                        stringResource(R.string.detail_portfolio_closed_position)
                    },
                    style = if (insight.hasOpenPosition) {
                        MaterialTheme.typography.titleMedium.asMoneyStyle()
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (insight.hasOpenPosition) {
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_quantity),
                    value = insight.quantityFormatted,
                    showDivider = false,
                    verticalPadding = 0.dp
                )
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_avg_price),
                    value = insight.averageBuyPriceFormatted,
                    showDivider = false,
                    verticalPadding = 0.dp,
                    isMonetary = true
                )
                if (insight.hasRealizedPnL) {
                    SectionInfoItem(
                        name = stringResource(R.string.detail_portfolio_realized_pnl_full),
                        value = insight.realizedPnLFormatted,
                        showDivider = false,
                        verticalPadding = 0.dp,
                        isMonetary = true
                    )
                }
            } else {
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_realized_pnl_short),
                    value = insight.realizedPnLFormatted,
                    showDivider = false,
                    verticalPadding = 0.dp,
                    isMonetary = true
                )
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_recorded_trades),
                    value = "${insight.tradeMarkers.size}",
                    showDivider = false,
                    verticalPadding = 0.dp
                )
            }
        }
    }
}

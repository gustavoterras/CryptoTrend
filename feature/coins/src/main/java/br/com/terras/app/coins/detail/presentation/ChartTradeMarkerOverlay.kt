package br.com.terras.app.coins.detail.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.ChartTradeMarkerVO
import br.com.terras.app.coins.detail.domain.CoinPortfolioInsightVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.network.portfolio.PortfolioTransactionType
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
    selectedMarkerId: Long? = null
) {
    if (positionedMarkers.isEmpty()) return

    val stemDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    val radius = 5.5.dp.toPx()
    val haloRadius = 7.dp.toPx()

    positionedMarkers.forEach { positioned ->
        val marker = positioned.marker
        val cx = positioned.center.x
        val cy = positioned.center.y
        val isBuy = marker.type == PortfolioTransactionType.BUY
        val markerColor = if (isBuy) buyMarkerColor(isDark) else sellMarkerColor(isDark)
        val isSelected = selectedMarkerId != null && selectedMarkerId == marker.transactionId

        if (plotBottom > cy + radius) {
            drawLine(
                color = markerColor.copy(alpha = if (isSelected) 0.75f else 0.42f),
                start = Offset(cx, cy),
                end = Offset(cx, plotBottom),
                strokeWidth = if (isSelected) 1.5.dp.toPx() else 1.dp.toPx(),
                pathEffect = stemDashEffect
            )
        }

        if (isSelected) {
            drawCircle(
                color = markerColor.copy(alpha = 0.24f),
                radius = radius * 2.1f,
                center = Offset(cx, cy),
                style = Fill
            )
        }

        val haloTriangle = buildMarkerTrianglePath(cx, cy, haloRadius, isBuy)
        drawPath(
            path = haloTriangle,
            color = pointFillColor,
            style = Fill
        )

        val trianglePath = buildMarkerTrianglePath(cx, cy, radius, isBuy)
        drawPath(
            path = trianglePath,
            color = markerColor,
            style = Fill
        )
        drawPath(
            path = trianglePath,
            color = pointFillColor.copy(alpha = 0.9f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun buildMarkerTrianglePath(
    cx: Float,
    cy: Float,
    radius: Float,
    pointsUp: Boolean
): Path = Path().apply {
    val halfWidth = radius * 0.95f
    if (pointsUp) {
        moveTo(cx, cy - radius)
        lineTo(cx + halfWidth, cy + radius * 0.75f)
        lineTo(cx - halfWidth, cy + radius * 0.75f)
        close()
    } else {
        moveTo(cx, cy + radius)
        lineTo(cx + halfWidth, cy - radius * 0.75f)
        lineTo(cx - halfWidth, cy - radius * 0.75f)
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
    val buyCount = visibleMarkers.count { it.type == PortfolioTransactionType.BUY }
    val sellCount = visibleMarkers.count { it.type == PortfolioTransactionType.SELL }
    val hiddenCount = (markers.size - visibleMarkers.size).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("chart_trade_markers_footer"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (buyCount > 0) {
                    Text(
                        text = stringResource(R.string.chart_marker_buy_count, buyCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = buyColor
                    )
                }
                if (sellCount > 0) {
                    Text(
                        text = stringResource(R.string.chart_marker_sell_count, sellCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sellColor
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectMarker(null) }
                    .padding(top = 2.dp)
                    .testTag("chart_selected_marker_callout"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = accent.copy(alpha = 0.45f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isBuy) "▲" else "▼"} $typeLabel • ${selectedMarker.quantityFormatted} @ ${selectedMarker.unitPriceFormatted}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.surface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedMarker.dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.detail_portfolio_total_pnl),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = insight.totalPnLFormatted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = insight.totalPnLTrend.textColor
                    )
                    Text(
                        text = "(${insight.totalPnLPercentageFormatted})",
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (insight.hasOpenPosition) {
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_quantity),
                    value = insight.quantityFormatted,
                    showDivider = true
                )
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_avg_price),
                    value = insight.averageBuyPriceFormatted,
                    showDivider = insight.hasRealizedPnL
                )
                if (insight.hasRealizedPnL) {
                    SectionInfoItem(
                        name = stringResource(R.string.detail_portfolio_realized_pnl_full),
                        value = insight.realizedPnLFormatted,
                        showDivider = false
                    )
                }
            } else {
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_realized_pnl_short),
                    value = insight.realizedPnLFormatted,
                    showDivider = true
                )
                SectionInfoItem(
                    name = stringResource(R.string.detail_portfolio_recorded_trades),
                    value = "${insight.tradeMarkers.size}",
                    showDivider = false
                )
            }
        }
    }
}

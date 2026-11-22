package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

data class AllocationSlice(
    val id: String,
    val symbol: String,
    val label: String,
    val valueText: String,
    val percentage: Float,
    val percentageText: String,
    val color: Color
)

@Composable
@ReadOnlyComposable
fun portfolioChartPalette(): List<Color> {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        listOf(
            Color(0xFF1ED788),
            Color(0xFFFBBF24),
            Color(0xFF38BDF8),
            Color(0xFFA78BFA),
            Color(0xFFFB7185),
            Color(0xFF94A3B8)
        )
    } else {
        listOf(
            Color(0xFF0D8756),
            Color(0xFFF59E0B),
            Color(0xFF0284C7),
            Color(0xFF7C3AED),
            Color(0xFFE11D48),
            Color(0xFF64748B)
        )
    }
}

@Composable
fun AllocationDonutChart(
    slices: List<AllocationSlice>,
    totalValueText: String,
    assetCountLabel: String,
    selectedSliceId: String?,
    onSelectSlice: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) return

    val sweepProgress = remember(slices.map { "${it.id}_${it.percentage}" }) { Animatable(0f) }
    LaunchedEffect(slices) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val selectedSlice = slices.firstOrNull { it.id == selectedSliceId }
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    val sliceAngles = remember(slices) {
        val totalPct = slices.sumOf { it.percentage.toDouble() }.coerceAtLeast(0.0001)
        var cursor = 0f
        slices.map { slice ->
            val sweep = ((slice.percentage / totalPct.toFloat()) * 360f).coerceAtLeast(0f)
            val start = cursor
            cursor += sweep
            Triple(slice, start, sweep)
        }
    }

    val baseStrokeDp = 24.dp
    val selectedStrokeDp = 30.dp
    val animatedStrokeWidths = sliceAngles.map { (slice, _, _) ->
        val width by animateDpAsState(
            targetValue = if (slice.id == selectedSliceId) selectedStrokeDp else baseStrokeDp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "donutSliceStroke_${slice.id}"
        )
        width
    }
    val animatedAlphas = sliceAngles.map { (slice, _, _) ->
        val alpha by animateFloatAsState(
            targetValue = if (selectedSliceId == null || slice.id == selectedSliceId) 1f else 0.42f,
            animationSpec = tween(durationMillis = 200),
            label = "donutSliceAlpha_${slice.id}"
        )
        alpha
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_allocation_chart"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(216.dp)
                .pointerInput(sliceAngles, selectedSliceId) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val radius = minOf(centerX, centerY)
                        val distance = hypot(dx, dy)
                        if (distance < radius * 0.42f) {
                            onSelectSlice(null)
                            return@detectTapGestures
                        }
                        if (distance <= radius * 1.05f) {
                            val rawDeg = atan2(dy.toDouble(), dx.toDouble()) * (180.0 / PI)
                            val fromTopDeg = ((rawDeg + 90.0 + 360.0) % 360.0).toFloat()
                            val hit = sliceAngles.firstOrNull { (_, start, sweep) ->
                                fromTopDeg >= start && fromTopDeg <= start + sweep
                            }?.first
                            if (hit != null) {
                                onSelectSlice(if (selectedSliceId == hit.id) null else hit.id)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(196.dp)) {
                val maxStrokePx = selectedStrokeDp.toPx()
                val arcDiameter = size.minDimension - maxStrokePx
                val topLeft = Offset(
                    x = (size.width - arcDiameter) / 2f,
                    y = (size.height - arcDiameter) / 2f
                )
                val arcSize = Size(arcDiameter, arcDiameter)

                drawArc(
                    color = trackColor.copy(alpha = 0.45f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = baseStrokeDp.toPx())
                )

                val progress = sweepProgress.value
                val gapDegrees = if (sliceAngles.size > 1) 2.5f else 0f

                sliceAngles.forEachIndexed { index, (slice, startDeg, rawSweepDeg) ->
                    val animatedSweep = rawSweepDeg * progress
                    val drawnSweep = (animatedSweep - gapDegrees).coerceAtLeast(
                        if (animatedSweep > 0.5f) animatedSweep else 0f
                    )
                    if (drawnSweep > 0f) {
                        val strokeWidthPx = animatedStrokeWidths[index].toPx()
                        drawArc(
                            color = slice.color.copy(alpha = animatedAlphas[index]),
                            startAngle = -90f + startDeg + gapDegrees / 2f,
                            sweepAngle = drawnSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Butt
                            )
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedSlice,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.92f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.92f))
                },
                label = "donutCenterReadout"
            ) { slice ->
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (slice != null) {
                        Text(
                            text = slice.symbol,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = slice.color
                        )
                        Text(
                            text = slice.percentageText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            text = slice.valueText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Alocação",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalValueText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = assetCountLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            slices.forEachIndexed { index, slice ->
                val isSelected = selectedSliceId == slice.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(role = Role.Button) {
                            onSelectSlice(if (isSelected) null else slice.id)
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                        .testTag("allocation_legend_${slice.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = slice.symbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = slice.valueText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = slice.percentageText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }
                if (index != slices.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AllocationDonutChartPreview() {
    DSMTheme {
        val palette = portfolioChartPalette()
        var selectedId by remember { mutableStateOf<String?>("bitcoin") }
        Surface(color = MaterialTheme.colorScheme.background) {
            AllocationDonutChart(
                slices = listOf(
                    AllocationSlice("bitcoin", "BTC", "Bitcoin", "R$ 32.000,00", 64f, "64,0%", palette[0]),
                    AllocationSlice("ethereum", "ETH", "Ethereum", "R$ 12.000,00", 24f, "24,0%", palette[1]),
                    AllocationSlice("solana", "SOL", "Solana", "R$ 6.000,00", 12f, "12,0%", palette[2])
                ),
                totalValueText = "R$ 50.000,00",
                assetCountLabel = "3 ativos",
                selectedSliceId = selectedId,
                onSelectSlice = { selectedId = it },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

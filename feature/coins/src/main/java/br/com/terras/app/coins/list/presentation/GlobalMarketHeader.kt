package br.com.terras.app.coins.list.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
import br.com.terras.app.coins.list.presentation.CoinsViewModel.GlobalMarketState
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Stable
class GlobalMarketCollapseState(
    private val scope: CoroutineScope
) {
    var maxHeightPx by mutableFloatStateOf(0f)
        internal set

    var collapseOffsetPx by mutableFloatStateOf(0f)
        internal set

    private var snapJob: Job? = null

    val expandedFraction: Float
        get() = if (maxHeightPx > 0f) {
            (1f + collapseOffsetPx / maxHeightPx).coerceIn(0f, 1f)
        } else {
            1f
        }

    fun updateMeasuredHeight(heightPx: Float) {
        if (heightPx != maxHeightPx) {
            maxHeightPx = heightPx
        }
        if (maxHeightPx <= 0f) {
            collapseOffsetPx = 0f
        } else if (collapseOffsetPx < -maxHeightPx) {
            collapseOffsetPx = -maxHeightPx
        }
    }

    fun expandSmoothly() {
        if (maxHeightPx <= 0f || collapseOffsetPx >= 0f) return
        animateCollapseTo(0f)
    }

    internal fun consumePreScroll(deltaY: Float, currentMaxHeight: Float): Float {
        if (currentMaxHeight <= 0f || deltaY == 0f) return 0f
        snapJob?.cancel()
        val oldOffset = collapseOffsetPx.coerceIn(-currentMaxHeight, 0f)
        val newOffset = (oldOffset + deltaY).coerceIn(-currentMaxHeight, 0f)
        collapseOffsetPx = newOffset
        return newOffset - oldOffset
    }

    internal fun snapAfterFling(velocityY: Float, currentMaxHeight: Float) {
        if (currentMaxHeight <= 0f) return
        if (collapseOffsetPx == 0f || collapseOffsetPx == -currentMaxHeight) return
        val target = when {
            velocityY > 250f -> 0f
            velocityY < -250f -> -currentMaxHeight
            collapseOffsetPx < -currentMaxHeight * 0.5f -> -currentMaxHeight
            else -> 0f
        }
        animateCollapseTo(target)
    }

    private fun animateCollapseTo(target: Float) {
        snapJob?.cancel()
        snapJob = scope.launch {
            val anim = Animatable(collapseOffsetPx)
            anim.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                collapseOffsetPx = value
            }
        }
    }
}

@Composable
fun rememberGlobalMarketCollapseState(): GlobalMarketCollapseState {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlobalMarketCollapseState(scope) }
}

@Composable
fun rememberGlobalMarketNestedScrollConnection(
    collapseState: GlobalMarketCollapseState
): NestedScrollConnection {
    val currentState by rememberUpdatedState(collapseState)
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val consumedY = currentState.consumePreScroll(
                    deltaY = available.y,
                    currentMaxHeight = currentState.maxHeightPx
                )
                return if (consumedY != 0f) Offset(0f, consumedY) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                currentState.snapAfterFling(
                    velocityY = available.y,
                    currentMaxHeight = currentState.maxHeightPx
                )
                return Velocity.Zero
            }
        }
    }
}

@Composable
fun CollapsingGlobalMarketHeader(
    state: GlobalMarketState,
    collapseState: GlobalMarketCollapseState,
    modifier: Modifier = Modifier
) {
    if (state is GlobalMarketState.Empty) {
        collapseState.updateMeasuredHeight(0f)
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val fullHeight = placeable.height.toFloat()
                collapseState.updateMeasuredHeight(fullHeight)
                val visibleHeight = (fullHeight + collapseState.collapseOffsetPx)
                    .roundToInt()
                    .coerceIn(0, placeable.height)
                layout(placeable.width, visibleHeight) {
                    placeable.placeRelative(0, visibleHeight - placeable.height)
                }
            }
            .graphicsLayer {
                alpha = collapseState.expandedFraction
            },
        contentAlignment = Alignment.TopCenter
    ) {
        val contentModifier = Modifier
            .widthIn(max = 880.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)

        when (state) {
            is GlobalMarketState.Loading -> GlobalMarketSideBySideShimmer(modifier = contentModifier)
            is GlobalMarketState.Success -> GlobalMarketSideBySideHeader(
                data = state.data,
                isScrollingCollapsed = collapseState.collapseOffsetPx < -8f,
                modifier = contentModifier
            )
            is GlobalMarketState.Empty -> Unit
        }
    }
}

@Composable
private fun GlobalMarketSideBySideHeader(
    data: GlobalMarketVO,
    isScrollingCollapsed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    }
    val isEnglish = !currentLocale.language.equals("pt", ignoreCase = true)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    var activeSection by remember { mutableStateOf<GlobalHeaderSection?>(null) }
    var headerSize by remember { mutableStateOf(IntSize.Zero) }
    val sectionCenters = remember { mutableStateMapOf<GlobalHeaderSection, Float>() }

    LaunchedEffect(isScrollingCollapsed) {
        if (isScrollingCollapsed && activeSection != null) {
            activeSection = null
        }
    }

    val toggleSection: (GlobalHeaderSection) -> Unit = { section ->
        activeSection = if (activeSection == section) null else section
    }

    Box(
        modifier = modifier.onSizeChanged { headerSize = it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .testTag("global_market_header"),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FearAndGreedSection(
                data = data,
                isEnglish = isEnglish,
                isSelected = activeSection == GlobalHeaderSection.FEAR_GREED,
                onClick = { toggleSection(GlobalHeaderSection.FEAR_GREED) },
                modifier = Modifier
                    .weight(0.94f)
                    .onGloballyPositioned { coords ->
                        sectionCenters[GlobalHeaderSection.FEAR_GREED] =
                            coords.positionInParent().x + coords.size.width / 2f
                    }
            )
            SectionVerticalDivider(dividerColor)
            AltcoinSeasonSection(
                data = data,
                isSelected = activeSection == GlobalHeaderSection.ALTCOIN_SEASON,
                onClick = { toggleSection(GlobalHeaderSection.ALTCOIN_SEASON) },
                modifier = Modifier
                    .weight(0.94f)
                    .onGloballyPositioned { coords ->
                        sectionCenters[GlobalHeaderSection.ALTCOIN_SEASON] =
                            coords.positionInParent().x + coords.size.width / 2f
                    }
            )
            SectionVerticalDivider(dividerColor)
            MarketCapSection(
                data = data,
                isEnglish = isEnglish,
                isSelected = activeSection == GlobalHeaderSection.MARKET_CAP,
                onClick = { toggleSection(GlobalHeaderSection.MARKET_CAP) },
                modifier = Modifier
                    .weight(1.22f)
                    .onGloballyPositioned { coords ->
                        sectionCenters[GlobalHeaderSection.MARKET_CAP] =
                            coords.positionInParent().x + coords.size.width / 2f
                    }
            )
            SectionVerticalDivider(dividerColor)
            VolumeSection(
                data = data,
                isEnglish = isEnglish,
                isSelected = activeSection == GlobalHeaderSection.VOLUME,
                onClick = { toggleSection(GlobalHeaderSection.VOLUME) },
                modifier = Modifier
                    .weight(0.90f)
                    .onGloballyPositioned { coords ->
                        sectionCenters[GlobalHeaderSection.VOLUME] =
                            coords.positionInParent().x + coords.size.width / 2f
                    }
            )
        }

        val currentSection = activeSection
        if (currentSection != null && headerSize.width > 0 && headerSize.height > 0) {
            val anchorX = sectionCenters[currentSection] ?: (headerSize.width / 2f)
            GlobalMarketInfoBalloon(
                section = currentSection,
                data = data,
                isEnglish = isEnglish,
                headerWidthPx = headerSize.width,
                headerHeightPx = headerSize.height,
                anchorCenterXPx = anchorX,
                onDismiss = { activeSection = null }
            )
        }
    }
}

@Composable
private fun HeaderMetricColumn(
    title: String,
    testTag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    centerContent: @Composable BoxScope.() -> Unit
) {
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = if (isSelected) {
        MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(highlightColor)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                letterSpacing = (-0.2).sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.surface else subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center,
            content = centerContent
        )
    }
}

@Composable
private fun SectionVerticalDivider(color: Color) {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        thickness = 1.dp,
        color = color
    )
}

@Composable
private fun FearAndGreedSection(
    data: GlobalMarketVO,
    isEnglish: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val sentimentColor = sentimentColor(data.fearAndGreedSentiment, isDark)

    HeaderMetricColumn(
        title = if (isEnglish) "Fear & Greed" else "Medo & Ganância",
        testTag = "fear_greed_section",
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    ) {
        FearAndGreedGauge(
            value = data.fearAndGreedValue,
            sentiment = data.fearAndGreedSentiment,
            hasValue = data.hasFearAndGreed,
            activeColor = sentimentColor,
            modifier = Modifier
                .widthIn(max = 68.dp)
                .fillMaxWidth()
                .testTag("fear_greed_gauge")
        )
    }
}

@Composable
private fun AltcoinSeasonSection(
    data: GlobalMarketVO,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val btcColor = if (isDark) Color(0xFFFFB020) else Color(0xFFE68400)
    val altsColor = MaterialTheme.colorScheme.scrim
    val primaryTextColor = MaterialTheme.colorScheme.surface
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    val targetRatio = if (data.hasBtcDominance) {
        (data.btcDominancePercent.toFloat() / 100f).coerceIn(0.05f, 0.95f)
    } else {
        0.5f
    }
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "altcoinSeasonBtcRatio"
    )

    HeaderMetricColumn(
        title = "Altcoin Season",
        testTag = "altcoin_season_section",
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (data.hasBtcDominance) "${data.btcDominanceScore}/100" else "--",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryTextColor,
                maxLines = 1,
                modifier = Modifier.testTag("altcoin_season_score")
            )

            Canvas(
                modifier = Modifier
                    .widthIn(max = 64.dp)
                    .fillMaxWidth()
                    .height(12.dp)
                    .testTag("altcoin_season_illustration")
            ) {
                val barHeight = 6.5.dp.toPx()
                val barTop = (size.height - barHeight) / 2f
                val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

                if (!data.hasBtcDominance) {
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(0f, barTop),
                        size = Size(size.width, barHeight),
                        cornerRadius = corner
                    )
                } else {
                    val gapPx = 2.5.dp.toPx()
                    val usableWidth = (size.width - gapPx).coerceAtLeast(0f)
                    val btcWidth = (usableWidth * animatedRatio).coerceAtLeast(barHeight)
                    val altsLeft = btcWidth + gapPx
                    val altsWidth = (size.width - altsLeft).coerceAtLeast(0f)

                    drawRoundRect(
                        color = btcColor,
                        topLeft = Offset(0f, barTop),
                        size = Size(btcWidth, barHeight),
                        cornerRadius = corner
                    )

                    if (altsWidth > 0f) {
                        drawRoundRect(
                            color = altsColor,
                            topLeft = Offset(altsLeft, barTop),
                            size = Size(altsWidth, barHeight),
                            cornerRadius = corner
                        )
                    }

                    val splitX = btcWidth + gapPx / 2f
                    drawCircle(
                        color = primaryTextColor,
                        radius = 4.2.dp.toPx(),
                        center = Offset(splitX, size.height / 2f)
                    )
                    drawCircle(
                        color = btcColor,
                        radius = 2.2.dp.toPx(),
                        center = Offset(splitX, size.height / 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketCapSection(
    data: GlobalMarketVO,
    isEnglish: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTextColor = MaterialTheme.colorScheme.surface

    HeaderMetricColumn(
        title = if (isEnglish) "Market Cap" else "Cap. Mercado",
        testTag = "global_market_cap_section",
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            Text(
                text = data.totalMarketCap,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 11.5.sp,
                letterSpacing = (-0.3).sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("global_market_cap_value")
            )

            Text(
                text = data.marketCapChangePercentage,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = (-0.2).sp,
                fontWeight = FontWeight.SemiBold,
                color = data.trendColor.textColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.testTag("global_market_cap_change")
            )
        }
    }
}

@Composable
private fun VolumeSection(
    data: GlobalMarketVO,
    isEnglish: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTextColor = MaterialTheme.colorScheme.surface

    HeaderMetricColumn(
        title = if (isEnglish) "24h Volume" else "Volume 24h",
        testTag = "global_market_volume_section",
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = data.totalVolume24h,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 11.5.sp,
            letterSpacing = (-0.3).sp,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("global_market_volume_value")
        )
    }
}

@Composable
private fun FearAndGreedGauge(
    value: Int,
    sentiment: FearAndGreedSentiment,
    hasValue: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background
    val scoreColor = MaterialTheme.colorScheme.surface

    val targetFraction = if (hasValue) (value.coerceIn(0, 100) / 100f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "fearGreedGaugeSweep"
    )

    val segmentOrder = remember {
        listOf(
            FearAndGreedSentiment.EXTREME_FEAR,
            FearAndGreedSentiment.FEAR,
            FearAndGreedSentiment.NEUTRAL,
            FearAndGreedSentiment.GREED,
            FearAndGreedSentiment.EXTREME_GREED
        )
    }

    Box(
        modifier = modifier.height(38.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            val strokePx = 5.5.dp.toPx()
            val horizontalPad = strokePx / 2f + 2.5.dp.toPx()
            val arcDiameter = (size.width - horizontalPad * 2f).coerceAtMost((size.height - strokePx) * 2f)
            val arcLeft = (size.width - arcDiameter) / 2f
            val arcTop = size.height - arcDiameter / 2f - strokePx / 2f - 1.dp.toPx()
            val arcSize = Size(arcDiameter, arcDiameter)

            val gapDegrees = 6f
            val totalGap = gapDegrees * (segmentOrder.size - 1)
            val segmentSweep = (180f - totalGap) / segmentOrder.size

            segmentOrder.forEachIndexed { index, segSentiment ->
                val segColor = sentimentColor(segSentiment, isDark)
                val isActive = hasValue && segSentiment == sentiment
                val alpha = when {
                    !hasValue -> 0.25f
                    isActive -> 1f
                    else -> 0.30f
                }
                val startAngle = 180f + index * (segmentSweep + gapDegrees)
                drawArc(
                    color = segColor.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = Offset(arcLeft, arcTop),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (hasValue) {
                val radius = arcDiameter / 2f
                val centerX = arcLeft + radius
                val centerY = arcTop + radius
                val angleRad = ((180f + animatedFraction * 180f) * PI / 180.0)
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val dotCenter = Offset(
                    x = centerX + radius * cosA,
                    y = centerY + radius * sinA
                )
                val pointerStart = Offset(
                    x = centerX + (radius * 0.54f) * cosA,
                    y = centerY + (radius * 0.54f) * sinA
                )
                val pointerEnd = Offset(
                    x = centerX + (radius - strokePx * 0.65f) * cosA,
                    y = centerY + (radius - strokePx * 0.65f) * sinA
                )

                drawLine(
                    color = activeColor,
                    start = pointerStart,
                    end = pointerEnd,
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = backgroundColor,
                    radius = strokePx * 0.72f,
                    center = dotCenter
                )
                drawCircle(
                    color = activeColor,
                    radius = strokePx * 0.48f,
                    center = dotCenter
                )
            }
        }

        Text(
            text = if (hasValue) value.toString() else "--",
            style = MaterialTheme.typography.titleSmall,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = scoreColor,
            modifier = Modifier
                .padding(bottom = 1.dp)
                .testTag("fear_greed_score_value")
        )
    }
}

internal fun sentimentColor(
    sentiment: FearAndGreedSentiment,
    isDark: Boolean
): Color = when (sentiment) {
    FearAndGreedSentiment.EXTREME_FEAR -> if (isDark) Color(0xFFFF6B63) else Color(0xFFD93829)
    FearAndGreedSentiment.FEAR -> if (isDark) Color(0xFFFF9F43) else Color(0xFFE66A00)
    FearAndGreedSentiment.NEUTRAL -> if (isDark) Color(0xFFFACC15) else Color(0xFFB48200)
    FearAndGreedSentiment.GREED -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    FearAndGreedSentiment.EXTREME_GREED -> if (isDark) Color(0xFF34E89E) else Color(0xFF0D8756)
}

@Composable
private fun GlobalMarketSideBySideShimmer(
    modifier: Modifier = Modifier
) {
    val brush = ShimmerEffect()
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .testTag("global_market_shimmer"),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section 1: Medo & Ganância (Arco + pontuação)
        ShimmerHeaderSectionColumn(
            titleWidth = 58.dp,
            brush = brush,
            testTag = "fear_greed_shimmer",
            modifier = Modifier.weight(0.94f)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 68.dp)
                    .fillMaxWidth()
                    .height(38.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    val strokePx = 5.5.dp.toPx()
                    val horizontalPad = strokePx / 2f + 2.5.dp.toPx()
                    val arcDiameter = (size.width - horizontalPad * 2f)
                        .coerceAtMost((size.height - strokePx) * 2f)
                    val arcLeft = (size.width - arcDiameter) / 2f
                    val arcTop = size.height - arcDiameter / 2f - strokePx / 2f - 1.dp.toPx()
                    drawArc(
                        brush = brush,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(arcLeft, arcTop),
                        size = Size(arcDiameter, arcDiameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .width(24.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        SectionVerticalDivider(dividerColor)

        // Section 2: Altcoin Season (Score + barra dividida)
        ShimmerHeaderSectionColumn(
            titleWidth = 54.dp,
            brush = brush,
            testTag = "altcoin_season_shimmer",
            modifier = Modifier.weight(0.94f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .widthIn(max = 64.dp)
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        SectionVerticalDivider(dividerColor)

        // Section 3: Cap. Mercado (Valor + percentual à direita)
        ShimmerHeaderSectionColumn(
            titleWidth = 54.dp,
            brush = brush,
            testTag = "market_cap_shimmer",
            modifier = Modifier.weight(1.22f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        SectionVerticalDivider(dividerColor)

        // Section 4: Volume 24h
        ShimmerHeaderSectionColumn(
            titleWidth = 50.dp,
            brush = brush,
            testTag = "volume_shimmer",
            modifier = Modifier.weight(0.90f)
        ) {
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun ShimmerHeaderSectionColumn(
    titleWidth: androidx.compose.ui.unit.Dp,
    brush: Brush,
    testTag: String,
    modifier: Modifier = Modifier,
    centerContent: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(titleWidth)
                    .height(9.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center,
            content = centerContent
        )
    }
}

@ThemePreviews
@Composable
private fun GlobalMarketSideBySideShimmerPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GlobalMarketSideBySideShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
private fun GlobalMarketSideBySideHeaderPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            GlobalMarketSideBySideHeader(
                data = GlobalMarketVO(
                    fearAndGreedValue = 22,
                    fearAndGreedSentiment = FearAndGreedSentiment.EXTREME_FEAR,
                    fearAndGreedLabel = "Medo Extremo",
                    hasFearAndGreed = true,
                    totalMarketCap = "R$ 16,42 tri",
                    marketCapChangePercentage = "+2,4%",
                    trendColor = TrendColor.UP,
                    totalVolume24h = "R$ 612,80 bi",
                    btcDominance = "58,0%",
                    ethDominance = "16,5%",
                    activeCryptocurrencies = "15.420",
                    marketsCount = "1.120",
                    btcDominancePercent = 58.0,
                    btcDominanceScore = 58,
                    altcoinsDominanceScore = 42,
                    hasBtcDominance = true
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

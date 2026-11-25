package br.com.terras.app.cryptotrend.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.cryptotrend.core.dsm.R as DsmR
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val SpaceGroteskDisplayFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Bold)
)

private val SmoothDecelerateEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    startEntrance: Boolean = true
) {
    // Matches exact icon asset background (#0C7D52) for a zero-seam OS splash handoff
    val iconBrandGreen = Color(0xFF0C7D52)
    val deepEdgeEmerald = Color(0xFF074B32)
    val emeraldRingAccent = Color(0xFF55E99F)
    val brightMintWordAccent = Color(0xFF7AF7B9)
    val goldAccent = Color(0xFFF4A612)
    val primaryTextColor = Color.White
    val mutedTextColor = Color.White.copy(alpha = 0.86f)

    var startLift by remember { mutableStateOf(false) }
    var startCryptoWord by remember { mutableStateOf(false) }
    var startTrendWord by remember { mutableStateOf(false) }
    var startUnderline by remember { mutableStateOf(false) }
    var startTagline by remember { mutableStateOf(false) }

    val ambientDepthAlpha = remember { Animatable(0f) }
    val haloPulseProgress = remember { Animatable(0f) }

    LaunchedEffect(startEntrance) {
        if (!startEntrance) return@LaunchedEffect
        ambientDepthAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(startEntrance) {
        if (!startEntrance) return@LaunchedEffect
        delay(80L)
        startLift = true
        haloPulseProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = SmoothDecelerateEasing)
        )
    }

    LaunchedEffect(startEntrance) {
        if (!startEntrance) return@LaunchedEffect
        delay(220L)
        startCryptoWord = true
        delay(180L)
        startTrendWord = true
        delay(300L)
        startUnderline = true
        delay(160L)
        startTagline = true
        delay(1000L)
        onSplashFinished()
    }

    val liftProgress by animateFloatAsState(
        targetValue = if (startLift) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = SmoothDecelerateEasing),
        label = "splashLiftProgress"
    )

    val underlineProgress by animateFloatAsState(
        targetValue = if (startUnderline) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splashUnderlineProgress"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = if (startTagline) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splashTaglineAlpha"
    )

    val taglineOffsetY by animateFloatAsState(
        targetValue = if (startTagline) 0f else 16f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "splashTaglineOffsetY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iconBrandGreen)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("splash_screen")
    ) {
        // Soft peripheral depth vignette keeping the center 100% matched to #0C7D52
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(ambientDepthAlpha.value)
                .background(
                    Brush.radialGradient(
                        0.0f to iconBrandGreen,
                        0.45f to iconBrandGreen,
                        1.0f to deepEdgeEmerald.copy(alpha = 0.85f)
                    )
                )
        )

        // Custom synchronized layout: 112.dp logo starts dead-center matching OS splash 1:1 and glides smoothly upward
        Layout(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            content = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(144.dp)
                ) {
                    // Subtle expanding orbital pulse matching the emblem's mint ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pulse = haloPulseProgress.value
                        if (pulse > 0.01f && pulse < 0.99f) {
                            val ringAlpha = (1f - pulse) * 0.35f
                            val ringRadius = (size.minDimension * 0.27f) * (1f + 0.42f * pulse)
                            drawCircle(
                                color = emeraldRingAccent.copy(alpha = ringAlpha),
                                radius = ringRadius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Image(
                        painter = painterResource(id = R.drawable.crypto_trend_icon_1795270968907),
                        contentDescription = stringResource(DsmR.string.splash_logo_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .testTag("splash_logo_image")
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.testTag("splash_animated_title")
                    ) {
                        AnimatedBrandWord(
                            word = "Crypto",
                            visible = startCryptoWord,
                            color = primaryTextColor,
                            staggerMillis = 38L
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        AnimatedBrandWord(
                            word = "Trend",
                            visible = startTrendWord,
                            color = brightMintWordAccent,
                            staggerMillis = 44L
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Canvas(
                        modifier = Modifier
                            .width(172.dp)
                            .height(8.dp)
                    ) {
                        if (underlineProgress > 0.01f) {
                            val endX = size.width * underlineProgress
                            val centerY = size.height / 2f
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        brightMintWordAccent.copy(alpha = 0.25f),
                                        emeraldRingAccent,
                                        goldAccent
                                    )
                                ),
                                start = Offset(0f, centerY),
                                end = Offset(endX, centerY),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = goldAccent,
                                radius = 3.5.dp.toPx(),
                                center = Offset(endX, centerY)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(DsmR.string.splash_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = mutedTextColor,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = taglineAlpha
                                translationY = taglineOffsetY
                            }
                            .testTag("splash_subtitle")
                    )
                }
            }
        ) { measurables, constraints ->
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val logoBoxPlaceable = measurables[0].measure(looseConstraints)
            val textPlaceable = measurables[1].measure(looseConstraints)

            val logoVisualHeight = 112.dp.roundToPx()
            val insetPx = (logoBoxPlaceable.height - logoVisualHeight) / 2
            val gapPx = 18.dp.roundToPx()
            val groupHeight = logoVisualHeight + gapPx + textPlaceable.height

            val y0 = (constraints.maxHeight - logoBoxPlaceable.height) / 2
            val groupTopY = ((constraints.maxHeight - groupHeight) / 2)
                .coerceAtLeast(16.dp.roundToPx())
            val y1 = groupTopY - insetPx

            val currentLogoY = (y0 + (y1 - y0) * liftProgress).roundToInt()
            val textY = groupTopY + logoVisualHeight + gapPx

            layout(constraints.maxWidth, constraints.maxHeight) {
                val logoX = (constraints.maxWidth - logoBoxPlaceable.width) / 2
                val textX = (constraints.maxWidth - textPlaceable.width) / 2
                logoBoxPlaceable.placeRelative(logoX, currentLogoY)
                textPlaceable.placeRelative(textX, textY)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = taglineAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(emeraldRingAccent)
            )
            Text(
                text = stringResource(DsmR.string.splash_live_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
                color = mutedTextColor
            )
        }
    }
}

@Composable
private fun AnimatedBrandWord(
    word: String,
    visible: Boolean,
    color: Color,
    staggerMillis: Long
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        word.forEachIndexed { index, letter ->
            var charVisible by remember(word) { mutableStateOf(false) }

            LaunchedEffect(visible) {
                if (visible) {
                    delay(index * staggerMillis)
                    charVisible = true
                }
            }

            val alpha by animateFloatAsState(
                targetValue = if (charVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 260),
                label = "letterAlpha_$index"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (charVisible) 0f else 28f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "letterOffsetY_$index"
            )
            val scale by animateFloatAsState(
                targetValue = if (charVisible) 1f else 0.78f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "letterScale_$index"
            )

            Text(
                text = letter.toString(),
                fontFamily = SpaceGroteskDisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp,
                color = color,
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = offsetY
                    this.scaleX = scale
                    this.scaleY = scale
                }
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SplashScreenPreview() {
    DSMTheme {
        Surface {
            SplashScreen(onSplashFinished = {})
        }
    }
}

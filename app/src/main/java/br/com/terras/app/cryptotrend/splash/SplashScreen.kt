package br.com.terras.app.cryptotrend.splash

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.delay

private val SpaceGroteskDisplayFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Bold)
)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(0xFF0C7C54)
    val emeraldAccent = Color(0xFF1ED788)
    val emeraldSecondary = Color(0xFF34E89E)
    val goldAccent = Color(0xFFFBBF24)
    val primaryTextColor = Color.White
    val mutedTextColor = Color.White.copy(alpha = 0.8f)

    var startLogo by remember { mutableStateOf(false) }
    var startCryptoWord by remember { mutableStateOf(false) }
    var startTrendWord by remember { mutableStateOf(false) }
    var startUnderline by remember { mutableStateOf(false) }
    var startTagline by remember { mutableStateOf(false) }

    val ambientGlowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        ambientGlowAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        startLogo = true
        delay(180L)
        startCryptoWord = true
        delay(220L)
        startTrendWord = true
        delay(380L)
        startUnderline = true
        delay(200L)
        startTagline = true
        delay(1200L)
        onSplashFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (startLogo) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splashLogoScale"
    )

    val underlineProgress by animateFloatAsState(
        targetValue = if (startUnderline) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "splashUnderlineProgress"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = if (startTagline) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "splashTaglineAlpha"
    )

    val taglineOffsetY by animateFloatAsState(
        targetValue = if (startTagline) 0f else 18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "splashTaglineOffsetY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("splash_screen")
    ) {
        // Atmospheric radial glow spanning edge-to-edge behind hero emblem
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(ambientGlowAlpha.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            emeraldAccent.copy(alpha = 0.16f),
                            emeraldSecondary.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        // Unified centered column containing the exact app logo image and texts perfectly aligned together
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.crypto_trend_icon_1795270968907),
                contentDescription = "Crypto Trend Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .testTag("splash_logo_image")
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("splash_animated_title")
            ) {
                AnimatedBrandWord(
                    word = "Crypto",
                    visible = startCryptoWord,
                    color = primaryTextColor,
                    staggerMillis = 42L
                )
                Spacer(modifier = Modifier.width(10.dp))
                AnimatedBrandWord(
                    word = "Trend",
                    visible = startTrendWord,
                    color = emeraldAccent,
                    staggerMillis = 48L
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
                                emeraldSecondary.copy(alpha = 0.25f),
                                emeraldAccent,
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
                text = "Cotações e tendências em tempo real",
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
                    .background(emeraldAccent)
            )
            Text(
                text = "MERCADO AO VIVO",
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
                animationSpec = tween(durationMillis = 280),
                label = "letterAlpha_$index"
            )
            val offsetY by animateFloatAsState(
                targetValue = if (charVisible) 0f else 36f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "letterOffsetY_$index"
            )
            val scale by animateFloatAsState(
                targetValue = if (charVisible) 1f else 0.72f,
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

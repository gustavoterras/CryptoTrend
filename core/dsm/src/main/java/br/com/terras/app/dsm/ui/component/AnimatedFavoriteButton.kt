package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private enum class FavoriteBurstType {
    NONE,
    FAVORITED,
    UNFAVORITED
}

@Composable
fun AnimatedFavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "favorite_button"
) {
    val hapticFeedback = LocalHapticFeedback.current
    val goldColor = MaterialTheme.colorScheme.inversePrimary
    val emeraldColor = MaterialTheme.colorScheme.scrim
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val starScale = remember { Animatable(1f) }
    val starRotation = remember { Animatable(0f) }
    val starOffsetY = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }

    var previousFavorite by remember { mutableStateOf(isFavorite) }
    var activeBurst by remember { mutableStateOf(FavoriteBurstType.NONE) }

    LaunchedEffect(isFavorite) {
        if (isFavorite != previousFavorite) {
            previousFavorite = isFavorite
            if (isFavorite) {
                activeBurst = FavoriteBurstType.FAVORITED
                burstProgress.snapTo(0f)
                coroutineScope {
                    launch {
                        burstProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                        )
                        activeBurst = FavoriteBurstType.NONE
                    }
                    launch {
                        starScale.snapTo(0.72f)
                        starScale.animateTo(
                            targetValue = 1.36f,
                            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                        )
                        starScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        starRotation.snapTo(-18f)
                        starRotation.animateTo(
                            targetValue = 10f,
                            animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                        )
                        starRotation.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        starOffsetY.snapTo(0f)
                        starOffsetY.animateTo(
                            targetValue = -6f,
                            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                        )
                        starOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            } else {
                activeBurst = FavoriteBurstType.UNFAVORITED
                burstProgress.snapTo(0f)
                coroutineScope {
                    launch {
                        burstProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                        )
                        activeBurst = FavoriteBurstType.NONE
                    }
                    launch {
                        starScale.snapTo(1.1f)
                        starScale.animateTo(
                            targetValue = 0.68f,
                            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                        )
                        starScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        starRotation.snapTo(14f)
                        starRotation.animateTo(
                            targetValue = -10f,
                            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                        )
                        starRotation.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        starOffsetY.snapTo(0f)
                        starOffsetY.animateTo(
                            targetValue = 4f,
                            animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                        )
                        starOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
        }
    }

    val iconTint by animateColorAsState(
        targetValue = if (isFavorite) goldColor else mutedColor,
        animationSpec = tween(durationMillis = 240),
        label = "favoriteStarTint"
    )

    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(
                if (isFavorite) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress
            )
            onToggle()
        },
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (activeBurst != FavoriteBurstType.NONE) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    val t = burstProgress.value
                    val center = Offset(size.width / 2f, size.height / 2f)

                    when (activeBurst) {
                        FavoriteBurstType.FAVORITED -> {
                            // Expanding halo shockwave ring
                            val ringRadius = (8.dp + 14.dp * t).toPx()
                            val ringAlpha = (1f - t).coerceIn(0f, 1f) * 0.75f
                            val ringStroke = (2.5.dp * (1f - t * 0.7f)).toPx()
                            drawCircle(
                                color = goldColor.copy(alpha = ringAlpha),
                                radius = ringRadius,
                                center = center,
                                style = Stroke(width = ringStroke)
                            )

                            // 8 radial gold & emerald micro-particles
                            val particleCount = 8
                            for (i in 0 until particleCount) {
                                val angleRad = Math.toRadians(i * (360.0 / particleCount) - 90.0)
                                val dist = (7.dp + 14.dp * t).toPx()
                                val px = center.x + (cos(angleRad) * dist).toFloat()
                                val py = center.y + (sin(angleRad) * dist).toFloat()
                                val dotRadius = (2.6.dp * (1f - t * 0.75f)).toPx()
                                val dotColor = if (i % 2 == 0) goldColor else emeraldColor
                                val dotAlpha = (1f - t).coerceIn(0f, 1f)
                                drawCircle(
                                    color = dotColor.copy(alpha = dotAlpha),
                                    radius = dotRadius,
                                    center = Offset(px, py)
                                )
                            }
                        }
                        FavoriteBurstType.UNFAVORITED -> {
                            // Subtle inward contracting ring on unfavorite
                            val ringRadius = (18.dp - 9.dp * t).toPx()
                            val ringAlpha = (1f - t).coerceIn(0f, 1f) * 0.5f
                            drawCircle(
                                color = mutedColor.copy(alpha = ringAlpha),
                                radius = ringRadius,
                                center = center,
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        }
                        FavoriteBurstType.NONE -> Unit
                    }
                }
            }

            AnimatedContent(
                targetState = isFavorite,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(160)) +
                        scaleIn(
                            initialScale = 0.75f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ))
                        .togetherWith(
                            fadeOut(animationSpec = tween(120)) +
                                scaleOut(targetScale = 0.75f, animationSpec = tween(120))
                        )
                },
                label = "favoriteIconMorph",
                modifier = Modifier.graphicsLayer {
                    scaleX = starScale.value
                    scaleY = starScale.value
                    rotationZ = starRotation.value
                    translationY = starOffsetY.value
                }
            ) { favorited ->
                Icon(
                    painter = painterResource(
                        id = if (favorited) R.drawable.ic_star_filled_24dp else R.drawable.star_24dp
                    ),
                    contentDescription = if (favorited) {
                        "Remover dos favoritos"
                    } else {
                        "Adicionar aos favoritos"
                    },
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AnimatedFavoriteButtonPreview() {
    var favorited by remember { mutableStateOf(false) }
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedFavoriteButton(
                    isFavorite = favorited,
                    onToggle = { favorited = !favorited }
                )
                AnimatedFavoriteButton(
                    isFavorite = true,
                    onToggle = {}
                )
            }
        }
    }
}

package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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

@Composable
fun AnimatedSearchFab(
    isSearchOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "market_search_fab"
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "searchFabPressScale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isSearchOpen) 28.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "searchFabCornerRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSearchOpen) {
            MaterialTheme.colorScheme.inverseSurface
        } else {
            MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(durationMillis = 240),
        label = "searchFabContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSearchOpen) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            MaterialTheme.colorScheme.onSecondary
        },
        animationSpec = tween(durationMillis = 240),
        label = "searchFabContentColor"
    )

    val fabBounce = remember { Animatable(1f) }
    val iconRotation = remember { Animatable(0f) }
    val pulseProgress = remember { Animatable(0f) }
    var showPulse by remember { mutableStateOf(false) }

    var previousOpen by remember { mutableStateOf(isSearchOpen) }
    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen != previousOpen) {
            previousOpen = isSearchOpen
            showPulse = true
            pulseProgress.snapTo(0f)
            coroutineScope {
                launch {
                    pulseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
                    showPulse = false
                }
                launch {
                    fabBounce.snapTo(0.84f)
                    fabBounce.animateTo(
                        targetValue = 1.12f,
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                    )
                    fabBounce.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    iconRotation.snapTo(if (isSearchOpen) -90f else 90f)
                    iconRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    val pulseColor = if (isSearchOpen) {
        MaterialTheme.colorScheme.scrim
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (showPulse) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val t = pulseProgress.value
                val radius = (24.dp + 12.dp * t).toPx()
                val alpha = (1f - t).coerceIn(0f, 1f) * 0.55f
                drawCircle(
                    color = pulseColor.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = (2.5.dp * (1f - t * 0.65f)).toPx())
                )
            }
        }

        FloatingActionButton(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            interactionSource = interactionSource,
            shape = RoundedCornerShape(cornerRadius),
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .graphicsLayer {
                    val combinedScale = pressScale * fabBounce.value
                    scaleX = combinedScale
                    scaleY = combinedScale
                }
                .testTag(testTag)
        ) {
            AnimatedContent(
                targetState = isSearchOpen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) +
                        scaleIn(
                            initialScale = 0.65f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ))
                        .togetherWith(
                            fadeOut(animationSpec = tween(120)) +
                                scaleOut(targetScale = 0.65f, animationSpec = tween(120))
                        )
                },
                label = "searchFabIconMorph",
                modifier = Modifier.graphicsLayer {
                    rotationZ = iconRotation.value
                }
            ) { open ->
                if (open) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar pesquisa"
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.search_24dp),
                        contentDescription = "Pesquisar criptomoedas"
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedSearchClearButton(
    hasQuery: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "market_search_clear_button"
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "searchClearPressScale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (hasQuery) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "searchClearIconRotation"
    )
    val iconTint by animateColorAsState(
        targetValue = if (hasQuery) {
            MaterialTheme.colorScheme.scrim
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "searchClearIconTint"
    )

    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = if (hasQuery) "Limpar pesquisa" else "Fechar pesquisa",
            tint = iconTint,
            modifier = Modifier.graphicsLayer {
                rotationZ = iconRotation
            }
        )
    }
}

@ThemePreviews
@Composable
private fun AnimatedSearchButtonPreview() {
    var open by remember { mutableStateOf(false) }
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedSearchFab(
                    isSearchOpen = open,
                    onClick = { open = !open }
                )
                AnimatedSearchClearButton(
                    hasQuery = open,
                    onClick = { open = !open }
                )
            }
        }
    }
}

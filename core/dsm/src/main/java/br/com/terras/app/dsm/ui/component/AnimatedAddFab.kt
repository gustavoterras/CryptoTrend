package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun AnimatedAddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.portfolio_add_operation),
    testTag: String = "portfolio_add_fab"
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "addFabPressScale"
    )

    var pulseTrigger by remember { mutableIntStateOf(0) }
    val pulseProgress = remember { Animatable(0f) }
    val iconBounce = remember { Animatable(1f) }
    var showPulse by remember { mutableStateOf(false) }

    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            showPulse = true
            pulseProgress.snapTo(0f)
            coroutineScope {
                launch {
                    pulseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    )
                    showPulse = false
                }
                launch {
                    iconBounce.snapTo(0.82f)
                    iconBounce.animateTo(
                        targetValue = 1.18f,
                        animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                    )
                    iconBounce.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    val haloColor = MaterialTheme.colorScheme.scrim

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showPulse) {
            Canvas(modifier = Modifier.requiredSize(72.dp)) {
                val t = pulseProgress.value
                val radius = (18.dp + 15.dp * t).toPx()
                val alpha = (1f - t).coerceIn(0f, 1f) * 0.55f
                drawCircle(
                    color = haloColor.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = (2.2.dp * (1f - t * 0.6f)).toPx())
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(60.dp)
        ) {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .minimumInteractiveComponentSize()
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClickLabel = contentDescription,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pulseTrigger++
                                onClick()
                            }
                        )
                        .testTag(testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.scrim,
                        modifier = Modifier
                            .size(25.dp)
                            .graphicsLayer {
                                scaleX = iconBounce.value
                                scaleY = iconBounce.value
                            }
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AnimatedAddFabPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimatedAddFab(
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

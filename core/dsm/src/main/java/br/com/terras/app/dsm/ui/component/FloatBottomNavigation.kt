package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun FloatBottomNavigation(
    items: List<BottomNavItem>,
    navController: NavController,
    isCollapsed: Boolean = false,
    onExpandRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val hapticFeedback = LocalHapticFeedback.current

    val matchedIndex = items.indexOfFirst { item ->
        currentDestination?.hierarchy?.any {
            it.hasRoute(item.destination::class)
        } == true
    }

    var lastSelectedIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(matchedIndex) {
        if (matchedIndex >= 0) {
            lastSelectedIndex = matchedIndex
        }
    }

    val selectedIndex = if (matchedIndex >= 0) {
        matchedIndex
    } else {
        lastSelectedIndex.coerceIn(0, items.lastIndex)
    }

    val capsuleScale = remember { Animatable(1f) }
    var previousCollapsed by remember { mutableStateOf(isCollapsed) }
    LaunchedEffect(isCollapsed) {
        if (isCollapsed != previousCollapsed) {
            previousCollapsed = isCollapsed
            if (!isCollapsed) {
                // Spring pop when expanding capsule back out
                capsuleScale.snapTo(0.92f)
                capsuleScale.animateTo(
                    targetValue = 1.04f,
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                )
                capsuleScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            } else {
                // Subtle settle when collapsing into single icon
                capsuleScale.snapTo(1.03f)
                capsuleScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 14.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = capsuleScale.value
                    scaleY = capsuleScale.value
                }
                .testTag("floating_bottom_navigation")
        ) {
            Row(
                modifier = Modifier
                    .padding(6.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val isVisible = !isCollapsed || isSelected

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(220)) +
                            expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                expandFrom = Alignment.CenterHorizontally
                            ) +
                            scaleIn(
                                initialScale = 0.8f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                        exit = fadeOut(animationSpec = tween(150)) +
                            shrinkHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                shrinkTowards = Alignment.CenterHorizontally
                            ) +
                            scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(150)
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index > selectedIndex) {
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            FloatingNavIconButton(
                                item = item,
                                isSelected = isSelected,
                                isCollapsed = isCollapsed,
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isCollapsed) {
                                        onExpandRequest()
                                    } else {
                                        navController.navigate(item.destination) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )

                            if (index < selectedIndex) {
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavIconButton(
    item: BottomNavItem,
    isSelected: Boolean,
    isCollapsed: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "floatingNavPressScale"
    )

    val selectionBounce = remember { Animatable(1f) }
    val selectionTilt = remember { Animatable(0f) }
    val selectionOffsetY = remember { Animatable(0f) }
    val haloPulseProgress = remember { Animatable(0f) }
    var showHaloPulse by remember { mutableStateOf(false) }

    var previousSelected by remember { mutableStateOf(isSelected) }
    LaunchedEffect(isSelected) {
        if (isSelected != previousSelected) {
            previousSelected = isSelected
            if (isSelected) {
                showHaloPulse = true
                haloPulseProgress.snapTo(0f)
                coroutineScope {
                    launch {
                        haloPulseProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                        )
                        showHaloPulse = false
                    }
                    launch {
                        selectionBounce.snapTo(0.84f)
                        selectionBounce.animateTo(
                            targetValue = 1.22f,
                            animationSpec = tween(durationMillis = 115, easing = FastOutSlowInEasing)
                        )
                        selectionBounce.animateTo(
                            targetValue = 1.06f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        selectionTilt.snapTo(-12f)
                        selectionTilt.animateTo(
                            targetValue = 7f,
                            animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
                        )
                        selectionTilt.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    launch {
                        selectionOffsetY.snapTo(0f)
                        selectionOffsetY.animateTo(
                            targetValue = -3.5f,
                            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                        )
                        selectionOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            } else {
                selectionBounce.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(220),
        label = "floatingNavContainerColor"
    )
    val pillScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floatingNavPillScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.scrim
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "floatingNavIconTint"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "floatingNavDotScale"
    )

    val haloColor = MaterialTheme.colorScheme.scrim

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
                role = Role.Tab,
                onClickLabel = if (isCollapsed) {
                    "Expandir barra de navegação"
                } else {
                    item.title
                },
                onClick = onClick
            )
            .testTag("floating_nav_item_${item.title.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        // Animated active pill backdrop
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = pillScale
                    scaleY = pillScale
                }
                .clip(CircleShape)
                .background(containerColor)
        )

        // Expanding emerald halo shockwave when selected
        if (showHaloPulse) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val t = haloPulseProgress.value
                val radius = (11.dp + 12.dp * t).toPx()
                val alpha = (1f - t).coerceIn(0f, 1f) * 0.55f
                drawCircle(
                    color = haloColor.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = (2.dp * (1f - t * 0.6f)).toPx())
                )
            }
        }

        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = if (isCollapsed) {
                "${item.title}, tocar para expandir navegação"
            } else {
                item.title
            },
            tint = iconTint,
            modifier = Modifier
                .size(23.dp)
                .offset(y = if (isSelected) (-1).dp else 0.dp)
                .graphicsLayer {
                    val effectiveScale = if (isSelected) selectionBounce.value.coerceAtLeast(1.06f) else 1f
                    scaleX = effectiveScale
                    scaleY = effectiveScale
                    rotationZ = selectionTilt.value
                    translationY = selectionOffsetY.value
                }
        )

        // Micro active indicator dot at bottom of active pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .size(4.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                    alpha = dotScale.coerceIn(0f, 1f)
                }
                .clip(CircleShape)
                .background(haloColor)
        )
    }
}

@ThemePreviews
@Composable
fun FloatBottomNavigationPreview() {
    val items = listOf(
        BottomNavItem("Mercado", android.R.drawable.star_on, "Mercado"),
        BottomNavItem("Notícias", android.R.drawable.sym_action_call, "Notícias")
    )

    DSMTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatBottomNavigation(
                items = items,
                navController = rememberNavController(),
                isCollapsed = false
            )
            FloatBottomNavigation(
                items = items,
                navController = rememberNavController(),
                isCollapsed = true
            )
        }
    }
}
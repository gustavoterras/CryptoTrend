package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
            modifier = Modifier.testTag("floating_bottom_navigation")
        ) {
            Row(
                modifier = Modifier
                    .padding(6.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
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
                                initialScale = 0.85f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                        exit = fadeOut(animationSpec = tween(150)) +
                            shrinkHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                shrinkTowards = Alignment.CenterHorizontally
                            ) +
                            scaleOut(
                                targetScale = 0.85f,
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
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "floatingNavContainerColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.scrim
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "floatingNavIconTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "floatingNavIconScale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
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
        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = if (isCollapsed) {
                "${item.title}, tocar para expandir navegação"
            } else {
                item.title
            },
            tint = iconTint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
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
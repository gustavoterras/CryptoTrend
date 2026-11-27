package br.com.terras.app.cryptotrend.debug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.cryptotrend.core.dsm.R as DsmR
import br.com.terras.app.dsm.ui.component.AnimatedAddFab
import br.com.terras.app.dsm.ui.component.AnimatedFavoriteButton
import br.com.terras.app.dsm.ui.component.AnimatedSearchClearButton
import br.com.terras.app.dsm.ui.component.AnimatedSearchFab
import br.com.terras.app.dsm.ui.component.BottomNavItem
import br.com.terras.app.dsm.ui.component.FloatBottomNavigation
import br.com.terras.app.dsm.ui.component.MarketSortBar
import br.com.terras.app.dsm.ui.component.MarketSortState
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.RippleButtonSize
import br.com.terras.app.dsm.ui.component.SegmentedControl
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.component.portfolioChartPalette
import br.com.terras.app.dsm.ui.theme.typography
import br.com.terras.app.navigation.Screen

@Composable
internal fun CatalogSectionBlock(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

@Composable
internal fun CatalogDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorTokensCatalogSection() {
    val scheme = MaterialTheme.colorScheme
    val swatches = listOf(
        "background" to scheme.background,
        "primary" to scheme.primary,
        "tertiary" to scheme.tertiary,
        "secondary" to scheme.secondary,
        "secondaryContainer" to scheme.secondaryContainer,
        "onSecondaryContainer" to scheme.onSecondaryContainer,
        "scrim" to scheme.scrim,
        "surface" to scheme.surface,
        "onSurfaceVariant" to scheme.onSurfaceVariant,
        "outlineVariant" to scheme.outlineVariant,
        "error" to scheme.error,
        "inversePrimary" to scheme.inversePrimary
    )
    val chartColors = portfolioChartPalette()

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_colors),
        subtitle = "DSMTheme ColorScheme & portfolioChartPalette()"
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            swatches.forEach { (name, color) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
                        .background(scheme.primary)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, scheme.outlineVariant, CircleShape)
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = scheme.surface
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chartColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
internal fun TypographyCatalogSection() {
    val entries: List<Pair<String, TextStyle>> = listOf(
        "headlineLarge · 28sp ExtraBold" to typography.headlineLarge,
        "headlineMedium · 26sp Bold" to typography.headlineMedium,
        "headlineSmall · 24sp SemiBold" to typography.headlineSmall,
        "titleLarge · 18sp Regular" to typography.titleLarge,
        "bodyLarge · 16sp ExtraLight" to typography.bodyLarge,
        "bodyMedium · 14sp Light" to typography.bodyMedium,
        "labelLarge · 12sp Bold" to typography.labelLarge,
        "labelMedium · 12sp Light" to typography.labelMedium,
        "labelSmall · 10sp Normal" to typography.labelSmall
    )

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_typography),
        subtitle = "avenirNextFontFamily"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { (label, style) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "CryptoTrend BTC $96,420.50",
                        style = style,
                        color = MaterialTheme.colorScheme.surface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun StickyHeaderCatalogSection() {
    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_sticky_header),
        subtitle = "App top brand header"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .consumeWindowInsets(WindowInsets.statusBars)
        ) {
            StickyHeaderView()
        }
    }
}

@Composable
internal fun FloatBottomNavCatalogSection() {
    val previewNavController = rememberNavController()
    var isCollapsed by remember { mutableStateOf(false) }
    val items = listOf(
        BottomNavItem(stringResource(DsmR.string.nav_market), DsmR.drawable.storefront_24dp, Screen.CoinsList),
        BottomNavItem(stringResource(DsmR.string.nav_portfolio), DsmR.drawable.ic_portfolio_24dp, Screen.Portfolio),
        BottomNavItem(stringResource(DsmR.string.nav_news), DsmR.drawable.newspaper_24dp, Screen.Feeds)
    )

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_bottom_nav),
        subtitle = "Capsule bottom navigation with spring collapse"
    ) {
        NavHost(
            navController = previewNavController,
            startDestination = Screen.CoinsList,
            modifier = Modifier.size(0.dp)
        ) {
            composable<Screen.CoinsList> {}
            composable<Screen.Portfolio> {}
            composable<Screen.Feeds> {}
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { isCollapsed = !isCollapsed },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("dsm_toggle_bottom_nav_collapse"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = stringResource(
                        R.string.dsm_action_toggle_collapse,
                        stringResource(if (isCollapsed) R.string.dsm_state_on else R.string.dsm_state_off)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Box(
                modifier = Modifier.consumeWindowInsets(WindowInsets(0, 0, 0, 0)),
                contentAlignment = Alignment.Center
            ) {
                FloatBottomNavigation(
                    items = items,
                    navController = previewNavController,
                    isCollapsed = isCollapsed,
                    onExpandRequest = { isCollapsed = false }
                )
            }
        }
    }
}

@Composable
internal fun RippleButtonsCatalogSection() {
    var clickCount by remember { mutableIntStateOf(0) }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_buttons),
        subtitle = stringResource(R.string.dsm_label_clicks, clickCount)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RippleButton(text = "SMALL", size = RippleButtonSize.SMALL) { clickCount++ }
            RippleButton(text = "MEDIUM", size = RippleButtonSize.MEDIUM) { clickCount++ }
            RippleButton(text = "LARGE", size = RippleButtonSize.LARGE) { clickCount++ }
        }
    }
}

@Composable
internal fun SegmentedControlCatalogSection() {
    val periods = listOf("1D", "7D", "30D", "90D", "1Y")
    var selectedEqualIndex by remember { mutableIntStateOf(0) }
    var selectedCompactIndex by remember { mutableIntStateOf(1) }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_segmented),
        subtitle = stringResource(R.string.dsm_label_selected, periods[selectedEqualIndex])
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiary,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedControl(
                items = periods,
                defaultSelectedItemIndex = selectedEqualIndex,
                equalWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                onItemSelection = { selectedEqualIndex = it }
            )
        }

        SegmentedControl(
            items = listOf("BRL", "USD", "BTC"),
            defaultSelectedItemIndex = selectedCompactIndex,
            useFixedWidth = true,
            itemWidth = 68.dp,
            onItemSelection = { selectedCompactIndex = it }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimatedActionsCatalogSection() {
    var favOff by remember { mutableStateOf(false) }
    var favOn by remember { mutableStateOf(true) }
    var searchOpen by remember { mutableStateOf(false) }
    var hasQuery by remember { mutableStateOf(true) }
    var addCount by remember { mutableIntStateOf(0) }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_animated_actions),
        subtitle = "AnimatedFavoriteButton · AnimatedAddFab · AnimatedSearchFab · Clear (${stringResource(R.string.dsm_label_clicks, addCount)})"
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedFavoriteButton(
                isFavorite = favOff,
                onToggle = { favOff = !favOff },
                testTag = "dsm_fav_button_1"
            )
            AnimatedFavoriteButton(
                isFavorite = favOn,
                onToggle = { favOn = !favOn },
                testTag = "dsm_fav_button_2"
            )
            AnimatedAddFab(
                onClick = { addCount++ },
                testTag = "dsm_add_fab"
            )
            AnimatedSearchFab(
                isSearchOpen = searchOpen,
                onClick = { searchOpen = !searchOpen },
                testTag = "dsm_search_fab"
            )
            AnimatedSearchClearButton(
                hasQuery = hasQuery,
                onClick = { hasQuery = !hasQuery },
                testTag = "dsm_search_clear"
            )
        }
    }
}

@Composable
internal fun MarketSortBarCatalogSection() {
    var sortState by remember { mutableStateOf(MarketSortState()) }
    var hasFavoriteColumn by remember { mutableStateOf(false) }

    val stateLabel = sortState.column?.let { col ->
        stringResource(R.string.dsm_label_sort_state, col.name, sortState.direction.name)
    } ?: stringResource(R.string.dsm_label_sort_none)

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_sort_bar),
        subtitle = stateLabel
    ) {
        OutlinedButton(
            onClick = { hasFavoriteColumn = !hasFavoriteColumn },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("dsm_toggle_favorite_col"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = stringResource(
                    R.string.dsm_action_favorite_col,
                    stringResource(if (hasFavoriteColumn) R.string.dsm_state_on else R.string.dsm_state_off)
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketSortBar(
                sortState = sortState,
                onSortClick = { column -> sortState = sortState.toggle(column) },
                hasFavoriteColumn = hasFavoriteColumn
            )
        }
    }
}

package br.com.terras.app.cryptotrend.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.dsm.ui.component.WebViewScreen
import kotlinx.coroutines.launch

private enum class CatalogCategory(val labelResId: Int) {
    ALL(R.string.dsm_cat_all),
    TOKENS(R.string.dsm_cat_tokens),
    NAVIGATION(R.string.dsm_cat_navigation),
    CONTROLS(R.string.dsm_cat_controls),
    CHARTS(R.string.dsm_cat_charts),
    LISTS(R.string.dsm_cat_lists)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsmCatalogScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by rememberSaveable { mutableStateOf(CatalogCategory.ALL) }
    var showWebViewPreview by rememberSaveable { mutableStateOf(false) }

    if (showWebViewPreview) {
        BackHandler { showWebViewPreview = false }
        Box(modifier = Modifier.fillMaxSize()) {
            WebViewScreen(url = "https://www.coingecko.com")
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { showWebViewPreview = false },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dsm_webview_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.dsm_catalog_back_cd),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dsm_catalog_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.dsm_catalog_back_cd),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.dsm_catalog_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dsm_catalog_debug_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = stringResource(R.string.dsm_catalog_theme_cd),
                                    onClick = onToggleTheme
                                )
                                .testTag("dsm_catalog_theme_toggle")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isDarkTheme) R.string.dsm_catalog_theme_dark
                                        else R.string.dsm_catalog_theme_light
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogCategory.entries.forEach { category ->
                        val selected = selectedCategory == category
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.scrim
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Tab) { selectedCategory = category }
                                .testTag("dsm_filter_chip_${category.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(category.labelResId),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    ) { innerPadding ->
        val showTokens = selectedCategory == CatalogCategory.ALL || selectedCategory == CatalogCategory.TOKENS
        val showNav = selectedCategory == CatalogCategory.ALL || selectedCategory == CatalogCategory.NAVIGATION
        val showControls = selectedCategory == CatalogCategory.ALL || selectedCategory == CatalogCategory.CONTROLS
        val showCharts = selectedCategory == CatalogCategory.ALL || selectedCategory == CatalogCategory.CHARTS
        val showLists = selectedCategory == CatalogCategory.ALL || selectedCategory == CatalogCategory.LISTS

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (showTokens) {
                item(key = "colors") { ColorTokensCatalogSection() }
                item(key = "div_colors") { CatalogDivider() }
                item(key = "typography") { TypographyCatalogSection() }
                if (showNav || showControls || showCharts || showLists) {
                    item(key = "div_typo") { CatalogDivider() }
                }
            }

            if (showNav) {
                item(key = "sticky_header") { StickyHeaderCatalogSection() }
                item(key = "div_sticky") { CatalogDivider() }
                item(key = "bottom_nav") { FloatBottomNavCatalogSection() }
                if (showControls || showCharts || showLists) {
                    item(key = "div_nav") { CatalogDivider() }
                }
            }

            if (showControls) {
                item(key = "ripple_buttons") { RippleButtonsCatalogSection() }
                item(key = "div_buttons") { CatalogDivider() }
                item(key = "segmented_control") { SegmentedControlCatalogSection() }
                item(key = "div_segmented") { CatalogDivider() }
                item(key = "animated_actions") { AnimatedActionsCatalogSection() }
                item(key = "div_actions") { CatalogDivider() }
                item(key = "sort_bar") { MarketSortBarCatalogSection() }
                if (showCharts || showLists) {
                    item(key = "div_sort") { CatalogDivider() }
                }
            }

            if (showCharts) {
                item(key = "donut_chart") { DonutChartCatalogSection() }
                item(key = "div_donut") { CatalogDivider() }
                item(key = "sparklines") { SparklineAndTrendCatalogSection() }
                if (showLists) {
                    item(key = "div_sparkline") { CatalogDivider() }
                }
            }

            if (showLists) {
                item(key = "coin_list_items") {
                    CoinListItemCatalogSection(
                        onCoinClick = { coinName ->
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.dsm_snack_coin_clicked, coinName)
                                )
                            }
                        }
                    )
                }
                item(key = "div_coin_items") { CatalogDivider() }
                item(key = "shimmer") { ShimmerCatalogSection() }
                item(key = "div_shimmer") { CatalogDivider() }
                item(key = "pull_refresh_webview") {
                    PullToRefreshAndWebViewCatalogSection(
                        onOpenWebView = { showWebViewPreview = true }
                    )
                }
            }
        }
    }
}

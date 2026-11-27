package br.com.terras.app.cryptotrend.debug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.dsm.ui.component.AllocationDonutChart
import br.com.terras.app.dsm.ui.component.AllocationSlice
import br.com.terras.app.dsm.ui.component.CoinListItemShimmerRow
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.PullToRefreshWrapper
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.SparklineChartView
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.component.portfolioChartPalette
import kotlinx.coroutines.delay

@Composable
internal fun DonutChartCatalogSection() {
    val palette = portfolioChartPalette()
    var selectedSliceId by remember { mutableStateOf<String?>("bitcoin") }
    val slices = remember(palette) {
        listOf(
            AllocationSlice("bitcoin", "BTC", "Bitcoin", "R$ 52.000,00", 52f, "52,0%", palette[0]),
            AllocationSlice("ethereum", "ETH", "Ethereum", "R$ 28.000,00", 28f, "28,0%", palette[1]),
            AllocationSlice("solana", "SOL", "Solana", "R$ 14.000,00", 14f, "14,0%", palette[2]),
            AllocationSlice("chainlink", "LINK", "Chainlink", "R$ 6.000,00", 6f, "6,0%", palette[3])
        )
    }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_donut),
        subtitle = "Tap chart slices or legend rows"
    ) {
        AllocationDonutChart(
            slices = slices,
            totalValueText = "R$ 100.000,00",
            assetCountLabel = "4 ativos",
            selectedSliceId = selectedSliceId,
            onSelectSlice = { selectedSliceId = it }
        )
    }
}

@Composable
internal fun SparklineAndTrendCatalogSection() {
    val trends = listOf(
        Triple("UP (+4.2%)", TrendColor.UP, listOf(10.0, 11.2, 10.8, 12.1, 13.0, 12.6, 14.5)),
        Triple("DOWN (-3.1%)", TrendColor.DOWN, listOf(14.5, 13.8, 14.1, 12.9, 12.2, 11.9, 10.8)),
        Triple("FLAT (0.0%)", TrendColor.FLAT, listOf(12.0, 12.1, 11.9, 12.0, 12.05, 11.98, 12.0))
    )

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_sparkline),
        subtitle = "TrendColor badges + SparklineChartView"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            trends.forEach { (label, trend, prices) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = trend.color,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = trend.onColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    SparklineChartView(
                        prices = prices,
                        lineColor = trend.textColor,
                        modifier = Modifier
                            .width(110.dp)
                            .height(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CoinListItemCatalogSection(
    onCoinClick: (String) -> Unit
) {
    var btcFav by remember { mutableStateOf(true) }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_coin_item),
        subtitle = "UP · DOWN · FLAT variants"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            CoinListItemView(
                coinId = "bitcoin",
                coinLogo = "",
                coinName = "Bitcoin",
                coinSymbol = "BTC",
                coinPosition = "1",
                coinPrice = "R$ 512.430,00",
                coinPriceChancePercentage = "+3.42%",
                trendColor = TrendColor.UP,
                coinVolume = "R$ 142B",
                sparklinePrices = listOf(492000.0, 498500.0, 496100.0, 504000.0, 509200.0, 512430.0),
                isFavorite = btcFav,
                onItemClick = {
                    btcFav = !btcFav
                    onCoinClick("Bitcoin")
                }
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            CoinListItemView(
                coinId = "ethereum",
                coinLogo = "",
                coinName = "Ethereum",
                coinSymbol = "ETH",
                coinPosition = "2",
                coinPrice = "R$ 18.210,50",
                coinPriceChancePercentage = "-2.15%",
                trendColor = TrendColor.DOWN,
                coinVolume = "R$ 58B",
                sparklinePrices = listOf(18800.0, 18720.0, 18590.0, 18440.0, 18310.0, 18210.5),
                isFavorite = false,
                onItemClick = { onCoinClick("Ethereum") }
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            CoinListItemView(
                coinId = "tether",
                coinLogo = "",
                coinName = "Tether",
                coinSymbol = "USDT",
                coinPosition = "3",
                coinPrice = "R$ 5,78",
                coinPriceChancePercentage = "0.00%",
                trendColor = TrendColor.FLAT,
                coinVolume = "R$ 210B",
                sparklinePrices = listOf(5.78, 5.79, 5.77, 5.78, 5.78, 5.78),
                isFavorite = false,
                onItemClick = { onCoinClick("Tether") }
            )
        }
    }
}

@Composable
internal fun ShimmerCatalogSection() {
    var showShimmer by remember { mutableStateOf(true) }
    val brush = ShimmerEffect(showShimmer = showShimmer)

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_shimmer),
        subtitle = "Variants 0, 1, 2 + ShimmerEffect brush"
    ) {
        OutlinedButton(
            onClick = { showShimmer = !showShimmer },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("dsm_toggle_shimmer"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = stringResource(
                    R.string.dsm_action_shimmer_on,
                    stringResource(if (showShimmer) R.string.dsm_state_on else R.string.dsm_state_off)
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            CoinListItemShimmerRow(variantIndex = 0)
            CoinListItemShimmerRow(variantIndex = 1)
            CoinListItemShimmerRow(variantIndex = 2)
        }
    }
}

@Composable
internal fun PullToRefreshAndWebViewCatalogSection(
    onOpenWebView: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1200L)
            isRefreshing = false
        }
    }

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_pull_refresh),
        subtitle = stringResource(R.string.dsm_label_pull_hint)
    ) {
        Button(
            onClick = { isRefreshing = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("dsm_simulate_refresh_button")
        ) {
            Text(
                text = stringResource(R.string.dsm_action_simulate_refresh),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
        ) {
            PullToRefreshWrapper(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true },
                indicatorTopPadding = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "PullToRefreshContainer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
    CatalogDivider()
    Spacer(modifier = Modifier.height(6.dp))

    CatalogSectionBlock(
        title = stringResource(R.string.dsm_sec_webview),
        subtitle = stringResource(R.string.dsm_label_webview_hint)
    ) {
        OutlinedButton(
            onClick = onOpenWebView,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("dsm_open_webview_button"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = stringResource(R.string.dsm_action_open_webview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

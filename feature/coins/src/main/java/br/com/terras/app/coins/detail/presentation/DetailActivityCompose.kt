@file:OptIn(ExperimentalGlideComposeApi::class)

package br.com.terras.app.coins.detail.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.InsightsError
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessInsights
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.AnimatedFavoriteButton
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.SegmentedControl
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.navigation.Screen
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@Composable
fun DetailActivityCompose(
    id: String, navHostController: NavHostController, viewModel: DetailViewModel = hiltViewModel()
) {
    val coinDetailState by viewModel.detail.collectAsState()
    val chartDetailState by viewModel.chart.collectAsState()
    val newsState by viewModel.news.collectAsState()
    val insightsState by viewModel.insights.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    LaunchedEffect(id) {
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail(id))
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadChart(id))
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadInsights(id))
    }

    LaunchedEffect(coinDetailState) {
        if (coinDetailState is SuccessDetail) {
            val coinName = (coinDetailState as SuccessDetail).coin.name
            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadNews(coinName))
        }
    }

    when (val state = coinDetailState) {
        is Loading -> OnCoinDetailLoading(
            onBackPress = { navHostController.popBackStack() }
        )
        is SuccessDetail -> OnCoinDetailSuccess(
            state = state,
            chartDetailState = chartDetailState,
            newsState = newsState,
            insightsState = insightsState,
            onBackPress = {
                navHostController.popBackStack()
            },
            onArticleClick = { url ->
                if (url.isNotBlank()) {
                    navHostController.navigate(Screen.WebView(url = url))
                }
            },
            isFavorite = isFavorite,
            onToggleFavorite = {
                viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleFavorite(id))
            }
        ) { days ->
            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadChart(id, days))
        }
        is DetailViewModel.DetailState.Error -> OnCoinDetailError(
            onBackPress = { navHostController.popBackStack() },
            onRetry = {
                viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail(id))
                viewModel.handleIntent(DetailViewModel.DetailIntent.LoadChart(id))
                viewModel.handleIntent(DetailViewModel.DetailIntent.LoadInsights(id))
            }
        )
        else -> {}
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnCoinDetailSuccess(
    state: SuccessDetail,
    chartDetailState: DetailViewModel.DetailState,
    newsState: DetailViewModel.DetailState,
    insightsState: DetailViewModel.DetailState,
    onBackPress: () -> Unit,
    onArticleClick: (String) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    selectedDaysOfChart: (Int) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isBarChart by remember { mutableStateOf(false) }
    val quoteCurrency = remember {
        if (Locale.getDefault().language == "pt") "BRL" else "USD"
    }
    val volume24h = remember(state.coin.marketDataList) {
        state.coin.marketDataList
            .firstOrNull { it.first == "Trading Volume 24h" }
            ?.second
            ?.takeIf { it.isNotBlank() }
            ?: "-"
    }
    val highPrice24h = remember(state.coin.marketDataList) {
        state.coin.marketDataList
            .firstOrNull { it.first == "Highest Price 24h" }
            ?.second
            ?.takeIf { it.isNotBlank() }
            ?: "-"
    }
    val lowPrice24h = remember(state.coin.marketDataList) {
        state.coin.marketDataList
            .firstOrNull { it.first == "Lowest Price 24h" }
            ?.second
            ?.takeIf { it.isNotBlank() }
            ?: "-"
    }
    val change24h = remember(state.coin.priceChangePercentageList) {
        state.coin.priceChangePercentageList.firstOrNull()?.priceChangeValue.orEmpty()
    }
    val referencePrice = remember(chartDetailState, state.coin.price) {
        (chartDetailState as? SuccessChart)
            ?.chart
            ?.chartData
            ?.firstOrNull()
            ?.yLabel
            ?.takeIf { it.isNotBlank() }
            ?: state.coin.price
    }

    BackHandler(onBack = onBackPress)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlideImage(
                        model = state.coin.image,
                        contentDescription = state.coin.name,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "${state.coin.symbol.uppercase()}/$quoteCurrency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.coin.marketCapRank.isNotBlank()) {
                        Text(
                            text = "#${state.coin.marketCapRank}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                navigationIconContentColor = MaterialTheme.colorScheme.surface,
                actionIconContentColor = MaterialTheme.colorScheme.inversePrimary
            ),
            navigationIcon = {
                IconButton(
                    onClick = onBackPress,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
            },
            actions = {
                AnimatedFavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = {
                        val willFavorite = !isFavorite
                        onToggleFavorite()
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                if (willFavorite) {
                                    "${state.coin.symbol.uppercase()} adicionado aos favoritos"
                                } else {
                                    "${state.coin.symbol.uppercase()} removido dos favoritos"
                                }
                            )
                        }
                    },
                    testTag = "favorite_button"
                )
            }
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = state.coin.price,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = referencePrice,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (change24h.isNotBlank()) {
                                Text(
                                    text = "($change24h)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = state.coin.trendColor.textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChartHeaderStatRow(label = "24h Vol", value = volume24h)
                        ChartHeaderStatRow(label = "High Price", value = highPrice24h)
                        ChartHeaderStatRow(label = "Low Price", value = lowPrice24h)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                when (chartDetailState) {
                    is SuccessChart -> {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                        ) {
                            val totalButtons = DaysOfChart.entries.size + 2
                            val spacingBetweenButtons = 4.dp
                            val minGroupGap = 16.dp
                            val totalOverhead = (spacingBetweenButtons * (DaysOfChart.entries.size - 1)) +
                                spacingBetweenButtons +
                                minGroupGap
                            val buttonWidth = ((maxWidth - totalOverhead) / totalButtons)
                                .coerceIn(28.dp, 36.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SegmentedControl(
                                    items = DaysOfChart.entries.map { day -> day.text },
                                    defaultSelectedItemIndex = DaysOfChart.entries
                                        .indexOfFirst { it.value == chartDetailState.days }
                                        .coerceAtLeast(0),
                                    useFixedWidth = true,
                                    itemWidth = buttonWidth,
                                    itemHeight = 30.dp,
                                    itemSpacing = spacingBetweenButtons,
                                    chipHorizontalPadding = 0.dp,
                                    chipVerticalPadding = 0.dp,
                                    textStyle = MaterialTheme.typography.labelSmall
                                ) { index ->
                                    selectedDaysOfChart(DaysOfChart.entries[index].value)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(spacingBetweenButtons),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val activeToggleTint = if (MaterialTheme.colorScheme.scrim.luminance() > 0.4f) {
                                        Color(0xFF052E16)
                                    } else {
                                        Color.White
                                    }
                                    val inactiveToggleBg = Color.Transparent

                                    Box(
                                        modifier = Modifier
                                            .size(width = buttonWidth, height = 30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isBarChart) MaterialTheme.colorScheme.scrim else inactiveToggleBg
                                            )
                                            .testTag("chart_type_bar_button")
                                            .clickable { isBarChart = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_bar_chart_24dp),
                                            contentDescription = "Gráfico de Barras",
                                            tint = if (isBarChart) activeToggleTint else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = buttonWidth, height = 30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (!isBarChart) MaterialTheme.colorScheme.scrim else inactiveToggleBg
                                            )
                                            .testTag("chart_type_line_button")
                                            .clickable { isBarChart = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_show_chart_24dp),
                                            contentDescription = "Gráfico de Linhas",
                                            tint = if (!isBarChart) activeToggleTint else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (isBarChart) {
                            BarChartCompose(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(265.dp),
                                data = chartDetailState.chart.chartData,
                                coinInfo = state.coin,
                                selectedDays = chartDetailState.days,
                                showHighLowCallouts = true,
                                pointFillColor = MaterialTheme.colorScheme.background
                            )
                        } else {
                            LineChartCompose(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(265.dp),
                                data = chartDetailState.chart.chartData,
                                graphColor = MaterialTheme.colorScheme.scrim,
                                showDashedLine = false,
                                showYLabels = true,
                                showHighLowCallouts = true,
                                pointFillColor = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                    else -> {}
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                state.coin.priceChangePercentageList.forEachIndexed { index, item ->
                    SectionContentItem(
                        showPaddingStart = index == 0,
                        showPaddingEnd = index == state.coin.priceChangePercentageList.lastIndex,
                        item = item
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = "Sobre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                state.coin.marketDataList.forEachIndexed { index, pair ->
                    SectionInfoItem(
                        name = pair.first,
                        value = pair.second,
                        showDivider = index != state.coin.marketDataList.lastIndex
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // CoinGecko Insights Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .testTag("coin_insights_section"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_show_chart_24dp),
                        contentDescription = "Insights",
                        tint = MaterialTheme.colorScheme.scrim,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Insights de Mercado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }

                when (insightsState) {
                    is SuccessInsights -> {
                        if (insightsState.insights.isEmpty()) {
                            Text(
                                text = "Nenhum insight disponível para ${state.coin.name} no momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            insightsState.insights.forEachIndexed { index, insight ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (insight.title.isNotBlank()) {
                                        Text(
                                            text = insight.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.surface
                                        )
                                    }
                                    if (insight.description.isNotBlank()) {
                                        Text(
                                            text = insight.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (insight.postedAt.isNotBlank()) {
                                        Text(
                                            text = insight.postedAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                if (index != insightsState.insights.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                    is InsightsError -> {
                        Text(
                            text = insightsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "Carregando insights de ${state.coin.name}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // News Section Filtered by Token in Horizontal Grid
            when (newsState) {
                is DetailViewModel.DetailState.SuccessNews -> {
                    val articles = (newsState as DetailViewModel.DetailState.SuccessNews).articles
                    if (articles.isEmpty()) {
                        Text(
                            text = "Nenhuma notícia encontrada para ${state.coin.name}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(articles.size) { index ->
                                val article = articles[index]
                                Column(
                                    modifier = Modifier
                                        .width(248.dp)
                                        .height(232.dp)
                                        .clickable { onArticleClick(article.url) },
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (article.urlToImage.isNotBlank()) {
                                            GlideImage(
                                                model = article.urlToImage,
                                                contentDescription = article.title,
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(112.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                        Text(
                                            text = article.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.surface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = article.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Ver mais",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.surfaceTint,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Carregando notícias...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartHeaderStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.surface,
            maxLines = 1,
            textAlign = TextAlign.End,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 6.dp)
        )
    }
}

@Composable
private fun SectionContentItem(showPaddingStart: Boolean, showPaddingEnd: Boolean, item: CoinDetailPricePercentageVO) {
    val paddingStart = if (showPaddingStart) 18.dp else 0.dp
    val paddingEnd = if (showPaddingEnd) 18.dp else 0.dp
    Column(
        modifier = Modifier
            .padding(start = paddingStart, end = paddingEnd, top = 4.dp, bottom = 4.dp)
            .width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.priceChangeTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = item.priceChangeValue,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = item.trendColor.textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SectionInfoItem(
    name: String,
    value: String,
    showDivider: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.size(0.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.surface,
        )
    }

    if (showDivider) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnCoinDetailLoading(
    onBackPress: () -> Unit
) {
    BackHandler(onBack = onBackPress)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackPress,
                        modifier = Modifier.testTag("loading_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.scrim
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnCoinDetailError(
    onBackPress: () -> Unit,
    onRetry: () -> Unit
) {
    BackHandler(onBack = onBackPress)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackPress,
                        modifier = Modifier.testTag("error_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Erro ao carregar os dados, tente novamente mais tarde.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            RippleButton(
                text = "Tentar novamente",
                onClick = onRetry
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CoinsActivityComposePreview() {
    DSMTheme {
        Surface {
            OnCoinDetailSuccess(state = SuccessDetail(
                coin = CoinDetailVO(
                    symbol = "BTC",
                    name = "Bitcoin",
                    image = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png?1547033579",
                    marketCapRank = "1",
                    price = "R$100,00",
                    priceChangePercentageList = arrayListOf(
                        CoinDetailPricePercentageVO(
                            priceChangeTime = "1d",
                            priceChangeValue = "+10%",
                            trendColor = TrendColor.UP
                        ),
                        CoinDetailPricePercentageVO(
                            priceChangeTime = "7d",
                            priceChangeValue = "-10%",
                            trendColor = TrendColor.DOWN
                        ),
                        CoinDetailPricePercentageVO(
                            priceChangeTime = "30d",
                            priceChangeValue = "-10%",
                            trendColor = TrendColor.DOWN
                        ), CoinDetailPricePercentageVO(
                            priceChangeTime = "90d",
                            priceChangeValue = "-10%",
                            trendColor = TrendColor.DOWN
                        ), CoinDetailPricePercentageVO(
                            priceChangeTime = "1y",
                            priceChangeValue = "-10%",
                            trendColor = TrendColor.DOWN
                        )
                    ),
                    marketDataList = listOf(
                        Pair("Market Cap", "R$100,00"),
                        Pair("Trading Volume 24h", "R$100,00"),
                        Pair("Highest Price 24h", "R$100,00"),
                        Pair("Lowest Price 24h", "R$100,00")
                    ),
                    trendColor = TrendColor.UP
                )
            ), chartDetailState = SuccessChart(
                chart = CoinChartDetailVO(
                    listOf(
                        DataPoint(
                            "price at May 21, 2024 ", 100.0, "$100.00", "$0.00"
                        ), DataPoint(
                            "price at May 21, 2024 ", 50.0, "$100.00", "$0.00"
                        ), DataPoint(
                            "price at May 21, 2024 ", 100.0, "$100.00", "$0.00"
                        ), DataPoint(
                            "price at May 21, 2024 ", 10.0, "$100.00", "$0.00"
                        )
                    )
                )
            ),
            newsState = DetailViewModel.DetailState.SuccessNews(emptyList()),
            insightsState = SuccessInsights(
                listOf(
                    CoinInsightVO(
                        title = "Volume institucional em alta",
                        description = "Fluxo de entrada líquido positivo em exchanges nas últimas 24h.",
                        postedAt = "20/11/2026"
                    )
                )
            ),
            onBackPress = {},
            onArticleClick = {},
            selectedDaysOfChart = {}
            )
        }
    }
}
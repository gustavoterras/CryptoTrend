@file:OptIn(ExperimentalGlideComposeApi::class)

package br.com.terras.app.coins.detail.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.SegmentedControl
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.theme.DSMTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@Composable
fun DetailActivityCompose(
    id: String, navHostController: NavHostController, viewModel: DetailViewModel = hiltViewModel()
) {

    var showDialog by remember { mutableStateOf(true) }

    val coinDetailState by viewModel.detail.collectAsState()
    val chartDetailState by viewModel.chart.collectAsState()

    LaunchedEffect(true) {
        viewModel.getDetailById(id)
        viewModel.getChartById(id)
    }

    when (coinDetailState) {
        is Loading -> {}
        is SuccessDetail -> OnCoinDetailSuccess((coinDetailState as SuccessDetail),
            chartDetailState,
            {
                navHostController.popBackStack()
            }) {
            viewModel.getChartById(id, it)
        }

        is Error -> OnCoinDetailError(
            showDialog
        ) { showDialog = false }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnCoinDetailSuccess(
    state: SuccessDetail,
    chartDetailState: DetailViewModel.DetailState,
    onBackPress: () -> Unit,
    selectedDaysOfChart: (Int) -> Unit
) {

    BackHandler(onBack = onBackPress)
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlideImage(
                    model = state.coin.image,
                    contentDescription = state.coin.name,
                    Modifier
                        .padding(end = 2.dp)
                        .width(30.dp)
                        .height(30.dp)
                )

                Text(
                    text = state.coin.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back"
                    )
                }
            },
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.star_24dp),
                        contentDescription = "favorite"
                    )
                }
            })
    }) {
        Column(
            modifier = Modifier
                .padding(top = it.calculateTopPadding(), bottom = 100.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(27.dp)
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(
                        MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)
                    ),
            ) {
                val (name, date, valueAtDate, position, price, percentage, graph, bar) = createRefs()

                Text(text = state.coin.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.constrainAs(name) {
                        start.linkTo(parent.start, 18.dp)
                        top.linkTo(parent.top, 18.dp)
                    })

                Box(modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp)
                    )
                    .padding(start = 3.dp, end = 3.dp)
                    .constrainAs(position) {
                        start.linkTo(name.end, 3.dp)
                        centerVerticallyTo(name)
                    }) {
                    Text(
                        text = state.coin.marketCapRank,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                Text(text = state.coin.price,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.surface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.constrainAs(price) {
                        top.linkTo(parent.top, 18.dp)
                        end.linkTo(parent.end, 18.dp)
                    })

                Box(modifier = Modifier
                    .background(
                        state.coin.trendColor.color, RoundedCornerShape(9.dp)
                    )
                    .padding(start = 9.dp, end = 9.dp)
                    .constrainAs(percentage) {
                        top.linkTo(price.bottom)
                        end.linkTo(price.end)
                    }) {
                    Text(
                        text = state.coin.priceChangePercentageList.first().priceChangeValue,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.surface,
                        textAlign = TextAlign.Center
                    )
                }

                when (chartDetailState) {
                    is SuccessChart -> {
                        Text(text = chartDetailState.chart.chartData.first().date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.constrainAs(date) {
                                start.linkTo(name.start)
                                top.linkTo(name.bottom)
                            })

                        Text(text = chartDetailState.chart.chartData.first().yLabel.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.constrainAs(valueAtDate) {
                                start.linkTo(date.start)
                                top.linkTo(date.bottom)
                            })

                        LineChartCompose(
                            modifier = Modifier.constrainAs(graph) {
                                start.linkTo(parent.start)
                                top.linkTo(percentage.bottom, 18.dp)
                                bottom.linkTo(bar.top, 9.dp)
                                end.linkTo(parent.end)
                                height = Dimension.fillToConstraints
                                width = Dimension.matchParent
                            },
                            data = chartDetailState.chart.chartData,
                            graphColor = MaterialTheme.colorScheme.scrim,
                            showDashedLine = true,
                            showYLabels = true
                        )

                        SegmentedControl(modifier = Modifier
                            .height(30.dp)
                            .constrainAs(bar) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                top.linkTo(graph.bottom, 9.dp)
                                bottom.linkTo(parent.bottom, 9.dp)
                            }, items = DaysOfChart.entries.map { day -> day.text }) { index ->
                            selectedDaysOfChart(DaysOfChart.entries[index].value)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)
                    ),
            ) {
                state.coin.priceChangePercentageList.forEachIndexed { index, item ->
                    SectionContentItem(
                        showPaddingStart = index == 0,
                        showPaddingEnd = index == state.coin.priceChangePercentageList.lastIndex,
                        item = item
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(18.dp)
                    )
            ) {
                state.coin.marketDataList.forEachIndexed { index, pair ->
                    SectionInfoItem(
                        name = pair.first,
                        value = pair.second,
                        showDivider = index != state.coin.marketDataList.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionContentItem(showPaddingStart: Boolean, showPaddingEnd: Boolean, item: CoinDetailPricePercentageVO) {
    val paddingStart = if (showPaddingStart) 18.dp else 0.dp
    val paddingEnd = if (showPaddingEnd) 18.dp else 0.dp
    Column(
        modifier = Modifier
            .padding(start = paddingStart, end = paddingEnd, top = 18.dp, bottom = 18.dp)
            .width(IntrinsicSize.Min)
    ) {
        Text(
            text = item.priceChangeTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .alpha(.2f)
                .fillMaxWidth()
        )
        Text(
            text = item.priceChangeValue,
            style = MaterialTheme.typography.labelMedium,
            color = item.trendColor.color,
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
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.surface,
        )

        Spacer(modifier = Modifier.size(0.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.surface,
        )
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .alpha(.2f),
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}


@Composable
private fun OnCoinDetailError(
    showDialog: Boolean, onDismissRequest: () -> Unit
) {
    if (showDialog.not()) return

    AlertDialog(onDismissRequest = onDismissRequest,
        title = { Text(text = "Alerta") },
        text = { Text(text = "Erro ao carregar os dados, tente novamente mais tarde.") },
        confirmButton = {
            RippleButton(text = "Entendi", onClick = onDismissRequest)
        })
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
            ), {}) {}
        }
    }
}
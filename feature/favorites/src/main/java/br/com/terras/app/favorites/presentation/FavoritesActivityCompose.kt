package br.com.terras.app.favorites.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.CoinListItemShimmerRow
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.MarketSortBar
import br.com.terras.app.dsm.ui.component.MarketSortState
import br.com.terras.app.dsm.ui.component.PullToRefreshWrapper
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.component.sortWithMarketState
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.favorites.domain.model.CoinVO
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesIntent
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Error
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Loading
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Success

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FavoriteActivityCompose(
    viewModel: FavoritesViewModel = hiltViewModel(),
    showTopBar: Boolean = true,
    onItemClick: (String) -> Unit
) {
    val coinsListState by viewModel.coins.collectAsState()
    var showDialog by remember(coinsListState) { mutableStateOf(coinsListState is Error) }

    if (showTopBar) {
        Scaffold(
            topBar = {
                StickyHeaderView()
            }
        ) { innerPadding ->
            FavoritesContentBody(
                coinsListState = coinsListState,
                topPadding = innerPadding.calculateTopPadding(),
                showDialog = showDialog,
                onDismissError = { showDialog = false },
                onRefresh = { viewModel.handleIntent(FavoritesIntent.Refresh) },
                onItemClick = onItemClick
            )
        }
    } else {
        FavoritesContentBody(
            coinsListState = coinsListState,
            topPadding = 0.dp,
            showDialog = showDialog,
            onDismissError = { showDialog = false },
            onRefresh = { viewModel.handleIntent(FavoritesIntent.Refresh) },
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun FavoritesContentBody(
    coinsListState: FavoritesViewModel.FavoritesState,
    topPadding: Dp,
    showDialog: Boolean,
    onDismissError: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (String) -> Unit
) {
    var sortState by remember { mutableStateOf(MarketSortState()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
    ) {
        MarketSortBar(
            sortState = sortState,
            onSortClick = { column ->
                sortState = sortState.toggle(column)
            }
        )

        PullToRefreshWrapper(
            isRefreshing = false,
            onRefresh = onRefresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            indicatorTestTag = "favorites_pull_to_refresh"
        ) {
            when (coinsListState) {
                is Loading -> OnFavoritesLoading()
                is Success -> {
                    val sortedCoins = remember(coinsListState.coinsList, sortState) {
                        coinsListState.coinsList.sortWithMarketState(
                            sortState = sortState,
                            nameSelector = { it.symbol.ifBlank { it.name } },
                            volumeSelector = { it.rawVolume },
                            priceSelector = { it.rawPrice },
                            change24hSelector = { it.rawPriceChangePercentage }
                        )
                    }
                    if (sortedCoins.isEmpty()) {
                        OnFavoritesEmpty()
                    } else {
                        OnCoinsListSuccess(
                            coinsList = sortedCoins,
                            onItemClick = onItemClick
                        )
                    }
                }
                is Error -> {
                    OnFavoritesEmpty()
                    OnCoinsListError(
                        showDialog = showDialog,
                        onDismissRequest = onDismissError
                    )
                }
            }
        }
    }
}

@Composable
private fun OnFavoritesLoading() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(8) {
            CoinListItemShimmerRow()
        }
    }
}

@Composable
private fun OnFavoritesEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.star_24dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inversePrimary,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 12.dp)
        )
        Text(
            text = "Nenhuma criptomoeda favoritada no momento.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnCoinsListSuccess(
    coinsList: List<CoinVO>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = 112.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(coinsList.size) {
            coinsList[it].run {
                CoinListItemView(
                    coinId = id,
                    coinPosition = marketCapRank,
                    coinLogo = image,
                    coinName = name,
                    coinSymbol = symbol,
                    coinPrice = price,
                    coinPriceChancePercentage = priceChangePercentage,
                    trendColor = trendColor,
                    coinVolume = volume,
                    sparklinePrices = sparklinePrices,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun OnCoinsListError(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showDialog.not()) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        title = { Text(text = "Alerta") },
        text = { Text(text = "Erro ao carregar os dados, tente novamente mais tarde.") },
        confirmButton = {
            RippleButton(text = "Entendi", onClick = onDismissRequest)
        }
    )
}

@ThemePreviews
@Composable
private fun FavoriteActivityComposePreview() {
    DSMTheme {
        Surface {
            FavoriteActivityCompose {}
        }
    }
}
package br.com.terras.app.favorites.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
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
    val coinsListState by viewModel.coins.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    if (showTopBar) {
        Scaffold(
            topBar = {
                StickyHeaderView()
            }
        ) { innerPadding ->
            FavoritesContentBody(
                coinsListState = coinsListState,
                isRefreshing = isRefreshing,
                topPadding = innerPadding.calculateTopPadding(),
                onRefresh = { viewModel.handleIntent(FavoritesIntent.Refresh) },
                onItemClick = onItemClick
            )
        }
    } else {
        FavoritesContentBody(
            coinsListState = coinsListState,
            isRefreshing = isRefreshing,
            topPadding = 0.dp,
            onRefresh = { viewModel.handleIntent(FavoritesIntent.Refresh) },
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun FavoritesContentBody(
    coinsListState: FavoritesViewModel.FavoritesState,
    isRefreshing: Boolean,
    topPadding: Dp,
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
            isRefreshing = isRefreshing,
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
                is Error -> OnFavoritesEmpty()
            }
        }
    }
}

@Composable
private fun OnFavoritesLoading() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("favorites_list_skeleton")
    ) {
        items(8) { index ->
            CoinListItemShimmerRow(variantIndex = index)
        }
    }
}

@Composable
private fun OnFavoritesEmpty() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.favorites_empty_avatar)
    )
    val watermarkBaseArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val watermarkAccentArgb = MaterialTheme.colorScheme.scrim.toArgb()
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = watermarkBaseArgb,
            keyPath = arrayOf("**", "AvatarFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = watermarkBaseArgb,
            keyPath = arrayOf("**", "AvatarSoftFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = watermarkBaseArgb,
            keyPath = arrayOf("**", "AvatarStroke")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = watermarkBaseArgb,
            keyPath = arrayOf("**", "AvatarStroke")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = watermarkAccentArgb,
            keyPath = arrayOf("**", "AccentFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = watermarkAccentArgb,
            keyPath = arrayOf("**", "AccentStroke")
        )
    )
    val emptyContentDescription = stringResource(R.string.favorites_empty_avatar_cd)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = emptyContentDescription
            }
            .testTag("favorites_empty_watermark"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 0.85f,
                dynamicProperties = dynamicProperties,
                modifier = Modifier
                    .size(216.dp)
                    .alpha(0.28f)
                    .testTag("favorites_empty_lottie")
            )
            Text(
                text = stringResource(R.string.favorites_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                textAlign = TextAlign.Center
            )
        }
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



@ThemePreviews
@Composable
private fun FavoriteActivityComposePreview() {
    DSMTheme {
        Surface {
            FavoriteActivityCompose {}
        }
    }
}
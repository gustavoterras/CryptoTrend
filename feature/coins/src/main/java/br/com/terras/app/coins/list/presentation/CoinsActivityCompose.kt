package br.com.terras.app.coins.list.presentation

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsIntent
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.EmptySearch
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Error
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Loading
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Success
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.AnimatedSearchClearButton
import br.com.terras.app.dsm.ui.component.AnimatedSearchFab
import br.com.terras.app.dsm.ui.component.CoinListItemShimmerRow
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.MarketSortBar
import br.com.terras.app.dsm.ui.component.MarketSortState
import br.com.terras.app.dsm.ui.component.PullToRefreshWrapper
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.component.sortWithMarketState
import br.com.terras.app.dsm.ui.theme.DSMTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CoinsActivityCompose(
    viewModel: CoinsViewModel = hiltViewModel(),
    favoritesContent: @Composable () -> Unit = {},
    onItemClick: (String) -> Unit
) {
    val coinsListState by viewModel.coins.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val trendingListState by viewModel.trendingCoins.collectAsState()
    val trendingSearchQuery by viewModel.trendingSearchQuery.collectAsState()
    val isTrendingRefreshing by viewModel.isTrendingRefreshing.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var cryptoSortState by remember { mutableStateOf(MarketSortState()) }
    var trendingSortState by remember { mutableStateOf(MarketSortState()) }
    var isCryptoSearchOpen by rememberSaveable { mutableStateOf(false) }
    var isTrendingSearchOpen by rememberSaveable { mutableStateOf(false) }
    var showCryptoDialog by remember(coinsListState) { mutableStateOf(coinsListState is Error) }
    var showTrendingDialog by remember(trendingListState) { mutableStateOf(trendingListState is Error) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isCurrentSearchOpen = when (selectedTabIndex) {
        1 -> isCryptoSearchOpen
        2 -> isTrendingSearchOpen
        else -> false
    }

    BackHandler(enabled = isCurrentSearchOpen) {
        if (selectedTabIndex == 1) {
            isCryptoSearchOpen = false
            keyboardController?.hide()
            viewModel.handleIntent(CoinsIntent.ClearSearch)
        } else if (selectedTabIndex == 2) {
            isTrendingSearchOpen = false
            keyboardController?.hide()
            viewModel.handleIntent(CoinsIntent.ClearTrendingSearch)
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 2) {
            viewModel.handleIntent(CoinsIntent.LoadTrendingCoins)
        }
    }

    LaunchedEffect(isCurrentSearchOpen, selectedTabIndex) {
        if ((selectedTabIndex == 1 || selectedTabIndex == 2) && isCurrentSearchOpen) {
            focusRequester.requestFocus()
        }
    }

    val marketTabs = listOf(
        "Favoritos" to R.drawable.star_24dp,
        "Cripto" to R.drawable.ic_show_chart_24dp,
        "Tendências" to R.drawable.ic_trending_up_24dp
    )

    Scaffold(
        topBar = {
            StickyHeaderView()
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTabIndex == 1 || selectedTabIndex == 2,
                enter = fadeIn(animationSpec = tween(200)) +
                    scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                exit = fadeOut(animationSpec = tween(150)) +
                    scaleOut(targetScale = 0.7f, animationSpec = tween(150))
            ) {
                AnimatedSearchFab(
                    isSearchOpen = isCurrentSearchOpen,
                    onClick = {
                        if (selectedTabIndex == 1) {
                            if (isCryptoSearchOpen) {
                                isCryptoSearchOpen = false
                                keyboardController?.hide()
                                viewModel.handleIntent(CoinsIntent.ClearSearch)
                            } else {
                                isCryptoSearchOpen = true
                            }
                        } else {
                            if (isTrendingSearchOpen) {
                                isTrendingSearchOpen = false
                                keyboardController?.hide()
                                viewModel.handleIntent(CoinsIntent.ClearTrendingSearch)
                            } else {
                                isTrendingSearchOpen = true
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 96.dp),
                    testTag = "market_search_fab"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.scrim
                        )
                    }
                },
                divider = {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("markets_tab_bar")
            ) {
                marketTabs.forEachIndexed { index, (title, iconRes) ->
                    val isSelected = selectedTabIndex == index
                    val tabTag = when (index) {
                        0 -> "markets_tab_favorites"
                        1 -> "markets_tab_crypto"
                        else -> "markets_tab_trending"
                    }
                    Tab(
                        selected = isSelected,
                        onClick = {
                            selectedTabIndex = index
                            if (index == 0) {
                                keyboardController?.hide()
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.scrim,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(tabTag),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        favoritesContent()
                    }
                }
                1 -> {
                    MarketCoinsTabContent(
                        coinsListState = coinsListState,
                        searchQuery = searchQuery,
                        isRefreshing = isRefreshing,
                        isSearchOpen = isCryptoSearchOpen,
                        sortState = cryptoSortState,
                        showErrorDialog = showCryptoDialog,
                        focusRequester = focusRequester,
                        pullToRefreshTestTag = "market_pull_to_refresh",
                        onSortClick = { column -> cryptoSortState = cryptoSortState.toggle(column) },
                        onSearchQueryChange = { query ->
                            viewModel.handleIntent(CoinsIntent.SearchQueryChanged(query))
                        },
                        onSearchExecute = {
                            viewModel.handleIntent(CoinsIntent.ExecuteSearch(searchQuery))
                            keyboardController?.hide()
                        },
                        onSearchClearOrClose = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.handleIntent(CoinsIntent.ClearSearch)
                            } else {
                                isCryptoSearchOpen = false
                                keyboardController?.hide()
                                viewModel.handleIntent(CoinsIntent.ClearSearch)
                            }
                        },
                        onRefresh = { viewModel.handleIntent(CoinsIntent.Refresh) },
                        onDismissError = { showCryptoDialog = false },
                        favoriteIds = favoriteIds,
                        onItemClick = onItemClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
                else -> {
                    MarketCoinsTabContent(
                        coinsListState = trendingListState,
                        searchQuery = trendingSearchQuery,
                        isRefreshing = isTrendingRefreshing,
                        isSearchOpen = isTrendingSearchOpen,
                        sortState = trendingSortState,
                        showErrorDialog = showTrendingDialog,
                        focusRequester = focusRequester,
                        pullToRefreshTestTag = "trending_pull_to_refresh",
                        onSortClick = { column -> trendingSortState = trendingSortState.toggle(column) },
                        onSearchQueryChange = { query ->
                            viewModel.handleIntent(CoinsIntent.TrendingSearchQueryChanged(query))
                        },
                        onSearchExecute = {
                            viewModel.handleIntent(CoinsIntent.ExecuteTrendingSearch(trendingSearchQuery))
                            keyboardController?.hide()
                        },
                        onSearchClearOrClose = {
                            if (trendingSearchQuery.isNotEmpty()) {
                                viewModel.handleIntent(CoinsIntent.ClearTrendingSearch)
                            } else {
                                isTrendingSearchOpen = false
                                keyboardController?.hide()
                                viewModel.handleIntent(CoinsIntent.ClearTrendingSearch)
                            }
                        },
                        onRefresh = { viewModel.handleIntent(CoinsIntent.RefreshTrending) },
                        onDismissError = { showTrendingDialog = false },
                        favoriteIds = favoriteIds,
                        onItemClick = onItemClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketCoinsTabContent(
    coinsListState: CoinsViewModel.CoinsState,
    searchQuery: String,
    isRefreshing: Boolean,
    isSearchOpen: Boolean,
    sortState: MarketSortState,
    showErrorDialog: Boolean,
    focusRequester: FocusRequester,
    pullToRefreshTestTag: String,
    onSortClick: (br.com.terras.app.dsm.ui.component.MarketSortColumn) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchExecute: () -> Unit,
    onSearchClearOrClose: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    favoriteIds: Set<String> = emptySet(),
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(180)) +
                fadeOut(animationSpec = tween(150))
        ) {
            val hasQuery = searchQuery.isNotEmpty()
            val leadingIconTint by animateColorAsState(
                targetValue = if (hasQuery) {
                    MaterialTheme.colorScheme.scrim
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(220),
                label = "searchLeadingTint"
            )
            val leadingIconScale by animateFloatAsState(
                targetValue = if (hasQuery) 1.12f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "searchLeadingScale"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = searchQuery,
                    singleLine = true,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("market_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.surface,
                        unfocusedTextColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.scrim
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.search_24dp),
                            contentDescription = "Pesquisar",
                            tint = leadingIconTint,
                            modifier = Modifier.graphicsLayer {
                                scaleX = leadingIconScale
                                scaleY = leadingIconScale
                            }
                        )
                    },
                    trailingIcon = {
                        AnimatedSearchClearButton(
                            hasQuery = hasQuery,
                            onClick = onSearchClearOrClose,
                            testTag = "market_search_clear_button"
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Procurar criptomoedas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearchExecute() }
                    )
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        MarketSortBar(
            sortState = sortState,
            onSortClick = onSortClick
        )

        PullToRefreshWrapper(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            indicatorTestTag = pullToRefreshTestTag
        ) {
            when (coinsListState) {
                is Loading -> OnCoinsListLoading()
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
                    OnCoinsListSuccess(
                        coinsList = sortedCoins,
                        isFromSearchApi = coinsListState.isFromSearchApi,
                        favoriteIds = favoriteIds,
                        onItemClick = onItemClick
                    )
                }
                is EmptySearch -> OnCoinsSearchEmpty(searchQuery)
                is Error -> {
                    OnCoinsSearchEmpty(searchQuery)
                    OnCoinsListError(
                        showDialog = showErrorDialog,
                        onDismissRequest = onDismissError
                    )
                }
            }
        }
    }
}

@Composable
private fun OnCoinsListLoading() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(10) {
            CoinListItemShimmerRow()
        }
    }
}

@Composable
private fun OnCoinsListSuccess(
    coinsList: List<CoinVO>,
    isFromSearchApi: Boolean,
    favoriteIds: Set<String>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (isFromSearchApi) {
            item {
                Text(
                    text = "Resultados encontrados na API de pesquisa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }
        }
        itemsIndexed(coinsList, key = { index, coin -> "${coin.id}_$index" }) { _, coin ->
            CoinListItemView(
                coinId = coin.id,
                coinPosition = coin.marketCapRank,
                coinLogo = coin.image,
                coinName = coin.name,
                coinSymbol = coin.symbol,
                coinPrice = coin.price,
                coinPriceChancePercentage = coin.priceChangePercentage,
                trendColor = coin.trendColor,
                coinVolume = coin.volume,
                sparklinePrices = coin.sparklinePrices,
                isFavorite = coin.id in favoriteIds,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun OnCoinsSearchEmpty(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (query.isNotBlank()) {
                "Nenhuma criptomoeda encontrada para \"$query\"."
            } else {
                "Nenhum resultado encontrado."
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center
        )
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
private fun CoinsActivityComposePreview() {
    DSMTheme {
        Surface {
            CoinsActivityCompose {}
        }
    }
}
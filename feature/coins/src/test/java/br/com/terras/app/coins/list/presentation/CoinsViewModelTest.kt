package br.com.terras.app.coins.list.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.domain.CoinsUseCase
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsIntent
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.EmptySearch
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Error
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Loading
import br.com.terras.app.coins.list.presentation.CoinsViewModel.CoinsState.Success
import br.com.terras.app.coins.list.presentation.CoinsViewModel.GlobalMarketState
import br.com.terras.app.coins.mockCoinsVOListStub
import br.com.terras.app.coins.mockGlobalMarketVOStub
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.database.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.common.refresh.DataRefreshCoordinator
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO as SearchCoinVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private class FakeCoinsFavoriteLocalRepository(
    private val idsFlow: Flow<List<String>>
) : FavoriteCoinsLocalRepository {
    override fun observeFavoriteIds(): Flow<List<String>> = idsFlow
    override fun observeIsFavorite(coinId: String): Flow<Boolean> = flowOf(false)
    override suspend fun toggleFavorite(coinId: String): Boolean = false
    override suspend fun addFavorite(coinId: String) = Unit
    override suspend fun removeFavorite(coinId: String) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoinsViewModelTest {
    private val userCase: CoinsUseCase = mockk(relaxed = true)
    private val searchUseCase: SearchUseCase = mockk(relaxed = true)
    private val feedsUseCase: FeedsUseCase = mockk(relaxed = true)
    private val favoriteIdsFlow = MutableStateFlow<List<String>>(emptyList())
    private lateinit var favoriteLocalRepository: FavoriteCoinsLocalRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator

    private lateinit var viewModel: CoinsViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        favoriteLocalRepository = FakeCoinsFavoriteLocalRepository(favoriteIdsFlow)
        refreshCoordinator = DataRefreshCoordinator()
        coEvery { userCase.getCachedGlobalMarketData() } returns null
        coEvery { userCase.getGlobalMarketData(any()) } returns Result.failure(IllegalStateException("no global stub"))
        viewModel = CoinsViewModel(
            userCase,
            searchUseCase,
            feedsUseCase,
            refreshCoordinator,
            favoriteLocalRepository
        )
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(Loading, viewModel.coins.value)
    }

    @Test
    fun `WHEN getCoins THEN return error`() = runBlocking {
        coEvery {
            userCase.getCoins()
        } returns Result.failure(IllegalAccessException())

        viewModel.getCoins()

        coVerify {
            userCase.getCoins()
        }

        viewModel.coins.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN get feeds THEN return success`() = runBlocking {
        val result = mockCoinsVOListStub

        coEvery {
            userCase.getCoins()
        } returns Result.success(result)

        viewModel.getCoins()

        coVerify {
            userCase.getCoins()
        }

        viewModel.coins.test {
            Assert.assertEquals(Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN search matches local list THEN filter locally without calling SearchUseCase`() = runBlocking {
        val result = mockCoinsVOListStub
        coEvery { userCase.getCoins() } returns Result.success(result)
        viewModel.getCoins()

        val firstCoinName = result.first().name
        viewModel.handleIntent(CoinsIntent.SearchQueryChanged(firstCoinName))

        Assert.assertEquals(Success(listOf(result.first()), isFromSearchApi = false), viewModel.coins.value)
        coVerify(exactly = 0) { searchUseCase.searchCoins(any()) }
    }

    @Test
    fun `WHEN search does not match local list THEN fallback to SearchUseCase API`() = runBlocking {
        coEvery { userCase.getCoins() } returns Result.success(mockCoinsVOListStub)
        viewModel.getCoins()

        val apiCoin = SearchCoinVO(id = "pepe", symbol = "PEPE", name = "Pepe", thumb = "https://img/pepe.png")
        coEvery { searchUseCase.searchCoins("pepe") } returns Result.success(listOf(apiCoin))

        viewModel.handleIntent(CoinsIntent.ExecuteSearch("pepe"))

        coVerify(exactly = 1) { searchUseCase.searchCoins("pepe") }
        val expected = listOf(
            CoinVO(
                id = "pepe",
                symbol = "PEPE",
                name = "Pepe",
                image = "https://img/pepe.png",
                marketCapRank = "",
                price = "",
                priceChangePercentage = "",
                trendColor = TrendColor.NONE
            )
        )
        Assert.assertEquals(Success(expected, isFromSearchApi = true), viewModel.coins.value)
    }

    @Test
    fun `WHEN API fallback returns empty THEN emit EmptySearch`() = runBlocking {
        coEvery { userCase.getCoins() } returns Result.success(mockCoinsVOListStub)
        viewModel.getCoins()

        coEvery { searchUseCase.searchCoins("unknown_coin_xyz") } returns Result.success(emptyList())

        viewModel.handleIntent(CoinsIntent.ExecuteSearch("unknown_coin_xyz"))

        Assert.assertEquals(EmptySearch, viewModel.coins.value)
    }

    @Test
    fun `WHEN Refresh intent THEN refresh coin prices and news feeds and stop refreshing`() = runBlocking {
        val updatedCoins = mockCoinsVOListStub
        coEvery { userCase.getCoins(page = 1, forceRefresh = true) } returns Result.success(updatedCoins)
        coEvery { feedsUseCase.getFeeds("bitcoin", forceRefresh = true) } returns Result.success(emptyList())

        refreshCoordinator.refreshEvents.test {
            viewModel.handleIntent(CoinsIntent.Refresh)

            Assert.assertEquals(Unit, awaitItem())
            coVerify(atLeast = 1) { userCase.getCoins(page = 1, forceRefresh = true) }
            coVerify(exactly = 1) { feedsUseCase.getFeeds("bitcoin", forceRefresh = true) }
            Assert.assertEquals(Success(updatedCoins), viewModel.coins.value)
            Assert.assertFalse(viewModel.isRefreshing.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN LoadTrendingCoins THEN loads once and returns success`() = runBlocking {
        val result = mockCoinsVOListStub
        coEvery { userCase.getTrendingCoins() } returns Result.success(result)

        viewModel.handleIntent(CoinsIntent.LoadTrendingCoins)
        viewModel.handleIntent(CoinsIntent.LoadTrendingCoins)

        coVerify(exactly = 1) { userCase.getTrendingCoins() }
        Assert.assertEquals(Success(result), viewModel.trendingCoins.value)
    }

    @Test
    fun `WHEN getTrendingCoins fails THEN return error`() = runBlocking {
        coEvery { userCase.getTrendingCoins() } returns Result.failure(IllegalStateException())

        viewModel.getTrendingCoins()

        coVerify { userCase.getTrendingCoins() }
        Assert.assertEquals(Error, viewModel.trendingCoins.value)
    }

    @Test
    fun `WHEN trending search matches local list THEN filter locally without calling SearchUseCase`() = runBlocking {
        val result = mockCoinsVOListStub
        coEvery { userCase.getTrendingCoins() } returns Result.success(result)
        viewModel.getTrendingCoins()

        viewModel.handleIntent(CoinsIntent.TrendingSearchQueryChanged(result.first().name))

        Assert.assertEquals(Success(listOf(result.first()), isFromSearchApi = false), viewModel.trendingCoins.value)
        coVerify(exactly = 0) { searchUseCase.searchCoins(any()) }
    }

    @Test
    fun `WHEN trending search does not match local list THEN fallback to SearchUseCase API`() = runBlocking {
        coEvery { userCase.getTrendingCoins() } returns Result.success(mockCoinsVOListStub)
        viewModel.getTrendingCoins()

        val apiCoin = SearchCoinVO(id = "pepe", symbol = "PEPE", name = "Pepe", thumb = "https://img/pepe.png")
        coEvery { searchUseCase.searchCoins("pepe") } returns Result.success(listOf(apiCoin))

        viewModel.handleIntent(CoinsIntent.ExecuteTrendingSearch("pepe"))

        coVerify(exactly = 1) { searchUseCase.searchCoins("pepe") }
        val expected = listOf(
            CoinVO(
                id = "pepe",
                symbol = "PEPE",
                name = "Pepe",
                image = "https://img/pepe.png",
                marketCapRank = "",
                price = "",
                priceChangePercentage = "",
                trendColor = TrendColor.NONE
            )
        )
        Assert.assertEquals(Success(expected, isFromSearchApi = true), viewModel.trendingCoins.value)
    }

    @Test
    fun `WHEN RefreshTrending intent THEN refresh trending coins and feeds and stop refreshing`() = runBlocking {
        val updatedCoins = mockCoinsVOListStub
        coEvery { userCase.getTrendingCoins(page = 1, forceRefresh = true) } returns Result.success(updatedCoins)
        coEvery { feedsUseCase.getFeeds("bitcoin", forceRefresh = true) } returns Result.success(emptyList())

        refreshCoordinator.refreshEvents.test {
            viewModel.handleIntent(CoinsIntent.RefreshTrending)

            Assert.assertEquals(Unit, awaitItem())
            coVerify(atLeast = 1) { userCase.getTrendingCoins(page = 1, forceRefresh = true) }
            coVerify(exactly = 1) { feedsUseCase.getFeeds("bitcoin", forceRefresh = true) }
            Assert.assertEquals(Success(updatedCoins), viewModel.trendingCoins.value)
            Assert.assertFalse(viewModel.isTrendingRefreshing.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN favorite IDs emit THEN updates favoriteIds StateFlow`() = runBlocking {
        viewModel.favoriteIds.test {
            Assert.assertEquals(emptySet<String>(), awaitItem())
            favoriteIdsFlow.value = listOf("bitcoin", "solana")
            Assert.assertEquals(setOf("bitcoin", "solana"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN LoadMoreCoins intent THEN appends deduplicated page and stops when page smaller than limit`() = runBlocking {
        val page1 = (1..CoinsUseCase.CRYPTO_PAGE_SIZE).map { idx ->
            CoinVO(
                id = "coin-$idx",
                symbol = "C$idx",
                name = "Coin $idx",
                image = "",
                marketCapRank = idx.toString(),
                price = "$1.00",
                priceChangePercentage = "1.0%",
                trendColor = TrendColor.UP
            )
        }
        val page2 = listOf(
            page1.last(),
            CoinVO(
                id = "coin-21",
                symbol = "C21",
                name = "Coin 21",
                image = "",
                marketCapRank = "21",
                price = "$2.00",
                priceChangePercentage = "2.0%",
                trendColor = TrendColor.UP
            )
        )

        coEvery { userCase.getCoins(page = 1) } returns Result.success(page1)
        coEvery { userCase.getCoins(page = 2) } returns Result.success(page2)

        viewModel.getCoins()
        Assert.assertEquals(
            Success(coinsList = page1, isLoadingMore = false, hasMorePages = true),
            viewModel.coins.value
        )

        viewModel.handleIntent(CoinsIntent.LoadMoreCoins)
        coVerify(exactly = 1) { userCase.getCoins(page = 2) }
        Assert.assertEquals(
            Success(coinsList = page1 + page2.last(), isLoadingMore = false, hasMorePages = false),
            viewModel.coins.value
        )

        viewModel.handleIntent(CoinsIntent.LoadMoreCoins)
        coVerify(exactly = 1) { userCase.getCoins(page = 2) }
    }

    @Test
    fun `WHEN LoadMoreTrendingCoins intent THEN appends deduplicated trending page and stops at end`() = runBlocking {
        val page1 = (1..CoinsUseCase.TRENDING_PAGE_SIZE).map { idx ->
            CoinVO(
                id = "trend-$idx",
                symbol = "T$idx",
                name = "Trend $idx",
                image = "",
                marketCapRank = idx.toString(),
                price = "$1.00",
                priceChangePercentage = "1.0%",
                trendColor = TrendColor.UP
            )
        }
        val page2 = listOf(
            page1.first(),
            CoinVO(
                id = "trend-8",
                symbol = "T8",
                name = "Trend 8",
                image = "",
                marketCapRank = "8",
                price = "$2.00",
                priceChangePercentage = "2.0%",
                trendColor = TrendColor.UP
            )
        )

        coEvery { userCase.getTrendingCoins(page = 1) } returns Result.success(page1)
        coEvery { userCase.getTrendingCoins(page = 2) } returns Result.success(page2)

        viewModel.getTrendingCoins()
        Assert.assertEquals(
            Success(coinsList = page1, isLoadingMore = false, hasMorePages = true),
            viewModel.trendingCoins.value
        )

        viewModel.handleIntent(CoinsIntent.LoadMoreTrendingCoins)
        coVerify(exactly = 1) { userCase.getTrendingCoins(page = 2) }
        Assert.assertEquals(
            Success(coinsList = page1 + page2.last(), isLoadingMore = false, hasMorePages = false),
            viewModel.trendingCoins.value
        )
    }

    @Test
    fun `WHEN search query is active THEN LoadMoreCoins is ignored`() = runBlocking {
        val page1 = (1..CoinsUseCase.CRYPTO_PAGE_SIZE).map { idx ->
            CoinVO(
                id = "coin-$idx",
                symbol = "C$idx",
                name = "Coin $idx",
                image = "",
                marketCapRank = idx.toString(),
                price = "$1.00",
                priceChangePercentage = "1.0%",
                trendColor = TrendColor.UP
            )
        }
        coEvery { userCase.getCoins(page = 1) } returns Result.success(page1)
        viewModel.getCoins()

        viewModel.handleIntent(CoinsIntent.SearchQueryChanged("Coin 1"))
        viewModel.handleIntent(CoinsIntent.LoadMoreCoins)

        coVerify(exactly = 0) { userCase.getCoins(page = 2) }
    }

    @Test
    fun `WHEN LoadGlobalMarket succeeds THEN emits GlobalMarketState Success`() = runBlocking {
        coEvery { userCase.getGlobalMarketData() } returns Result.success(mockGlobalMarketVOStub)

        viewModel.handleIntent(CoinsIntent.LoadGlobalMarket)

        Assert.assertEquals(GlobalMarketState.Success(mockGlobalMarketVOStub), viewModel.globalMarket.value)
    }

    @Test
    fun `WHEN Refresh intent runs THEN refreshes global market data as well`() = runBlocking {
        coEvery { userCase.getCoins(page = 1, forceRefresh = true) } returns Result.success(mockCoinsVOListStub)
        coEvery { feedsUseCase.getFeeds("bitcoin", forceRefresh = true) } returns Result.success(emptyList())
        coEvery { userCase.getGlobalMarketData(forceRefresh = true) } returns Result.success(mockGlobalMarketVOStub)

        viewModel.handleIntent(CoinsIntent.Refresh)

        coVerify(atLeast = 1) { userCase.getGlobalMarketData(forceRefresh = true) }
        Assert.assertEquals(GlobalMarketState.Success(mockGlobalMarketVOStub), viewModel.globalMarket.value)
    }
}
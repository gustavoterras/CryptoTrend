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
import br.com.terras.app.coins.mockCoinsVOListStub
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.network.refresh.DataRefreshCoordinator
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO as SearchCoinVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoinsViewModelTest {
    private val userCase: CoinsUseCase = mockk(relaxed = true)
    private val searchUseCase: SearchUseCase = mockk(relaxed = true)
    private val feedsUseCase: FeedsUseCase = mockk(relaxed = true)
    private lateinit var refreshCoordinator: DataRefreshCoordinator

    private lateinit var viewModel: CoinsViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = CoinsViewModel(userCase, searchUseCase, feedsUseCase, refreshCoordinator)
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
        coEvery { userCase.getCoins() } returns Result.success(updatedCoins)
        coEvery { feedsUseCase.getFeeds("bitcoin") } returns Result.success(emptyList())

        refreshCoordinator.refreshEvents.test {
            viewModel.handleIntent(CoinsIntent.Refresh)

            Assert.assertEquals(Unit, awaitItem())
            coVerify(atLeast = 1) { userCase.getCoins() }
            coVerify(exactly = 1) { feedsUseCase.getFeeds("bitcoin") }
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
        coEvery { userCase.getTrendingCoins() } returns Result.success(updatedCoins)
        coEvery { feedsUseCase.getFeeds("bitcoin") } returns Result.success(emptyList())

        refreshCoordinator.refreshEvents.test {
            viewModel.handleIntent(CoinsIntent.RefreshTrending)

            Assert.assertEquals(Unit, awaitItem())
            coVerify(atLeast = 1) { userCase.getTrendingCoins() }
            coVerify(exactly = 1) { feedsUseCase.getFeeds("bitcoin") }
            Assert.assertEquals(Success(updatedCoins), viewModel.trendingCoins.value)
            Assert.assertFalse(viewModel.isTrendingRefreshing.value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
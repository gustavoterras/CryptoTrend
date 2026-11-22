package br.com.terras.app.coins.detail.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.detail.domain.CoinDetailPortfolioCalculator
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Error
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.InsightsError
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessInsights
import br.com.terras.app.coins.mockCoinChartDetailVOStub
import br.com.terras.app.coins.mockCoinDetailVOStub
import br.com.terras.app.coins.mockCoinInsightVOStub
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.network.portfolio.PortfolioLocalRepository
import br.com.terras.app.network.portfolio.PortfolioTransactionRecord
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private class FakeDetailFavoriteLocalRepository : FavoriteCoinsLocalRepository {
    val favoriteIds = MutableStateFlow<List<String>>(emptyList())
    val isFavoriteFlow = MutableStateFlow(false)
    val toggledIds = mutableListOf<String>()

    override fun observeFavoriteIds(): Flow<List<String>> = favoriteIds
    override fun observeIsFavorite(coinId: String): Flow<Boolean> = isFavoriteFlow
    override suspend fun toggleFavorite(coinId: String): Boolean {
        toggledIds += coinId
        val next = !isFavoriteFlow.value
        isFavoriteFlow.value = next
        return next
    }
    override suspend fun addFavorite(coinId: String) = Unit
    override suspend fun removeFavorite(coinId: String) = Unit
}

private class FakeDetailPortfolioLocalRepository : PortfolioLocalRepository {
    val transactionsFlow = MutableStateFlow<List<PortfolioTransactionRecord>>(emptyList())

    override fun observeTransactionsChronological(): Flow<List<PortfolioTransactionRecord>> = transactionsFlow
    override suspend fun getTransactionById(id: Long): PortfolioTransactionRecord? =
        transactionsFlow.value.firstOrNull { it.id == id }
    override suspend fun insertTransaction(record: PortfolioTransactionRecord): Long {
        val nextId = (transactionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1L
        transactionsFlow.value = transactionsFlow.value + record.copy(id = nextId)
        return nextId
    }
    override suspend fun updateTransaction(record: PortfolioTransactionRecord) {
        transactionsFlow.value = transactionsFlow.value.map { if (it.id == record.id) record else it }
    }
    override suspend fun deleteTransaction(id: Long) {
        transactionsFlow.value = transactionsFlow.value.filterNot { it.id == id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val userCase: DetailUseCase = mockk(relaxed = true)
    private val feedsUseCase: FeedsUseCase = mockk(relaxed = true)
    private lateinit var favoriteLocalRepository: FakeDetailFavoriteLocalRepository
    private lateinit var portfolioLocalRepository: FakeDetailPortfolioLocalRepository

    private lateinit var viewModel: DetailViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        favoriteLocalRepository = FakeDetailFavoriteLocalRepository()
        portfolioLocalRepository = FakeDetailPortfolioLocalRepository()
        viewModel = DetailViewModel(
            useCase = userCase,
            feedsUseCase = feedsUseCase,
            favoriteLocalRepository = favoriteLocalRepository,
            portfolioLocalRepository = portfolioLocalRepository,
            portfolioCalculator = CoinDetailPortfolioCalculator()
        )
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(Loading, viewModel.detail.value)
        Assert.assertEquals(Loading, viewModel.chart.value)
    }

    @Test
    fun `WHEN getDetailById THEN return error`() = runBlocking {
        val id = "btc"
        coEvery {
            userCase.getDetailById(id)
        } returns Result.failure(IllegalAccessException())

        viewModel.getDetailById(id)

        viewModel.detail.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN get getDetailById THEN return success`() = runBlocking {
        val id = "btc"
        val result = mockCoinDetailVOStub

        coEvery {
            userCase.getDetailById(id)
        } returns Result.success(result)

        viewModel.getDetailById(id)

        viewModel.detail.test {
            Assert.assertEquals(SuccessDetail(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN getChartDetailById THEN return error`() = runBlocking {
        val id = "btc"
        val days = DaysOfChart.DAY.value
        coEvery {
            userCase.getChartDetailById(id, days)
        } returns Result.failure(IllegalAccessException())

        viewModel.getChartById(id, days)

        viewModel.chart.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN get getChartDetailById THEN return success`() = runBlocking {
        val id = "btc"
        val days = DaysOfChart.DAY.value
        val result = mockCoinChartDetailVOStub

        coEvery {
            userCase.getChartDetailById(id, days)
        } returns Result.success(result)

        viewModel.getChartById(id, days)

        viewModel.chart.test {
            Assert.assertEquals(SuccessChart(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN getInsightsByCoinId THEN return success`() = runBlocking {
        val coinId = "bitcoin"
        val insights = listOf(mockCoinInsightVOStub)

        coEvery {
            userCase.getInsightsByCoinId(coinId)
        } returns Result.success(insights)

        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadInsights(coinId))

        viewModel.insights.test {
            Assert.assertEquals(SuccessInsights(insights), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN getInsightsByCoinId THEN return insights error`() = runBlocking {
        val coinId = "bitcoin"

        coEvery {
            userCase.getInsightsByCoinId(coinId)
        } returns Result.failure(IllegalStateException("Network failure"))

        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadInsights(coinId))

        viewModel.insights.test {
            val state = awaitItem()
            Assert.assertTrue(state is InsightsError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN ToggleFavorite intent THEN calls repository toggleFavorite and updates isFavorite`() = runBlocking {
        viewModel.isFavorite.test {
            Assert.assertFalse(awaitItem())
            viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleFavorite("bitcoin"))
            Assert.assertEquals(listOf("bitcoin"), favoriteLocalRepository.toggledIds)
            Assert.assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN coin has no portfolio transactions THEN portfolioInsight emits null`() = runBlocking {
        coEvery {
            userCase.getDetailById("bitcoin")
        } returns Result.success(mockCoinDetailVOStub.copy(rawPrice = 300000.0))

        viewModel.portfolioInsight.test {
            Assert.assertNull(awaitItem())
            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN coin has portfolio transactions THEN portfolioInsight emits pnl and markers`() = runBlocking {
        coEvery {
            userCase.getDetailById("bitcoin")
        } returns Result.success(mockCoinDetailVOStub.copy(rawPrice = 150.0))

        portfolioLocalRepository.transactionsFlow.value = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 2.0,
                unitPrice = 100.0,
                timestamp = 1_700_000_000_000L
            )
        )

        viewModel.portfolioInsight.test {
            Assert.assertNull(awaitItem())
            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
            val insight = expectMostRecentItem()
            Assert.assertNotNull(insight)
            Assert.assertEquals("BTC", insight!!.symbol)
            Assert.assertEquals(TrendColor.UP, insight.totalPnLTrend)
            Assert.assertEquals(1, insight.tradeMarkers.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
package br.com.terras.app.coins.detail.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.detail.domain.CoinDetailPortfolioCalculator
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.domain.model.OrderBookEntryVO
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplier
import br.com.terras.app.coins.detail.domain.model.OrderBookSnapshotVO
import br.com.terras.app.coins.detail.domain.model.OrderBookUiState
import br.com.terras.app.coins.detail.network.CoinWebSocketService
import br.com.terras.app.coins.detail.network.CoinbaseOrderBookService
import br.com.terras.app.coins.detail.network.LivePriceSource
import br.com.terras.app.coins.detail.network.LivePriceTick
import br.com.terras.app.coins.detail.network.OrderBookStreamEvent
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Error
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.InsightsError
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessInsights
import br.com.terras.app.coins.mockCoinChartDetailVOStub
import br.com.terras.app.coins.mockCoinDetailVOStub
import br.com.terras.app.coins.mockCoinInsightVOStub
import br.com.terras.app.common.formatLivePrice
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.database.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.database.portfolio.PortfolioLocalRepository
import br.com.terras.app.database.portfolio.PortfolioTransactionRecord
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

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

private class FakeCoinWebSocketService : CoinWebSocketService {
    val ticksFlow = MutableSharedFlow<LivePriceTick>(extraBufferCapacity = 16)
    var lastRequestedCoinId: String? = null
    var lastRequestedSymbol: String? = null
    var lastRequestedVsCurrency: String? = null

    override fun observeLivePrice(
        coinId: String,
        coinSymbol: String,
        vsCurrency: String
    ): Flow<LivePriceTick> {
        lastRequestedCoinId = coinId
        lastRequestedSymbol = coinSymbol
        lastRequestedVsCurrency = vsCurrency
        return ticksFlow
    }
}

private class FakeCoinbaseOrderBookService : CoinbaseOrderBookService {
    val eventsFlow = MutableSharedFlow<OrderBookStreamEvent>(replay = 1, extraBufferCapacity = 16)
    var lastRequestedSymbol: String? = null
    var lastMultiplierFlow: StateFlow<OrderBookMultiplier>? = null
    var pairAvailable: Boolean = true
    var availabilityCalls: Int = 0
    var pendingAvailability: CompletableDeferred<Boolean>? = null

    override suspend fun isPairAvailable(coinSymbol: String): Boolean {
        availabilityCalls++
        return pendingAvailability?.await() ?: pairAvailable
    }

    override fun observeOrderBook(
        coinSymbol: String,
        multiplier: StateFlow<OrderBookMultiplier>
    ): Flow<OrderBookStreamEvent> {
        lastRequestedSymbol = coinSymbol
        lastMultiplierFlow = multiplier
        return eventsFlow
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val userCase: DetailUseCase = mockk(relaxed = true)
    private val feedsUseCase: FeedsUseCase = mockk(relaxed = true)
    private lateinit var favoriteLocalRepository: FakeDetailFavoriteLocalRepository
    private lateinit var portfolioLocalRepository: FakeDetailPortfolioLocalRepository
    private lateinit var webSocketService: FakeCoinWebSocketService
    private lateinit var orderBookService: FakeCoinbaseOrderBookService

    private lateinit var viewModel: DetailViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        favoriteLocalRepository = FakeDetailFavoriteLocalRepository()
        portfolioLocalRepository = FakeDetailPortfolioLocalRepository()
        webSocketService = FakeCoinWebSocketService()
        orderBookService = FakeCoinbaseOrderBookService()
        coEvery { feedsUseCase.getFeeds(any()) } returns Result.success(emptyList())
        viewModel = DetailViewModel(
            useCase = userCase,
            feedsUseCase = feedsUseCase,
            favoriteLocalRepository = favoriteLocalRepository,
            portfolioLocalRepository = portfolioLocalRepository,
            webSocketService = webSocketService,
            orderBookService = orderBookService,
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

    @Test
    fun `WHEN Refresh intent succeeds THEN reloads detail chart insights news and updates timestamp`() = runBlocking {
        val updatedDetail = mockCoinDetailVOStub.copy(name = "Bitcoin", price = "R$ 550.000,00")
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(updatedDetail)
        coEvery { userCase.getChartDetailById("bitcoin", 7) } returns Result.success(mockCoinChartDetailVOStub)
        coEvery { userCase.getInsightsByCoinId("bitcoin") } returns Result.success(listOf(mockCoinInsightVOStub))
        coEvery { feedsUseCase.getFeeds("Bitcoin") } returns Result.success(emptyList())

        viewModel.handleIntent(DetailViewModel.DetailIntent.Refresh("bitcoin", days = 7))

        Assert.assertFalse(viewModel.isRefreshing.value)
        Assert.assertEquals(SuccessDetail(updatedDetail), viewModel.detail.value)
        Assert.assertEquals(SuccessChart(mockCoinChartDetailVOStub, days = 7), viewModel.chart.value)
        Assert.assertEquals(SuccessInsights(listOf(mockCoinInsightVOStub)), viewModel.insights.value)
        Assert.assertNotNull(viewModel.lastUpdatedAtMillis.value)
        Assert.assertNull(viewModel.refreshErrorMessage.value)
    }

    @Test
    fun `WHEN Refresh intent fails after SuccessDetail THEN keeps existing detail and emits refresh error`() = runBlocking {
        val initialDetail = mockCoinDetailVOStub.copy(name = "Bitcoin")
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(initialDetail)
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))

        coEvery { userCase.getDetailById("bitcoin") } returns Result.failure(RuntimeException("timeout"))
        coEvery { userCase.getChartDetailById("bitcoin", 1) } returns Result.failure(RuntimeException("timeout"))
        coEvery { userCase.getInsightsByCoinId("bitcoin") } returns Result.failure(RuntimeException("timeout"))

        viewModel.handleIntent(DetailViewModel.DetailIntent.Refresh("bitcoin", days = 1))

        Assert.assertFalse(viewModel.isRefreshing.value)
        Assert.assertEquals(SuccessDetail(initialDetail), viewModel.detail.value)
        Assert.assertNotNull(viewModel.refreshErrorMessage.value)

        viewModel.consumeRefreshErrorMessage()
        Assert.assertNull(viewModel.refreshErrorMessage.value)
    }

    @Test
    fun `WHEN ToggleRealTime enabled THEN validates tick updates price 24h change timestamp and chart`() = runBlocking {
        val initialDetail = mockCoinDetailVOStub.copy(
            rawPrice = 500.0,
            priceChangePercentageList = listOf(
                CoinDetailPricePercentageVO("1D", "+1,0%", TrendColor.UP)
            ),
            trendColor = TrendColor.UP
        )
        val initialChart = CoinChartDetailVO(
            chartData = listOf(
                DataPoint("May 21", 490.0, "10:00", "$490.00", timestampMillis = 1_700_000_000_000L)
            )
        )
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(initialDetail)
        coEvery { userCase.getChartDetailById("bitcoin", 1) } returns Result.success(initialChart)

        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadChart("bitcoin", 1))
        viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleRealTime(true, "bitcoin", "BTC"))

        Assert.assertTrue(viewModel.isRealTimeActive.value)
        val expectedVs = if (Locale.getDefault().language.equals("pt", ignoreCase = true)) "brl" else "usd"
        Assert.assertEquals("bitcoin", webSocketService.lastRequestedCoinId)
        Assert.assertEquals("BTC", webSocketService.lastRequestedSymbol)
        Assert.assertEquals(expectedVs, webSocketService.lastRequestedVsCurrency)

        val liveTimestamp = 1_700_000_060_000L
        webSocketService.ticksFlow.tryEmit(
            LivePriceTick(
                coinId = "bitcoin",
                vsCurrency = expectedVs,
                price = 525.75,
                change24h = -2.5,
                timestampMillis = liveTimestamp,
                source = LivePriceSource.WEBSOCKET
            )
        )

        val updatedDetail = (viewModel.detail.value as SuccessDetail).coin
        Assert.assertEquals(525.75, updatedDetail.rawPrice, 0.0001)
        Assert.assertEquals(525.75.formatLivePrice(), updatedDetail.price)
        Assert.assertEquals(TrendColor.DOWN, updatedDetail.trendColor)
        Assert.assertEquals(TrendColor.DOWN, updatedDetail.priceChangePercentageList.first().trendColor)
        Assert.assertEquals(liveTimestamp, viewModel.lastUpdatedAtMillis.value)

        val updatedChart = (viewModel.chart.value as SuccessChart).chart.chartData
        Assert.assertEquals(2, updatedChart.size)
        Assert.assertEquals(525.75, updatedChart.last().y, 0.0001)
        Assert.assertEquals(liveTimestamp, updatedChart.last().timestampMillis)

        viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleRealTime(false, "bitcoin"))
        Assert.assertFalse(viewModel.isRealTimeActive.value)
    }

    @Test
    fun `WHEN ToggleRealTime receives invalid or mismatched ticks THEN ignores them`() = runBlocking {
        val initialDetail = mockCoinDetailVOStub.copy(price = "$100.00", rawPrice = 100.0)
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(initialDetail)
        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
        viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleRealTime(true, "bitcoin"))

        val expectedVs = if (Locale.getDefault().language.equals("pt", ignoreCase = true)) "brl" else "usd"

        // Invalid NaN, <= 0, wrong coin, wrong currency
        webSocketService.ticksFlow.tryEmit(LivePriceTick("bitcoin", expectedVs, Double.NaN))
        webSocketService.ticksFlow.tryEmit(LivePriceTick("bitcoin", expectedVs, -10.0))
        webSocketService.ticksFlow.tryEmit(LivePriceTick("ethereum", expectedVs, 3000.0))
        webSocketService.ticksFlow.tryEmit(LivePriceTick("bitcoin", "eur", 90000.0))

        val detailAfter = (viewModel.detail.value as SuccessDetail).coin
        Assert.assertEquals(100.0, detailAfter.rawPrice, 0.0001)
        Assert.assertEquals("$100.00", detailAfter.price)
    }

    @Test
    fun `WHEN validateAndFormatLiveTick formats sub-dollar price THEN preserves small-cap precision`() {
        val tick = LivePriceTick(
            coinId = "shiba-inu",
            vsCurrency = "usd",
            price = 0.00001425,
            change24h = 4.2,
            timestampMillis = 1_700_000_000_000L
        )

        val validated = viewModel.validateAndFormatLiveTick(
            tick = tick,
            expectedCoinId = "shiba-inu",
            expectedVsCurrency = "usd",
            locale = Locale.US
        )

        Assert.assertNotNull(validated)
        Assert.assertEquals("$0.00001425", validated!!.formattedPrice)
        Assert.assertEquals("+4.2%", validated.formattedChange24h)
    }

    @Test
    fun `WHEN LoadChart intent is dispatched while RT active THEN disables RT mode`() = runBlocking {
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(mockCoinDetailVOStub)
        coEvery { userCase.getChartDetailById("bitcoin", 7) } returns Result.success(mockCoinChartDetailVOStub)

        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
        viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleRealTime(true, "bitcoin"))
        Assert.assertTrue(viewModel.isRealTimeActive.value)

        viewModel.handleIntent(DetailViewModel.DetailIntent.LoadChart("bitcoin", 7))
        Assert.assertFalse(viewModel.isRealTimeActive.value)
    }

    @Test
    fun `WHEN coin loads THEN orderBook starts Collapsed and shows Loading before Visible on expand`() = runBlocking {
        coEvery {
            userCase.getDetailById("bitcoin")
        } returns Result.success(mockCoinDetailVOStub.copy(symbol = "btc"))

        val sampleSnapshot = OrderBookSnapshotVO(
            pairSymbol = "BTC-USD",
            bids = listOf(OrderBookEntryVO(65000.0, "$65,000.00", 1.0, "1.000", 1.0, 1.0f)),
            asks = listOf(OrderBookEntryVO(65010.0, "$65,010.00", 0.5, "0.5000", 0.5, 0.5f)),
            spreadAbsolute = 10.0,
            spreadAbsoluteFormatted = "$10.00",
            spreadPercentageFormatted = "0.02%",
            isLive = true
        )

        viewModel.orderBookState.test {
            Assert.assertEquals(OrderBookUiState.Hidden, awaitItem())

            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))
            Assert.assertEquals(OrderBookUiState.Collapsed("BTC-USD"), awaitItem())

            viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleOrderBookExpanded(true))
            Assert.assertEquals(OrderBookUiState.Loading("BTC-USD"), awaitItem())
            Assert.assertEquals("BTC", orderBookService.lastRequestedSymbol)
            Assert.assertEquals(OrderBookMultiplier.DEFAULT, orderBookService.lastMultiplierFlow?.value)

            viewModel.handleIntent(DetailViewModel.DetailIntent.SelectOrderBookMultiplier(OrderBookMultiplier.X100))
            Assert.assertEquals(OrderBookMultiplier.X100, orderBookService.lastMultiplierFlow?.value)

            orderBookService.eventsFlow.tryEmit(OrderBookStreamEvent.Snapshot(sampleSnapshot))
            val visible = awaitItem()
            Assert.assertTrue(visible is OrderBookUiState.Visible)
            Assert.assertEquals("BTC-USD", (visible as OrderBookUiState.Visible).snapshot.pairSymbol)

            orderBookService.eventsFlow.tryEmit(OrderBookStreamEvent.Unavailable)
            Assert.assertEquals(OrderBookUiState.Hidden, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN pair is not available on Coinbase THEN orderBookState remains Hidden`() = runBlocking {
        orderBookService.pairAvailable = false
        coEvery {
            userCase.getDetailById("unlisted")
        } returns Result.success(mockCoinDetailVOStub.copy(symbol = "xyz"))

        viewModel.orderBookState.test {
            Assert.assertEquals(OrderBookUiState.Hidden, awaitItem())
            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("unlisted"))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN LoadDetail runs THEN detail remains Loading until sequential order book check completes`() = runBlocking {
        val availabilityGate = CompletableDeferred<Boolean>()
        orderBookService.pendingAvailability = availabilityGate
        val detailResult = mockCoinDetailVOStub.copy(symbol = "btc")
        coEvery { userCase.getDetailById("bitcoin") } returns Result.success(detailResult)

        viewModel.orderBookState.test {
            Assert.assertEquals(OrderBookUiState.Hidden, awaitItem())

            viewModel.handleIntent(DetailViewModel.DetailIntent.LoadDetail("bitcoin"))

            Assert.assertEquals(Loading, viewModel.detail.value)
            Assert.assertEquals(1, orderBookService.availabilityCalls)

            availabilityGate.complete(true)

            Assert.assertEquals(SuccessDetail(detailResult), viewModel.detail.value)
            Assert.assertEquals(OrderBookUiState.Collapsed("BTC-USD"), awaitItem())

            viewModel.handleIntent(DetailViewModel.DetailIntent.ToggleOrderBookExpanded(true))
            Assert.assertEquals(OrderBookUiState.Loading("BTC-USD"), awaitItem())
            Assert.assertEquals(1, orderBookService.availabilityCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
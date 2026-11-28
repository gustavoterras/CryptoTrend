package br.com.terras.app.portfolio.presentation

import br.com.terras.app.common.ai.AiAnalysisUiState
import br.com.terras.app.common.ai.AiChartAnalysisReport
import br.com.terras.app.common.ai.AiChartAnalysisRequest
import br.com.terras.app.common.ai.AiPortfolioAnalysisReport
import br.com.terras.app.common.ai.AiPortfolioAnalysisRequest
import br.com.terras.app.common.ai.AiPortfolioHealth
import br.com.terras.app.common.ai.AiTrendBias
import br.com.terras.app.common.ai.CryptoAiAnalyzer
import br.com.terras.app.database.portfolio.PortfolioLocalRepository
import br.com.terras.app.database.portfolio.PortfolioTransactionRecord
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.data.PortfolioMarketRepository
import br.com.terras.app.portfolio.domain.PortfolioCalculator
import br.com.terras.app.portfolio.domain.PortfolioUseCaseImpl
import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import br.com.terras.app.portfolio.domain.model.SelectableCoinVO
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioIntent
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioSubTab
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioUiState
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakePortfolioLocalRepository : PortfolioLocalRepository {
        private val flow = MutableStateFlow<List<PortfolioTransactionRecord>>(emptyList())
        private var nextId = 1L

        override fun observeTransactionsChronological(): Flow<List<PortfolioTransactionRecord>> =
            flow.map { list ->
                list.sortedWith(compareBy<PortfolioTransactionRecord> { it.timestamp }.thenBy { it.id })
            }

        override suspend fun getTransactionById(id: Long): PortfolioTransactionRecord? =
            flow.value.firstOrNull { it.id == id }

        override suspend fun insertTransaction(record: PortfolioTransactionRecord): Long {
            val id = nextId++
            flow.value = flow.value + record.copy(id = id)
            return id
        }

        override suspend fun updateTransaction(record: PortfolioTransactionRecord) {
            flow.value = flow.value.map { if (it.id == record.id) record else it }
        }

        override suspend fun deleteTransaction(id: Long) {
            flow.value = flow.value.filterNot { it.id == id }
        }
    }

    private class FakePortfolioMarketRepository(
        var quotesResult: Result<Map<String, PortfolioMarketQuote>> = Result.success(
            mapOf(
                "bitcoin" to PortfolioMarketQuote(
                    id = "bitcoin",
                    symbol = "BTC",
                    name = "Bitcoin",
                    imageUrl = "https://img/btc.png",
                    currentPrice = 400_000.0,
                    priceChangePercentage24h = 2.5
                )
            )
        ),
        var topCoinsResult: Result<List<PortfolioMarketQuote>> = Result.success(
            listOf(
                PortfolioMarketQuote(
                    id = "bitcoin",
                    symbol = "BTC",
                    name = "Bitcoin",
                    imageUrl = "https://img/btc.png",
                    currentPrice = 400_000.0,
                    priceChangePercentage24h = 2.5
                )
            )
        )
    ) : PortfolioMarketRepository {
        override suspend fun getMarketQuotes(coinIds: List<String>): Result<Map<String, PortfolioMarketQuote>> =
            quotesResult

        override suspend fun getTopMarketCoins(limit: Int): Result<List<PortfolioMarketQuote>> =
            topCoinsResult
    }

    private class FakeSearchUseCase : SearchUseCase {
        override suspend fun searchCoins(query: String): Result<List<CoinVO>> =
            Result.success(
                listOf(
                    CoinVO(id = "solana", symbol = "SOL", name = "Solana", thumb = "https://img/sol.png")
                )
            )
    }

    private class FakePortfolioCryptoAiAnalyzer : CryptoAiAnalyzer {
        var lastPortfolioRequest: AiPortfolioAnalysisRequest? = null
        var portfolioResult: Result<AiPortfolioAnalysisReport> = Result.success(
            AiPortfolioAnalysisReport(
                headline = "Carteira concentrada em BTC",
                healthRating = AiPortfolioHealth.CONCENTRATED,
                summary = "100% do capital está alocado em Bitcoin.",
                strengths = listOf("Lucro não realizado positivo"),
                risksAndConcentration = listOf("Alta concentração em um único ativo"),
                actionableIdeas = listOf("Avaliar diversificação gradual"),
                disclaimer = "Análise educacional."
            )
        )

        override suspend fun analyzeChart(request: AiChartAnalysisRequest): AiChartAnalysisReport =
            AiChartAnalysisReport(
                headline = "Neutro",
                trendBias = AiTrendBias.NEUTRAL,
                summary = "Resumo",
                supportLevels = emptyList(),
                resistanceLevels = emptyList(),
                keyObservations = emptyList(),
                riskNote = "Aviso"
            )

        override suspend fun analyzePortfolio(request: AiPortfolioAnalysisRequest): AiPortfolioAnalysisReport {
            lastPortfolioRequest = request
            return portfolioResult.getOrThrow()
        }
    }

    private lateinit var localRepo: FakePortfolioLocalRepository
    private lateinit var marketRepo: FakePortfolioMarketRepository
    private lateinit var aiAnalyzer: FakePortfolioCryptoAiAnalyzer
    private lateinit var viewModel: PortfolioViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        localRepo = FakePortfolioLocalRepository()
        marketRepo = FakePortfolioMarketRepository()
        aiAnalyzer = FakePortfolioCryptoAiAnalyzer()
        val useCase = PortfolioUseCaseImpl(
            localRepository = localRepo,
            marketRepository = marketRepo,
            searchUseCase = FakeSearchUseCase(),
            calculator = PortfolioCalculator()
        )
        viewModel = PortfolioViewModel(useCase, aiAnalyzer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state transitions to Empty when no local transactions exist`() = runTest {
        advanceUntilIdle()
        assertEquals(PortfolioUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `saving a buy transaction transitions to Content and computes live balance`() = runTest {
        advanceUntilIdle()

        viewModel.handleIntent(
            PortfolioIntent.OpenNewTransaction(
                prefilledCoin = SelectableCoinVO("bitcoin", "BTC", "Bitcoin", "https://img/btc.png", 400_000.0)
            )
        )
        viewModel.handleIntent(PortfolioIntent.UpdateQuantityInput("0,5"))
        viewModel.handleIntent(PortfolioIntent.UpdateUnitPriceInput("300000"))
        viewModel.handleIntent(PortfolioIntent.SaveTransaction)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PortfolioUiState.Content
        assertFalse(viewModel.editorState.value.isOpen)
        assertEquals(1, state.snapshot.holdings.size)
        assertEquals(200_000.0, state.snapshot.summary.totalValue, 1e-6)
        assertEquals(150_000.0, state.snapshot.summary.totalInvested, 1e-6)
        assertEquals(50_000.0, state.snapshot.summary.totalPnLValue, 1e-6)
    }

    @Test
    fun `attempting to sell more than held shows inline validation error`() = runTest {
        localRepo.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 0.25,
                unitPrice = 300_000.0,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        viewModel.handleIntent(
            PortfolioIntent.OpenNewTransaction(
                prefilledCoin = SelectableCoinVO("bitcoin", "BTC", "Bitcoin", "", 400_000.0),
                lockCoin = true
            )
        )
        viewModel.handleIntent(PortfolioIntent.UpdateEditorType(PortfolioTransactionType.SELL))
        viewModel.handleIntent(PortfolioIntent.UpdateQuantityInput("1.0"))
        viewModel.handleIntent(PortfolioIntent.UpdateUnitPriceInput("400000"))
        viewModel.handleIntent(PortfolioIntent.SaveTransaction)
        advanceUntilIdle()

        assertTrue(viewModel.editorState.value.isOpen)
        assertNotNull(viewModel.editorState.value.validationError)
    }

    @Test
    fun `offline market failure activates fallback banner without losing local holdings`() = runTest {
        marketRepo.quotesResult = Result.failure(RuntimeException("Network down"))
        localRepo.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 250_000.0,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as PortfolioUiState.Content
        assertTrue(state.isOfflineFallback)
        assertEquals(1, state.snapshot.holdings.size)
        assertEquals(250_000.0, state.snapshot.summary.totalValue, 1e-6)
    }

    @Test
    fun `switching subtab and selecting slice updates state`() = runTest {
        localRepo.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 250_000.0,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        viewModel.handleIntent(PortfolioIntent.SelectSubTab(PortfolioSubTab.HISTORY))
        viewModel.handleIntent(PortfolioIntent.SelectAllocationSlice("bitcoin"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as PortfolioUiState.Content
        assertEquals(PortfolioSubTab.HISTORY, state.activeTab)
        assertEquals("bitcoin", state.selectedSliceId)
    }

    @Test
    fun `analyzing portfolio with AI populates request and emits Success`() = runTest {
        localRepo.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 0.5,
                unitPrice = 300_000.0,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        viewModel.handleIntent(PortfolioIntent.AnalyzePortfolioWithAi)
        advanceUntilIdle()

        val aiState = viewModel.aiPortfolioAnalysis.value
        assertTrue(aiState is AiAnalysisUiState.Success)
        assertEquals(1, aiAnalyzer.lastPortfolioRequest?.holdings?.size)
        assertEquals("BTC", aiAnalyzer.lastPortfolioRequest?.holdings?.first()?.symbol)
    }

    @Test
    fun `portfolio AI failure propagates authentic Error state`() = runTest {
        aiAnalyzer.portfolioResult = Result.failure(IllegalStateException("Network timeout"))
        localRepo.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 250_000.0,
                timestamp = 1000L
            )
        )
        advanceUntilIdle()

        viewModel.handleIntent(PortfolioIntent.AnalyzePortfolioWithAi)
        advanceUntilIdle()

        val aiState = viewModel.aiPortfolioAnalysis.value
        assertTrue(aiState is AiAnalysisUiState.Error)
        assertEquals("Network timeout", (aiState as AiAnalysisUiState.Error).message)
    }
}

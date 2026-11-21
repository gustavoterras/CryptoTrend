package br.com.terras.app.coins.detail.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
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
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val userCase: DetailUseCase = mockk(relaxed = true)
    private val feedsUseCase: FeedsUseCase = mockk(relaxed = true)
    private lateinit var favoriteLocalRepository: FakeDetailFavoriteLocalRepository

    private lateinit var viewModel: DetailViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        favoriteLocalRepository = FakeDetailFavoriteLocalRepository()
        viewModel = DetailViewModel(userCase, feedsUseCase, favoriteLocalRepository)
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
}
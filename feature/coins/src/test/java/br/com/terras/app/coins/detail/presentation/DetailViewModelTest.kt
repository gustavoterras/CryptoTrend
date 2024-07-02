package br.com.terras.app.coins.detail.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Error
import br.com.terras.app.coins.mockCoinChartDetailVOStub
import br.com.terras.app.coins.mockCoinDetailVOStub
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val userCase: DetailUseCase = mockk()

    private lateinit var viewModel: DetailViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        viewModel = DetailViewModel(userCase)
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
}
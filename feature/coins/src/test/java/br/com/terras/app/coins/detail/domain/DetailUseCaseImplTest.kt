package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.detail.data.DetailRepository
import br.com.terras.app.coins.detail.presentation.DaysOfChart
import br.com.terras.app.coins.mockCoinChartDetailVOStub
import br.com.terras.app.coins.mockCoinDetailChartResponseStub
import br.com.terras.app.coins.mockCoinDetailResponseStub
import br.com.terras.app.coins.mockCoinDetailVOStub
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailUseCaseImplTest {
    private val repository: DetailRepository = mockk()
    private val mapper: DetailMapper = mockk()

    private lateinit var useCase: DetailUseCase

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        useCase = DetailUseCaseImpl(repository, mapper)
    }

    @Test
    fun `WHEN getDetailById is called THEN it should return a detail of coin`() = runTest {
        val id = "btc"
        coEvery {
            repository.getDetailById(id)
        } returns Result.success(mockCoinDetailResponseStub)

        every {
            mapper.toCoin(mockCoinDetailResponseStub)
        } returns mockCoinDetailVOStub

        val result = useCase.getDetailById(id)

        Assert.assertEquals(result, Result.success(mockCoinDetailVOStub))
    }

    @Test
    fun `WHEN getChartDetailById is called THEN it should return a chart detail of coin`() = runTest {
        val id = "btc"
        val days = DaysOfChart.DAY
        coEvery {
            repository.getChartDetailById(id, days.value)
        } returns Result.success(mockCoinDetailChartResponseStub)

        every {
            mapper.toChart(mockCoinDetailChartResponseStub)
        } returns mockCoinChartDetailVOStub

        val result = useCase.getChartDetailById(id, days.value)

        Assert.assertEquals(result, Result.success(mockCoinChartDetailVOStub))
    }
}
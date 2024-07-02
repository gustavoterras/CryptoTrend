package br.com.terras.app.coins.detail.data

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.detail.network.ApiServiceDetail
import br.com.terras.app.coins.detail.presentation.DaysOfChart
import br.com.terras.app.coins.mockCoinDetailChartResponseStub
import br.com.terras.app.coins.mockCoinDetailResponseStub
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
class DetailRepositoryImplTest {
    private val services: ApiServiceDetail = mockk()

    private lateinit var repository: DetailRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = DetailRepositoryImpl(services)
    }

    @Test
    fun `WHEN getDetailById THEN should return success response`() = runBlocking {
        val id = "btc"
        coEvery {
            services.getDetailById(id)
        } returns mockCoinDetailResponseStub

        val result = repository.getDetailById(id)

        Assert.assertEquals(result, Result.success(mockCoinDetailResponseStub))
    }

    @Test
    fun `WHEN getChartDetailById THEN should return success response`() = runBlocking {
        val id = "btc"
        val days = DaysOfChart.DAY
        coEvery {
            services.getChartDetailById(id, days.value)
        } returns mockCoinDetailChartResponseStub

        val result = repository.getChartDetailById(id, days.value)

        Assert.assertEquals(result, Result.success(mockCoinDetailChartResponseStub))
    }
}
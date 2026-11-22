package br.com.terras.app.coins.list.data

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.network.ApiServiceCoins
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockTrendingCoinsResponseStub
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
class CoinsRepositoryImplTest {
    private val services: ApiServiceCoins = mockk(relaxed = true)

    private lateinit var repository: CoinsRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = CoinsRepositoryImpl(services)
    }

    @Test
    fun `WHEN getCoins THEN should return success response`() = runBlocking {
        coEvery {
            services.getCoins()
        } returns mockCoinsListResponseStub

        val result = repository.getCoins()

        Assert.assertEquals(result, Result.success(mockCoinsListResponseStub))
    }

    @Test
    fun `WHEN getTrendingCoins THEN should return success response`() = runBlocking {
        coEvery {
            services.getTrendingCoins()
        } returns mockTrendingCoinsResponseStub

        val result = repository.getTrendingCoins()

        Assert.assertEquals(Result.success(mockTrendingCoinsResponseStub), result)
    }

    @Test
    fun `WHEN getCoins with page 2 THEN forwards page parameter to service`() = runBlocking {
        coEvery {
            services.getCoins(page = 2)
        } returns mockCoinsListResponseStub

        val result = repository.getCoins(page = 2)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }

    @Test
    fun `WHEN getCoinsByIds THEN should return success response`() = runBlocking {
        val ids = listOf("id")
        coEvery {
            services.getCoinsByIds(ids)
        } returns mockCoinsListResponseStub

        val result = repository.getCoinsByIds(ids)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }
}
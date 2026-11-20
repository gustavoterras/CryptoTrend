package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsVOListStub
import br.com.terras.app.coins.mockTrendingCoinsResponseStub
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
class CoinsUseCaseImplTest {
    private val repository: CoinsRepository = mockk(relaxed = true)
    private val mapper: CoinsMapper = mockk(relaxed = true)

    private lateinit var useCase: CoinsUseCase

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        useCase = CoinsUseCaseImpl(repository, mapper)
    }

    @Test
    fun `WHEN getCoins is called THEN it should return a list of coins`() = runTest {
        coEvery {
            repository.getCoins()
        } returns Result.success(mockCoinsListResponseStub)

        every {
            mapper.toCoins(mockCoinsListResponseStub)
        } returns mockCoinsVOListStub

        val result = useCase.getCoins()

        Assert.assertEquals(result, Result.success(mockCoinsVOListStub))
    }

    @Test
    fun `WHEN getTrendingCoins is called THEN enriches with market data and returns mapped coins`() = runTest {
        coEvery {
            repository.getTrendingCoins()
        } returns Result.success(mockTrendingCoinsResponseStub)

        coEvery {
            repository.getCoinsByIds(listOf("id"))
        } returns Result.success(mockCoinsListResponseStub)

        every {
            mapper.toTrendingCoins(mockTrendingCoinsResponseStub, mockCoinsListResponseStub)
        } returns mockCoinsVOListStub

        val result = useCase.getTrendingCoins()

        Assert.assertEquals(Result.success(mockCoinsVOListStub), result)
    }

    @Test
    fun `WHEN getCoinsByIds fails during getTrendingCoins THEN falls back to empty market list`() = runTest {
        coEvery {
            repository.getTrendingCoins()
        } returns Result.success(mockTrendingCoinsResponseStub)

        coEvery {
            repository.getCoinsByIds(listOf("id"))
        } returns Result.failure(RuntimeException("rate limit"))

        every {
            mapper.toTrendingCoins(mockTrendingCoinsResponseStub, emptyList())
        } returns mockCoinsVOListStub

        val result = useCase.getTrendingCoins()

        Assert.assertEquals(Result.success(mockCoinsVOListStub), result)
    }
}
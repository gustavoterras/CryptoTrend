package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.list.data.model.TrendingCoinItem
import br.com.terras.app.coins.list.data.model.TrendingCoinWrapper
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsVOListStub
import br.com.terras.app.coins.mockGlobalMarketResponseStub
import br.com.terras.app.coins.mockGlobalMarketVOStub
import br.com.terras.app.coins.mockTrendingCoinsResponseStub
import br.com.terras.app.dsm.ui.component.TrendColor
import io.mockk.coEvery
import io.mockk.coVerify
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

    @Test
    fun `WHEN getCoins is called with page 2 THEN requests page 2 from repository`() = runTest {
        coEvery { repository.getCoins(page = 2) } returns Result.success(mockCoinsListResponseStub)
        every { mapper.toCoins(mockCoinsListResponseStub) } returns mockCoinsVOListStub

        val result = useCase.getCoins(page = 2)

        coVerify(exactly = 1) { repository.getCoins(page = 2) }
        Assert.assertEquals(Result.success(mockCoinsVOListStub), result)
    }

    @Test
    fun `WHEN getTrendingCoins paginates across pages THEN slices cached trending coins and enriches per page`() = runTest {
        val wrappers = (1..10).map { index ->
            TrendingCoinWrapper(
                item = TrendingCoinItem(id = "coin-$index", symbol = "C$index", name = "Coin $index")
            )
        }
        val fullResponse = TrendingCoinsResponse(coins = wrappers)
        val page1Wrappers = wrappers.subList(0, CoinsUseCase.TRENDING_PAGE_SIZE)
        val page2Wrappers = wrappers.subList(CoinsUseCase.TRENDING_PAGE_SIZE, 10)
        val page1VOs = page1Wrappers.map {
            CoinVO(
                id = it.item.id,
                symbol = it.item.symbol,
                name = it.item.name,
                image = "",
                marketCapRank = "1",
                price = "$1.00",
                priceChangePercentage = "1.0%",
                trendColor = TrendColor.UP
            )
        }
        val page2VOs = page2Wrappers.map {
            CoinVO(
                id = it.item.id,
                symbol = it.item.symbol,
                name = it.item.name,
                image = "",
                marketCapRank = "2",
                price = "$2.00",
                priceChangePercentage = "2.0%",
                trendColor = TrendColor.UP
            )
        }

        coEvery { repository.getTrendingCoins() } returns Result.success(fullResponse)
        coEvery { repository.getCoinsByIds(any()) } returns Result.success(emptyList())
        every {
            mapper.toTrendingCoins(TrendingCoinsResponse(coins = page1Wrappers), emptyList())
        } returns page1VOs
        every {
            mapper.toTrendingCoins(TrendingCoinsResponse(coins = page2Wrappers), emptyList())
        } returns page2VOs

        val page1Result = useCase.getTrendingCoins(page = 1)
        val page2Result = useCase.getTrendingCoins(page = 2)
        val page3Result = useCase.getTrendingCoins(page = 3)

        coVerify(exactly = 1) { repository.getTrendingCoins() }
        coVerify(exactly = 1) { repository.getCoinsByIds(page1Wrappers.map { it.item.id }) }
        coVerify(exactly = 1) { repository.getCoinsByIds(page2Wrappers.map { it.item.id }) }
        Assert.assertEquals(Result.success(page1VOs), page1Result)
        Assert.assertEquals(Result.success(page2VOs), page2Result)
        Assert.assertEquals(Result.success(emptyList<CoinVO>()), page3Result)
    }

    @Test
    fun `WHEN getGlobalMarketData is called THEN maps repository response to GlobalMarketVO`() = runTest {
        coEvery { repository.getGlobalMarketData(forceRefresh = false) } returns Result.success(mockGlobalMarketResponseStub)
        every { mapper.toGlobalMarket(mockGlobalMarketResponseStub) } returns mockGlobalMarketVOStub

        val result = useCase.getGlobalMarketData()

        Assert.assertEquals(Result.success(mockGlobalMarketVOStub), result)
    }
}
package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsVOListStub
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
    private val repository: CoinsRepository = mockk()
    private val mapper: CoinsMapper = mockk()

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
}
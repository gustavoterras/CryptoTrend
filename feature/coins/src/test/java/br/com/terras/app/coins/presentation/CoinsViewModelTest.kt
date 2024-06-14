package br.com.terras.app.coins.presentation

import app.cash.turbine.test
import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.domain.CoinsUseCase
import br.com.terras.app.coins.mockCoinsVOListStub
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Error
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Loading
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Success
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoinsViewModelTest {
    private val userCase: CoinsUseCase = mockk()

    private lateinit var viewModel: CoinsViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        viewModel = CoinsViewModel(userCase)
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(Loading, viewModel.coins.value)
    }

    @Test
    fun `WHEN getCoins THEN return error`() = runBlocking {
        coEvery {
            userCase.getCoins()
        } returns Result.failure(IllegalAccessException())

        coVerify {
            userCase.getCoins()
        }

        viewModel.getCoins()

        viewModel.coins.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN get feeds THEN return success`() = runBlocking {
        val result = mockCoinsVOListStub

        coEvery {
            userCase.getCoins()
        } returns Result.success(result)

        coVerify {
            userCase.getCoins()
        }

        viewModel.getCoins()

        viewModel.coins.test {
            Assert.assertEquals(Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
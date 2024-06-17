package br.com.terras.app.favorites.presentation

import app.cash.turbine.test
import br.com.terras.app.favorites.MainDispatcherRule
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.mockCoinsVOListStub
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Success
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Loading
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Error
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
class FavoritesViewModelTest {
    private val userCase: FavoritesUseCase = mockk()

    private lateinit var viewModel: FavoritesViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        viewModel = FavoritesViewModel(userCase)
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(Loading, viewModel.coins.value)
    }

    @Test
    fun `WHEN getFavorites THEN return error`() = runBlocking {
        val ids = listOf("bitcoin", "ethereum", "cardano")

        coEvery {
            userCase.getCoinsByIds(ids)
        } returns Result.failure(IllegalAccessException())

        viewModel.getFavorites()

        coVerify {
            userCase.getCoinsByIds(ids)
        }

        viewModel.coins.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN getFavorites THEN return success`() = runBlocking {
        val ids = listOf("bitcoin", "ethereum", "cardano")
        val result = mockCoinsVOListStub

        coEvery {
            userCase.getCoinsByIds(ids)
        } returns Result.success(result)

        viewModel.getFavorites()

        coVerify {
            userCase.getCoinsByIds(ids)
        }

        viewModel.coins.test {
            Assert.assertEquals(Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
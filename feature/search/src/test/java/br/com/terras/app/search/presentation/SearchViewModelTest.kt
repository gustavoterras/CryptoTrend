package br.com.terras.app.search.presentation

import app.cash.turbine.test
import br.com.terras.app.search.MainDispatcherRule
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO
import br.com.terras.app.search.mockCoinsVOListStub
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Empty
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Error
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Success
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
class SearchViewModelTest {
    private val userCase: SearchUseCase = mockk()

    private lateinit var viewModel: SearchViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        viewModel = SearchViewModel(userCase)
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(Success(emptyList()), viewModel.coins.value)
    }

    @Test
    fun `WHEN doSearch THEN return error`() = runBlocking {
        val query = "btc"
        val result = mockCoinsVOListStub

        coEvery {
            userCase.searchCoins(query)
        } returns Result.success(result)

        viewModel.doSearch(query)

        viewModel.coins.test {
            Assert.assertEquals(Success(emptyList()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN searchCoins THEN return error`() = runBlocking {
        val query = "btc"

        coEvery {
            userCase.searchCoins(query)
        } returns Result.failure(IllegalAccessException())

        viewModel.getCoins(query)

        coVerify {
            userCase.searchCoins(query)
        }

        viewModel.coins.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN searchCoins THEN return success`() = runBlocking {
        val query = "btc"
        val result = mockCoinsVOListStub

        coEvery {
            userCase.searchCoins(query)
        } returns Result.success(result)

        viewModel.getCoins(query)

        coVerify {
            userCase.searchCoins(query)
        }

        viewModel.coins.test {
            Assert.assertEquals(Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN searchCoins THEN return success with empty result`() = runBlocking {
        val query = "btc"
        val result = arrayListOf<CoinVO>()

        coEvery {
            userCase.searchCoins(query)
        } returns Result.success(result)

        viewModel.getCoins(query)

        coVerify {
            userCase.searchCoins(query)
        }

        viewModel.coins.test {
            Assert.assertEquals(Empty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
package br.com.terras.app.feeds.presentation

import app.cash.turbine.test
import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.mockArticlesVOStub
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState
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
class FeedsViewModelTest {

    private val userCase: FeedsUseCase = mockk()

    private lateinit var viewModel: FeedsViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        viewModel = FeedsViewModel(userCase)
    }

    @Test
    fun `Init ViewModel()`() {
        Assert.assertEquals(FeedsState.Loading, viewModel.feeds.value)
    }

    @Test
    fun `WHEN get feeds THEN return error`() = runBlocking {
        val query = "bitcoin"

        coEvery {
            userCase.getFeeds(query)
        } returns Result.failure(IllegalAccessException())

        coVerify {
            userCase.getFeeds(query)
        }

        viewModel.getFeeds()

        viewModel.feeds.test {
            Assert.assertEquals(FeedsState.Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN get feeds THEN return success`() = runBlocking {
        val query = "bitcoin"
        val result = mockArticlesVOStub

        coEvery {
            userCase.getFeeds(query)
        } returns Result.success(result)

        coVerify {
            userCase.getFeeds(query)
        }

        viewModel.getFeeds()

        viewModel.feeds.test {
            Assert.assertEquals(FeedsState.Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
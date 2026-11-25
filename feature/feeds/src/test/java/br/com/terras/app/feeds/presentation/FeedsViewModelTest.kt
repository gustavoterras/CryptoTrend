package br.com.terras.app.feeds.presentation

import app.cash.turbine.test
import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.mockArticlesVOStub
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsIntent
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState
import br.com.terras.app.network.refresh.DataRefreshCoordinator
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

    private val userCase: FeedsUseCase = mockk(relaxed = true)
    private lateinit var refreshCoordinator: DataRefreshCoordinator

    private lateinit var viewModel: FeedsViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = FeedsViewModel(userCase, refreshCoordinator)
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

        viewModel.getFeeds()

        coVerify {
            userCase.getFeeds(query)
        }

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

        viewModel.getFeeds()

        coVerify {
            userCase.getFeeds(query)
        }

        viewModel.feeds.test {
            Assert.assertEquals(FeedsState.Success(result), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN Refresh intent THEN reload feeds and reset isRefreshing`() = runBlocking {
        val result = mockArticlesVOStub
        coEvery { userCase.getFeeds("bitcoin", forceRefresh = true) } returns Result.success(result)

        viewModel.handleIntent(FeedsIntent.Refresh)

        coVerify(exactly = 1) { userCase.getFeeds("bitcoin", forceRefresh = true) }
        Assert.assertEquals(FeedsState.Success(result), viewModel.feeds.value)
        Assert.assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `WHEN market refresh coordinator triggers THEN reload feeds`() = runBlocking {
        val result = mockArticlesVOStub
        coEvery { userCase.getFeeds("bitcoin", forceRefresh = true) } returns Result.success(result)

        refreshCoordinator.triggerRefresh()

        coVerify(exactly = 1) { userCase.getFeeds("bitcoin") }
        coVerify(exactly = 1) { userCase.getFeeds("bitcoin", forceRefresh = true) }
        Assert.assertEquals(FeedsState.Success(result), viewModel.feeds.value)
    }
}
package br.com.terras.app.feeds.presentation

import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.domain.FeedsUseCaseImpl
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class FeedsViewModelTest {

    private val userCase: FeedsUseCaseImpl = mockk()

    private lateinit var viewModel: FeedsViewModel

    @get:Rule
    val mockkRule = MockKRule(this)

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
    fun `WHEN get feeds THEN return error`() {
        val query = "bitcoin"
        val message = "Error from api"

        coEvery {
            userCase.getFeeds(query)
        } throws IllegalAccessException(message)

        coVerify {
            userCase.getFeeds(query)
        }

        Assert.assertEquals(FeedsState.Loading, viewModel.feeds.value)
    }
}
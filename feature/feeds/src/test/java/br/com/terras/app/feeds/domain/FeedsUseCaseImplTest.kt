package br.com.terras.app.feeds.domain

import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.data.FeedsRepository
import br.com.terras.app.feeds.getDateFrom
import br.com.terras.app.feeds.mockArticlesVOStub
import br.com.terras.app.feeds.mockFeedsResponseStub
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
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class FeedsUseCaseImplTest {

    private val repository: FeedsRepository = mockk()
    private val mapper: FeedsMapper = mockk()

    private lateinit var useCase: FeedsUseCase

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        useCase = FeedsUseCaseImpl(repository, mapper)
    }

    @Test
    fun `WHEN getFeeds is called THEN it should return a list of articles`() = runTest {
        val query = "bitcoin"
        val dateFrom = getDateFrom()
        val language = Locale.getDefault().language

        coEvery {
            repository.getFeeds(query, dateFrom, language)
        } returns Result.success(mockFeedsResponseStub)

        every {
            mapper.toFeeds(mockFeedsResponseStub.articles)
        } returns mockArticlesVOStub

        val result = useCase.getFeeds(query)

        Assert.assertEquals(result, Result.success(mockArticlesVOStub))
    }
}
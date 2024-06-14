package br.com.terras.app.feeds.data

import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.getDateFrom
import br.com.terras.app.feeds.mockFeedsResponseStub
import br.com.terras.app.feeds.network.ApiServicesFeeds
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)

class FeedsRepositoryImplTest {

    private val services: ApiServicesFeeds = mockk()

    private lateinit var repository: FeedsRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = FeedsRepositoryImpl(services)
    }

    @Test
    fun `WHEN getFeeds THEN should return success response`() = runBlocking {
        val query = "bitcoin"
        val dateFrom = getDateFrom()
        val language = Locale.getDefault().language

        coEvery {
            services.getFeeds(query, dateFrom, language)
        } returns mockFeedsResponseStub

        val result = repository.getFeeds(query, dateFrom, language)

        Assert.assertEquals(result, Result.success(mockFeedsResponseStub))
    }
}
package br.com.terras.app.search.data

import br.com.terras.app.search.MainDispatcherRule
import br.com.terras.app.search.mockCoinsResponseStub
import br.com.terras.app.search.network.ApiServiceSearch
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryImplTest {
    private val services: ApiServiceSearch = mockk()

    private lateinit var repository: SearchRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = SearchRepositoryImpl(services)
    }

    @Test
    fun `WHEN getSearch THEN should return success response`() = runBlocking {
        val query = "btc"
        coEvery {
            services.searchCoins(query)
        } returns mockCoinsResponseStub

        val result = repository.searchCoins(query)

        Assert.assertEquals(result, Result.success(mockCoinsResponseStub))
    }
}
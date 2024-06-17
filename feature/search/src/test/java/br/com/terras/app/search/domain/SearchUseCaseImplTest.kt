package br.com.terras.app.search.domain

import br.com.terras.app.search.MainDispatcherRule
import br.com.terras.app.search.data.SearchRepository
import br.com.terras.app.search.mockCoinsResponseStub
import br.com.terras.app.search.mockCoinsVOListStub
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
class SearchUseCaseImplTest {
    private val repository: SearchRepository = mockk()
    private val mapper: SearchMapper = mockk()

    private lateinit var useCase: SearchUseCase

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        useCase = SearchUseCaseImpl(repository, mapper)
    }

    @Test
    fun `WHEN searchCoins is called THEN it should return a list of coins`() = runTest {
        val query = "btc"
        coEvery {
            repository.searchCoins(query)
        } returns Result.success(mockCoinsResponseStub)

        every {
            mapper.toSearch(mockCoinsResponseStub)
        } returns mockCoinsVOListStub

        val result = useCase.searchCoins(query)

        Assert.assertEquals(result, Result.success(mockCoinsVOListStub))
    }
}
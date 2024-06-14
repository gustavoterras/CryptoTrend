package br.com.terras.app.coins.data

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.network.ApiService
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
class CoinsRepositoryImplTest {
    private val services: ApiService = mockk()

    private lateinit var repository: CoinsRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = CoinsRepositoryImpl(services)
    }

    @Test
    fun `WHEN getCoins THEN should return success response`() = runBlocking {
        coEvery {
            services.getCoins()
        } returns mockCoinsListResponseStub

        val result = repository.getCoins()

        Assert.assertEquals(result, Result.success(mockCoinsListResponseStub))
    }
}
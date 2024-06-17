package br.com.terras.app.favorites.data

import br.com.terras.app.favorites.MainDispatcherRule
import br.com.terras.app.favorites.mockCoinsListResponseStub
import br.com.terras.app.favorites.network.ApiServiceFavorites
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
class FavoritesRepositoryImplTest {
    private val services: ApiServiceFavorites = mockk()

    private lateinit var repository: FavoritesRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        repository = FavoritesRepositoryImpl(services)
    }

    @Test
    fun `WHEN getCoins THEN should return success response`() = runBlocking {
        val ids = listOf("bitcoin", "ethereum", "cardano")

        coEvery {
            services.getCoinsByIds(ids.joinToString(","))
        } returns mockCoinsListResponseStub

        val result = repository.getCoinsByIds(ids)

        Assert.assertEquals(result, Result.success(mockCoinsListResponseStub))
    }
}
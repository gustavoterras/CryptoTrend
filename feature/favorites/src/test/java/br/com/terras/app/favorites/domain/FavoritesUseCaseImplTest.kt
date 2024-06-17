package br.com.terras.app.favorites.domain

import br.com.terras.app.favorites.MainDispatcherRule
import br.com.terras.app.favorites.data.FavoritesRepository
import br.com.terras.app.favorites.mockCoinsListResponseStub
import br.com.terras.app.favorites.mockCoinsVOListStub
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
class FavoritesUseCaseImplTest {
    private val repository: FavoritesRepository = mockk()
    private val mapper: FavoritesMapper = mockk()

    private lateinit var useCase: FavoritesUseCase

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        useCase = FavoritesUseCaseImpl(repository, mapper)
    }

    @Test
    fun `WHEN getCoinsByIds is called THEN it should return a list of coins`() = runTest {
        val ids = listOf("bitcoin", "ethereum", "cardano")

        coEvery {
            repository.getCoinsByIds(ids)
        } returns Result.success(mockCoinsListResponseStub)

        every {
            mapper.toCoins(mockCoinsListResponseStub)
        } returns mockCoinsVOListStub

        val result = useCase.getCoinsByIds(ids)

        Assert.assertEquals(result, Result.success(mockCoinsVOListStub))
    }
}
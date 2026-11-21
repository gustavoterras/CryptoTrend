package br.com.terras.app.network.favorites

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FavoriteCoinsLocalRepositoryImplTest {

    private val dao: FavoriteCoinDao = mockk(relaxed = true)
    private lateinit var repository: FavoriteCoinsLocalRepository

    @Before
    fun setup() {
        repository = FavoriteCoinsLocalRepositoryImpl(dao)
    }

    @Test
    fun `WHEN observeFavoriteIds THEN emits IDs from DAO`() = runTest {
        every { dao.observeFavoriteIds() } returns flowOf(listOf("bitcoin", "ethereum"))

        repository.observeFavoriteIds().test {
            Assert.assertEquals(listOf("bitcoin", "ethereum"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `WHEN observeIsFavorite THEN emits state from DAO`() = runTest {
        every { dao.observeIsFavorite("bitcoin") } returns flowOf(true)

        repository.observeIsFavorite("bitcoin").test {
            Assert.assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `WHEN toggleFavorite on non-favorite coin THEN inserts into DAO and returns true`() = runTest {
        coEvery { dao.isFavorite("bitcoin") } returns false

        val result = repository.toggleFavorite("bitcoin")

        Assert.assertTrue(result)
        coVerify(exactly = 1) { dao.insertFavorite(match { it.id == "bitcoin" }) }
        coVerify(exactly = 0) { dao.deleteFavoriteById(any()) }
    }

    @Test
    fun `WHEN toggleFavorite on existing favorite coin THEN deletes from DAO and returns false`() = runTest {
        coEvery { dao.isFavorite("bitcoin") } returns true

        val result = repository.toggleFavorite("bitcoin")

        Assert.assertFalse(result)
        coVerify(exactly = 1) { dao.deleteFavoriteById("bitcoin") }
        coVerify(exactly = 0) { dao.insertFavorite(any()) }
    }
}

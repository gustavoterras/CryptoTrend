package br.com.terras.app.favorites.presentation

import app.cash.turbine.test
import br.com.terras.app.favorites.MainDispatcherRule
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.mockCoinVOStub
import br.com.terras.app.favorites.mockCoinsVOListStub
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesIntent
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Error
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Loading
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Success
import br.com.terras.app.database.favorites.FavoriteCoinsLocalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private class FakeFavoriteCoinsLocalRepository(
    private val idsFlow: Flow<List<String>>
) : FavoriteCoinsLocalRepository {
    override fun observeFavoriteIds(): Flow<List<String>> = idsFlow
    override fun observeIsFavorite(coinId: String): Flow<Boolean> = flowOf(false)
    override suspend fun toggleFavorite(coinId: String): Boolean = false
    override suspend fun addFavorite(coinId: String) = Unit
    override suspend fun removeFavorite(coinId: String) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val userCase: FavoritesUseCase = mockk(relaxed = true)
    private val favoriteIdsFlow = MutableStateFlow<List<String>>(emptyList())
    private lateinit var favoriteLocalRepository: FavoriteCoinsLocalRepository

    private lateinit var viewModel: FavoritesViewModel

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        favoriteLocalRepository = FakeFavoriteCoinsLocalRepository(favoriteIdsFlow)
        viewModel = FavoritesViewModel(userCase, favoriteLocalRepository)
    }

    @Test
    fun `WHEN no favorites emitted yet THEN state is Loading`() {
        val loadingVm = FavoritesViewModel(userCase, FakeFavoriteCoinsLocalRepository(emptyFlow()))

        Assert.assertEquals(Loading, loadingVm.coins.value)
    }

    @Test
    fun `WHEN local favorites is empty THEN returns empty Success without calling network`() = runBlocking {
        viewModel.coins.test {
            Assert.assertEquals(Success(emptyList()), awaitItem())
            coVerify(exactly = 0) { userCase.getCoinsByIds(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN local favorites fail to load from API THEN return Error`() = runBlocking {
        val ids = listOf("bitcoin", "ethereum")
        coEvery {
            userCase.getCoinsByIds(ids)
        } returns Result.failure(IllegalAccessException())

        favoriteIdsFlow.value = ids

        coVerify(exactly = 1) {
            userCase.getCoinsByIds(ids)
        }

        viewModel.coins.test {
            Assert.assertEquals(Error, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WHEN local favorites emit IDs THEN loads and orders coins and reacts to unfavorite`() = runBlocking {
        val bitcoin = mockCoinVOStub.copy(id = "bitcoin", symbol = "BTC", name = "Bitcoin")
        val ethereum = mockCoinVOStub.copy(id = "ethereum", symbol = "ETH", name = "Ethereum")

        coEvery {
            userCase.getCoinsByIds(listOf("bitcoin", "ethereum"))
        } returns Result.success(listOf(ethereum, bitcoin))

        coEvery {
            userCase.getCoinsByIds(listOf("bitcoin"))
        } returns Result.success(listOf(bitcoin))

        favoriteIdsFlow.value = listOf("bitcoin", "ethereum")

        Assert.assertEquals(Success(listOf(bitcoin, ethereum)), viewModel.coins.value)

        favoriteIdsFlow.value = listOf("bitcoin")
        Assert.assertEquals(Success(listOf(bitcoin)), viewModel.coins.value)

        favoriteIdsFlow.value = emptyList()
        Assert.assertEquals(Success(emptyList()), viewModel.coins.value)
    }

    @Test
    fun `WHEN Refresh intent THEN reloads favorites and clears isRefreshing`() = runBlocking {
        val ids = listOf("id")
        coEvery { userCase.getCoinsByIds(ids) } returns Result.success(mockCoinsVOListStub)
        coEvery { userCase.getCoinsByIds(ids, forceRefresh = true) } returns Result.success(mockCoinsVOListStub)
        favoriteIdsFlow.value = ids

        viewModel.handleIntent(FavoritesIntent.Refresh)

        coVerify(exactly = 1) { userCase.getCoinsByIds(ids) }
        coVerify(exactly = 1) { userCase.getCoinsByIds(ids, forceRefresh = true) }
        Assert.assertEquals(Success(mockCoinsVOListStub), viewModel.coins.value)
        Assert.assertFalse(viewModel.isRefreshing.value)
    }
}
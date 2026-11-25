package br.com.terras.app.favorites.domain

import br.com.terras.app.favorites.data.FavoritesRepository
import br.com.terras.app.favorites.domain.model.CoinVO
import javax.inject.Inject

interface FavoritesUseCase {
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinVO>>

    suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinVO>? = null
    suspend fun getCoinsByIds(ids: List<String>, forceRefresh: Boolean): Result<List<CoinVO>> =
        getCoinsByIds(ids)
}

class FavoritesUseCaseImpl @Inject constructor(
    private val repository: FavoritesRepository,
    private val mapper: FavoritesMapper
) : FavoritesUseCase {

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinVO>> =
        getCoinsByIds(ids = ids, forceRefresh = false)

    override suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinVO>? {
        if (ids.isEmpty()) return emptyList()
        val cached = repository.getCachedCoinsByIds(ids) ?: return null
        return mapper.toCoins(cached)
    }

    override suspend fun getCoinsByIds(
        ids: List<String>,
        forceRefresh: Boolean
    ): Result<List<CoinVO>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        val result = if (forceRefresh) {
            repository.getCoinsByIds(ids, forceRefresh = true)
        } else {
            repository.getCoinsByIds(ids)
        }
        return result.map { items ->
            mapper.toCoins(items)
        }
    }
}
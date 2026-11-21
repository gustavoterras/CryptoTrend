package br.com.terras.app.favorites.domain

import br.com.terras.app.favorites.data.FavoritesRepository
import br.com.terras.app.favorites.domain.model.CoinVO
import javax.inject.Inject

interface FavoritesUseCase {
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinVO>>
}

class FavoritesUseCaseImpl @Inject constructor(
    private val repository: FavoritesRepository,
    private val mapper: FavoritesMapper
) : FavoritesUseCase {

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinVO>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return repository.getCoinsByIds(ids).map { result ->
            mapper.toCoins(result)
        }
    }
}
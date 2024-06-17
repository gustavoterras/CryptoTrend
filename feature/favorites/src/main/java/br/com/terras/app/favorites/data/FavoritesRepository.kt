package br.com.terras.app.favorites.data

import br.com.terras.app.favorites.data.model.CoinsResponse
import br.com.terras.app.favorites.network.ApiServiceFavorites
import javax.inject.Inject

interface FavoritesRepository {
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>>
}

class FavoritesRepositoryImpl @Inject constructor(
    private val api: ApiServiceFavorites
) : FavoritesRepository {

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>> {
        return try {
            Result.success(api.getCoinsByIds(ids.joinToString(",")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
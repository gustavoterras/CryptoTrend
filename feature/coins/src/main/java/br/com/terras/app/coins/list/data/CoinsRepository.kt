package br.com.terras.app.coins.list.data

import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.network.ApiServiceCoins
import javax.inject.Inject

interface CoinsRepository {
    suspend fun getCoins(page: Int = 1): Result<List<CoinsResponse>>
    suspend fun getTrendingCoins(): Result<TrendingCoinsResponse>
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>>
}

class CoinsRepositoryImpl @Inject constructor(
    private val api: ApiServiceCoins
) : CoinsRepository {

    override suspend fun getCoins(page: Int): Result<List<CoinsResponse>> {
        return try {
            Result.success(api.getCoins(page))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingCoins(): Result<TrendingCoinsResponse> {
        return try {
            Result.success(api.getTrendingCoins())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>> {
        return try {
            Result.success(api.getCoinsByIds(ids))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
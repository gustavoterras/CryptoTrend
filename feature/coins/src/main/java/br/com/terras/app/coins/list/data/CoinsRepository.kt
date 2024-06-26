package br.com.terras.app.coins.list.data

import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.network.ApiServiceCoins
import javax.inject.Inject

interface CoinsRepository {
    suspend fun getCoins(): Result<List<CoinsResponse>>
}

class CoinsRepositoryImpl @Inject constructor(
    private val api: ApiServiceCoins
) : CoinsRepository {

    override suspend fun getCoins(): Result<List<CoinsResponse>> {
        return try {
            Result.success(api.getCoins())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
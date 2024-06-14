package br.com.terras.app.coins.data

import br.com.terras.app.coins.data.model.CoinsResponse
import br.com.terras.app.coins.network.ApiService
import javax.inject.Inject

interface CoinsRepository {
    suspend fun getCoins(): Result<List<CoinsResponse>>
}

class CoinsRepositoryImpl @Inject constructor(
    private val api: ApiService
) : CoinsRepository {

    override suspend fun getCoins(): Result<List<CoinsResponse>> {
        return try {
            Result.success(api.getCoins())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
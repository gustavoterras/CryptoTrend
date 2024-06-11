package br.com.terras.app.home.data

import br.com.terras.app.home.data.model.CoinsListResponse
import br.com.terras.app.home.network.ApiService
import io.ktor.client.call.body
import javax.inject.Inject

interface CoinsListRepository {
    suspend fun getCoinsList(): Result<List<CoinsListResponse>>
}

class CoinsListRepositoryImpl @Inject constructor(
    private val api: ApiService
) : CoinsListRepository {

    override suspend fun getCoinsList(): Result<List<CoinsListResponse>> {
        return try {
            Result.success(api.getCoins().body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package br.com.terras.app.coins.detail.data

import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import br.com.terras.app.coins.detail.data.model.CoinInsightResponse
import br.com.terras.app.coins.detail.network.ApiServiceDetail
import javax.inject.Inject

interface DetailRepository {
    suspend fun getDetailById(id: String): Result<CoinDetailResponse>
    suspend fun getChartDetailById(id: String, days: Int): Result<CoinDetailMarketChartResponse>
    suspend fun getInsightsByCoinId(coinId: String): Result<List<CoinInsightResponse>>
}

class DetailRepositoryImpl @Inject constructor(
    private val api: ApiServiceDetail
) : DetailRepository {

    override suspend fun getDetailById(id: String): Result<CoinDetailResponse> {
        return try {
            Result.success(api.getDetailById(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChartDetailById(id: String, days: Int): Result<CoinDetailMarketChartResponse> {
        return try {
            Result.success(api.getChartDetailById(id, days))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInsightsByCoinId(coinId: String): Result<List<CoinInsightResponse>> {
        return try {
            Result.success(api.getInsightsByCoinId(coinId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
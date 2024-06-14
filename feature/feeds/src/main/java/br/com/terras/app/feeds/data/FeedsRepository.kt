package br.com.terras.app.feeds.data

import br.com.terras.app.feeds.data.model.FeedsResponse
import br.com.terras.app.feeds.network.ApiServicesFeeds
import io.ktor.client.call.body
import javax.inject.Inject

interface FeedsRepository {
    suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String
    ): Result<FeedsResponse>
}

class FeedsRepositoryImpl @Inject constructor(
    private val api: ApiServicesFeeds
) : FeedsRepository {

    override suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String
    ): Result<FeedsResponse> {
        return try {
            Result.success(api.getFeeds(query, fromDate, language))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
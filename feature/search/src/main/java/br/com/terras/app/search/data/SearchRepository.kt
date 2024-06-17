package br.com.terras.app.search.data

import br.com.terras.app.search.data.model.SearchResponse
import br.com.terras.app.search.network.ApiServiceSearch
import javax.inject.Inject

interface SearchRepository {
    suspend fun searchCoins(query: String): Result<SearchResponse>
}

class SearchRepositoryImpl @Inject constructor(
    private val api: ApiServiceSearch
) : SearchRepository {

    override suspend fun searchCoins(query: String): Result<SearchResponse> {
        return try {
            Result.success(api.searchCoins(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
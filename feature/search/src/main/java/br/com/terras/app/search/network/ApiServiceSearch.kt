package br.com.terras.app.search.network

import br.com.terras.app.search.data.model.SearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url

class ApiServiceSearch(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_SEARCH_DATA_ROUTE = "v3/search"
        private const val COINS_SEARCH_DATA_PARAMS_KEY = "query"
    }

    suspend fun searchCoins(query: String): SearchResponse = client.get {
        url("$baseUrl$COINS_SEARCH_DATA_ROUTE")
        parameter(COINS_SEARCH_DATA_PARAMS_KEY, query)
    }.body()
}
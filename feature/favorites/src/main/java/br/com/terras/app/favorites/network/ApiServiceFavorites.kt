package br.com.terras.app.favorites.network

import br.com.terras.app.favorites.data.model.CoinsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url

class ApiServiceFavorites(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/markets"
        private const val COINS_MARKET_DATA_PARAMS_IDS = "ids"
        private const val COINS_MARKET_DATA_PARAMS_KEY = "vs_currency"
        private const val COINS_MARKET_DATA_PARAMS_VALUE = "usd"
    }

    suspend fun getCoinsByIds(ids: String): List<CoinsResponse> = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(COINS_MARKET_DATA_PARAMS_IDS, ids)
        parameter(COINS_MARKET_DATA_PARAMS_KEY, COINS_MARKET_DATA_PARAMS_VALUE)
    }.body()
}
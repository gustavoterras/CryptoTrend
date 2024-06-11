package br.com.terras.app.home.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url

class ApiService(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/markets"
        private const val COINS_MARKET_DATA_PARAMS_KEY = "vs_currency"
        private const val COINS_MARKET_DATA_PARAMS_VALUE = "usd"
    }

    suspend fun getCoins() = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(COINS_MARKET_DATA_PARAMS_KEY, COINS_MARKET_DATA_PARAMS_VALUE)
    }
}
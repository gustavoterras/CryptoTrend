package br.com.terras.app.coins.list.network

import br.com.terras.app.coins.list.data.model.CoinsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import java.util.Locale

class ApiService(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/markets"
        private const val COINS_MARKET_DATA_PARAMS_KEY = "vs_currency"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_EN = "usd"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_PT = "brl"
    }

    private fun Locale.getCurrencyParamData() = when (this.language) {
        "en" -> COINS_MARKET_DATA_PARAMS_VALUE_EN
        else -> COINS_MARKET_DATA_PARAMS_VALUE_PT
    }

    suspend fun getCoins(): List<CoinsResponse> = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(COINS_MARKET_DATA_PARAMS_KEY, Locale.getDefault().getCurrencyParamData())
    }.body()
}
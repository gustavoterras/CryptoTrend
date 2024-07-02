package br.com.terras.app.coins.detail.network

import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import java.util.Locale

class ApiServiceDetail(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/"
        private const val COINS_MARKET_DATA_PARAMS_KEY = "vs_currency"
        private const val COINS_MARKET_DATA_PARAMS_CHART_KEY = "/market_chart"
        private const val COINS_MARKET_DATA_PARAMS_DAYS_KEY = "days"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_EN = "usd"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_PT = "brl"
    }

    private fun Locale.getCurrencyParamData() = when (this.language) {
        "en" -> COINS_MARKET_DATA_PARAMS_VALUE_EN
        else -> COINS_MARKET_DATA_PARAMS_VALUE_PT
    }

    suspend fun getDetailById(id: String): CoinDetailResponse = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE$id")
    }.body()

    suspend fun getChartDetailById(id: String, days: Int): CoinDetailMarketChartResponse = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE$id$COINS_MARKET_DATA_PARAMS_CHART_KEY")
        parameter(COINS_MARKET_DATA_PARAMS_KEY, Locale.getDefault().getCurrencyParamData())
        parameter(COINS_MARKET_DATA_PARAMS_DAYS_KEY, days)
    }.body()
}
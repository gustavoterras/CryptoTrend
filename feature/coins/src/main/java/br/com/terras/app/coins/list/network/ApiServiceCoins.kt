package br.com.terras.app.coins.list.network

import br.com.terras.app.coins.list.data.model.AlternativeFngResponse
import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.GlobalMarketResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import java.util.Locale

class ApiServiceCoins(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/markets"
        private const val COINS_TRENDING_ROUTE = "v3/search/trending"
        private const val GLOBAL_MARKET_DATA_ROUTE = "v3/global"
        private const val FALLBACK_FEAR_GREED_URL = "https://api.alternative.me/fng/?limit=1"
        private const val COINS_MARKET_DATA_PARAMS_KEY = "vs_currency"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_EN = "usd"
        private const val COINS_MARKET_DATA_PARAMS_VALUE_PT = "brl"
        private const val COINS_MARKET_DATA_TOP_LIMIT = 20
    }

    private fun Locale.getCurrencyParamData() = when (this.language) {
        "en" -> COINS_MARKET_DATA_PARAMS_VALUE_EN
        else -> COINS_MARKET_DATA_PARAMS_VALUE_PT
    }

    suspend fun getCoins(page: Int = 1): List<CoinsResponse> = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(COINS_MARKET_DATA_PARAMS_KEY, Locale.getDefault().getCurrencyParamData())
        parameter("order", "market_cap_desc")
        parameter("per_page", COINS_MARKET_DATA_TOP_LIMIT)
        parameter("page", page.coerceAtLeast(1))
        parameter("sparkline", true)
    }.body()

    suspend fun getCoinsByIds(ids: List<String>): List<CoinsResponse> {
        if (ids.isEmpty()) return emptyList()
        val limitedIds = ids.take(COINS_MARKET_DATA_TOP_LIMIT)
        return client.get {
            url("$baseUrl$COINS_MARKET_DATA_ROUTE")
            parameter(COINS_MARKET_DATA_PARAMS_KEY, Locale.getDefault().getCurrencyParamData())
            parameter("ids", limitedIds.joinToString(","))
            parameter("per_page", limitedIds.size.coerceAtLeast(1))
            parameter("page", 1)
            parameter("sparkline", true)
        }.body()
    }

    suspend fun getTrendingCoins(): TrendingCoinsResponse = client.get {
        url("$baseUrl$COINS_TRENDING_ROUTE")
    }.body()

    suspend fun getGlobalMarketData(): GlobalMarketResponse = client.get {
        url("$baseUrl$GLOBAL_MARKET_DATA_ROUTE")
    }.body()

    suspend fun getFallbackFearAndGreed(): AlternativeFngResponse = client.get {
        url(FALLBACK_FEAR_GREED_URL)
    }.body()
}
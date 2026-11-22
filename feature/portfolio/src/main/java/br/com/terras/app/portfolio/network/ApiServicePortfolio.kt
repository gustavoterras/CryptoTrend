package br.com.terras.app.portfolio.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class PortfolioMarketCoinDto(
    @SerialName("id") val id: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("name") val name: String,
    @SerialName("image") val image: String = "",
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null
)

class ApiServicePortfolio(
    private val baseUrl: String,
    private val client: HttpClient
) {

    companion object {
        private const val COINS_MARKET_DATA_ROUTE = "v3/coins/markets"
        private const val PARAM_VS_CURRENCY = "vs_currency"
        private const val PARAM_IDS = "ids"
        private const val PARAM_ORDER = "order"
        private const val PARAM_PER_PAGE = "per_page"
        private const val PARAM_PAGE = "page"
        private const val PARAM_SPARKLINE = "sparkline"
        private const val CURRENCY_EN = "usd"
        private const val CURRENCY_PT = "brl"
    }

    private fun Locale.getCurrencyParam(): String = when (language) {
        "en" -> CURRENCY_EN
        else -> CURRENCY_PT
    }

    suspend fun getMarketQuotesByIds(ids: String): List<PortfolioMarketCoinDto> = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(PARAM_IDS, ids)
        parameter(PARAM_VS_CURRENCY, Locale.getDefault().getCurrencyParam())
        parameter(PARAM_SPARKLINE, false)
    }.body()

    suspend fun getTopMarketCoins(perPage: Int = 25): List<PortfolioMarketCoinDto> = client.get {
        url("$baseUrl$COINS_MARKET_DATA_ROUTE")
        parameter(PARAM_VS_CURRENCY, Locale.getDefault().getCurrencyParam())
        parameter(PARAM_ORDER, "market_cap_desc")
        parameter(PARAM_PER_PAGE, perPage)
        parameter(PARAM_PAGE, 1)
        parameter(PARAM_SPARKLINE, false)
    }.body()
}

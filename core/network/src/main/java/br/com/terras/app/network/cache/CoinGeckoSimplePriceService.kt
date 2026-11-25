package br.com.terras.app.network.cache

import br.com.terras.app.network.di.CoinGeckoBaseUrl
import br.com.terras.app.network.di.CoinGeckoClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class SimpleCoinPrice(
    val price: Double,
    val change24h: Double?,
    val lastUpdatedAtSeconds: Long?
)

interface CoinGeckoSimplePriceService {
    suspend fun getSimplePrices(
        ids: List<String>,
        vsCurrency: String
    ): Map<String, SimpleCoinPrice>
}

object NoOpCoinGeckoSimplePriceService : CoinGeckoSimplePriceService {
    override suspend fun getSimplePrices(
        ids: List<String>,
        vsCurrency: String
    ): Map<String, SimpleCoinPrice> = emptyMap()
}

@Singleton
class CoinGeckoSimplePriceServiceImpl @Inject constructor(
    @CoinGeckoBaseUrl private val baseUrl: String,
    @CoinGeckoClient private val client: HttpClient
) : CoinGeckoSimplePriceService {

    override suspend fun getSimplePrices(
        ids: List<String>,
        vsCurrency: String
    ): Map<String, SimpleCoinPrice> {
        val cleanIds = ids
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        if (cleanIds.isEmpty()) return emptyMap()

        val normalizedCurrency = vsCurrency.lowercase()
        val changeKey = "${normalizedCurrency}_24h_change"
        val aggregated = LinkedHashMap<String, SimpleCoinPrice>(cleanIds.size)

        cleanIds.chunked(MAX_IDS_PER_REQUEST).forEach { batch ->
            val rawObject: JsonObject = client.get {
                url("${baseUrl}$SIMPLE_PRICE_ROUTE")
                parameter(PARAM_IDS, batch.joinToString(","))
                parameter(PARAM_VS_CURRENCIES, normalizedCurrency)
                parameter(PARAM_PRECISION, "full")
                parameter(PARAM_INCLUDE_24HR_CHANGE, true)
                parameter(PARAM_INCLUDE_LAST_UPDATED_AT, true)
            }.body()

            rawObject.forEach { (coinId, element) ->
                val coinObj = runCatching { element.jsonObject }.getOrNull() ?: return@forEach
                val price = coinObj[normalizedCurrency]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                val change24h = coinObj[changeKey]?.jsonPrimitive?.doubleOrNull
                val lastUpdatedAt = coinObj[KEY_LAST_UPDATED_AT]?.jsonPrimitive?.longOrNull
                aggregated[coinId] = SimpleCoinPrice(
                    price = price,
                    change24h = change24h,
                    lastUpdatedAtSeconds = lastUpdatedAt
                )
            }
        }

        return aggregated
    }

    private companion object {
        private const val SIMPLE_PRICE_ROUTE = "v3/simple/price"
        private const val PARAM_IDS = "ids"
        private const val PARAM_VS_CURRENCIES = "vs_currencies"
        private const val PARAM_PRECISION = "precision"
        private const val PARAM_INCLUDE_24HR_CHANGE = "include_24hr_change"
        private const val PARAM_INCLUDE_LAST_UPDATED_AT = "include_last_updated_at"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
        private const val MAX_IDS_PER_REQUEST = 515
    }
}

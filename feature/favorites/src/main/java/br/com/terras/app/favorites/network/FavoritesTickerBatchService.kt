package br.com.terras.app.favorites.network

import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.network.di.WebSocketHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class FavoriteTickerTarget(
    val coinId: String,
    val symbol: String
)

data class FavoriteLiveTicker(
    val coinId: String,
    val price: Double,
    val change24h: Double? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed interface FavoriteTickerBatchEvent {
    data class Connected(val activeProductCount: Int) : FavoriteTickerBatchEvent
    data class Tick(val ticker: FavoriteLiveTicker) : FavoriteTickerBatchEvent
    data object Disconnected : FavoriteTickerBatchEvent
}

interface FavoritesTickerBatchService {
    fun observeTickerBatch(
        targets: List<FavoriteTickerTarget>,
        vsCurrency: String
    ): Flow<FavoriteTickerBatchEvent>
}

object NoOpFavoritesTickerBatchService : FavoritesTickerBatchService {
    override fun observeTickerBatch(
        targets: List<FavoriteTickerTarget>,
        vsCurrency: String
    ): Flow<FavoriteTickerBatchEvent> = emptyFlow()
}

internal sealed interface TickerBatchFrameOutcome {
    data class Subscribed(val productCount: Int) : TickerBatchFrameOutcome
    data class Ticks(val items: List<FavoriteLiveTicker>) : TickerBatchFrameOutcome
    data class Rejected(val reason: String) : TickerBatchFrameOutcome
    data object Ignored : TickerBatchFrameOutcome
}

private class TickerBatchRejectedException(message: String) : RuntimeException(message)

@Singleton
class FavoritesTickerBatchServiceImpl @Inject constructor(
    @WebSocketHttpClient private val wsClient: HttpClient,
    private val simplePriceService: CoinGeckoSimplePriceService,
    private val json: Json
) : FavoritesTickerBatchService {

    @Volatile
    private var cachedOnlineUsdProducts: Set<String> = emptySet()

    @Volatile
    private var productsCacheTimestampMs: Long = 0L

    private val rejectedProducts = ConcurrentHashMap.newKeySet<String>()

    override fun observeTickerBatch(
        targets: List<FavoriteTickerTarget>,
        vsCurrency: String
    ): Flow<FavoriteTickerBatchEvent> = flow {
        val cleanVsCurrency = vsCurrency.trim().lowercase(Locale.US).ifEmpty { DEFAULT_VS_CURRENCY }
        val productToCoinIds = linkedMapOf<String, MutableList<String>>()
        targets.forEach { target ->
            val cleanCoinId = target.coinId.trim().lowercase(Locale.US)
            val cleanSymbol = target.symbol.trim().uppercase(Locale.US)
            if (cleanCoinId.isEmpty() || cleanSymbol.isEmpty() || cleanSymbol == QUOTE_USD) return@forEach
            val productId = "$cleanSymbol-$QUOTE_USD"
            if (productId in rejectedProducts) return@forEach
            productToCoinIds.getOrPut(productId) { mutableListOf() }.add(cleanCoinId)
        }

        if (productToCoinIds.isEmpty()) {
            emit(FavoriteTickerBatchEvent.Disconnected)
            return@flow
        }

        val availableUsdProducts = resolveOnlineUsdProducts()
        val activeProducts = LinkedHashMap<String, List<String>>()
        productToCoinIds.forEach { (productId, coinIds) ->
            val supported = availableUsdProducts == null || productId in availableUsdProducts
            if (supported) {
                activeProducts[productId] = coinIds
            }
        }

        if (activeProducts.isEmpty()) {
            emit(FavoriteTickerBatchEvent.Disconnected)
            return@flow
        }

        var reconnectDelayMs = RECONNECT_DELAY_MS
        while (currentCoroutineContext().isActive && activeProducts.isNotEmpty()) {
            val fxRate = resolveUsdToQuoteRate(cleanVsCurrency)
            if (fxRate == null) {
                emit(FavoriteTickerBatchEvent.Disconnected)
                delay(reconnectDelayMs)
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                continue
            }

            try {
                streamTickerBatchSocket(
                    productToCoinIds = activeProducts,
                    vsCurrency = cleanVsCurrency,
                    initialFxRate = fxRate
                )
                reconnectDelayMs = RECONNECT_DELAY_MS
            } catch (e: CancellationException) {
                throw e
            } catch (e: TickerBatchRejectedException) {
                val invalidProductId = extractInvalidProductId(e.message.orEmpty())
                if (invalidProductId != null && activeProducts.remove(invalidProductId) != null) {
                    rejectedProducts.add(invalidProductId)
                    if (activeProducts.isEmpty()) {
                        emit(FavoriteTickerBatchEvent.Disconnected)
                        return@flow
                    }
                    continue
                }
                emit(FavoriteTickerBatchEvent.Disconnected)
                delay(reconnectDelayMs)
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                continue
            } catch (_: Throwable) {
                emit(FavoriteTickerBatchEvent.Disconnected)
                delay(reconnectDelayMs)
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                continue
            }

            emit(FavoriteTickerBatchEvent.Disconnected)
            delay(reconnectDelayMs)
        }
    }

    private suspend fun FlowCollector<FavoriteTickerBatchEvent>.streamTickerBatchSocket(
        productToCoinIds: Map<String, List<String>>,
        vsCurrency: String,
        initialFxRate: Double
    ) {
        val productIds = productToCoinIds.keys.toList()
        var currentFxRate = initialFxRate
        var lastFxRefreshMillis = System.currentTimeMillis()
        var emittedConnected = false

        wsClient.webSocket(urlString = COINBASE_WS_URL) {
            send(Frame.Text(buildTickerBatchSubscribeFrame(productIds)))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val rawText = frame.readText()
                val now = System.currentTimeMillis()
                if (vsCurrency != DEFAULT_VS_CURRENCY && now - lastFxRefreshMillis >= FX_REFRESH_INTERVAL_MS) {
                    lastFxRefreshMillis = now
                    resolveUsdToQuoteRate(vsCurrency)?.let { updatedRate ->
                        currentFxRate = updatedRate
                    }
                }

                when (
                    val outcome = parseTickerBatchFrame(
                        rawText = rawText,
                        productToCoinIds = productToCoinIds,
                        fxRate = currentFxRate,
                        nowMillis = now
                    )
                ) {
                    is TickerBatchFrameOutcome.Subscribed -> {
                        if (outcome.productCount > 0 && !emittedConnected) {
                            emittedConnected = true
                            emit(FavoriteTickerBatchEvent.Connected(outcome.productCount))
                        }
                    }
                    is TickerBatchFrameOutcome.Ticks -> {
                        if (!emittedConnected) {
                            emittedConnected = true
                            emit(FavoriteTickerBatchEvent.Connected(productIds.size))
                        }
                        outcome.items.forEach { tick ->
                            emit(FavoriteTickerBatchEvent.Tick(tick))
                        }
                    }
                    is TickerBatchFrameOutcome.Rejected -> {
                        throw TickerBatchRejectedException(outcome.reason)
                    }
                    is TickerBatchFrameOutcome.Ignored -> Unit
                }
            }
        }
    }

    private suspend fun resolveOnlineUsdProducts(): Set<String>? {
        val now = System.currentTimeMillis()
        val cached = cachedOnlineUsdProducts
        if (cached.isNotEmpty() && now - productsCacheTimestampMs < PRODUCTS_CACHE_TTL_MS) {
            return cached
        }

        return try {
            val rawBody = wsClient.get(COINBASE_PRODUCTS_URL).bodyAsText()
            val parsed = parseCoinbaseUsdProducts(rawBody)
            if (parsed.isNotEmpty()) {
                cachedOnlineUsdProducts = parsed
                productsCacheTimestampMs = now
                parsed
            } else {
                cached.takeIf { it.isNotEmpty() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            cached.takeIf { it.isNotEmpty() }
        }
    }

    private suspend fun resolveUsdToQuoteRate(vsCurrency: String): Double? {
        if (vsCurrency == DEFAULT_VS_CURRENCY) return 1.0
        return try {
            val quotes = simplePriceService.getSimplePrices(listOf(TETHER_ID), vsCurrency)
            quotes[TETHER_ID]?.price?.takeIf { it.isFinite() && it > 0.0 }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    internal fun parseCoinbaseUsdProducts(rawJson: String): Set<String> {
        val array = runCatching { json.parseToJsonElement(rawJson).jsonArray }.getOrNull()
            ?: return emptySet()
        val result = HashSet<String>()
        for (element in array) {
            val obj = element as? JsonObject ?: continue
            val quoteCurrency = obj["quote_currency"]?.jsonPrimitive?.contentOrNull?.uppercase(Locale.US)
            val status = obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.US)
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.uppercase(Locale.US) ?: continue
            if (quoteCurrency == QUOTE_USD && (status == null || status == "online")) {
                result.add(id)
            }
        }
        return result
    }

    internal fun parseTickerBatchFrame(
        rawText: String,
        productToCoinIds: Map<String, List<String>>,
        fxRate: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): TickerBatchFrameOutcome {
        val root = runCatching { json.parseToJsonElement(rawText).jsonObject }.getOrNull()
            ?: return TickerBatchFrameOutcome.Ignored

        val type = root["type"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.US)
            ?: return TickerBatchFrameOutcome.Ignored

        if (type == "error") {
            val reason = root["reason"]?.jsonPrimitive?.contentOrNull
                ?: root["message"]?.jsonPrimitive?.contentOrNull
                ?: "Coinbase ticker_batch subscription error"
            return TickerBatchFrameOutcome.Rejected(reason)
        }

        if (type == "subscriptions") {
            val channels = root["channels"] as? JsonArray ?: return TickerBatchFrameOutcome.Ignored
            var subscribedCount = 0
            for (channelItem in channels) {
                val obj = channelItem as? JsonObject ?: continue
                val name = obj["name"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.US)
                if (name == CHANNEL_TICKER_BATCH || name == CHANNEL_TICKER_1000) {
                    val products = obj["product_ids"] as? JsonArray
                    subscribedCount += products?.size ?: 0
                }
            }
            return if (subscribedCount > 0) {
                TickerBatchFrameOutcome.Subscribed(subscribedCount)
            } else {
                TickerBatchFrameOutcome.Ignored
            }
        }

        if (type != "ticker") return TickerBatchFrameOutcome.Ignored

        val productId = root["product_id"]?.jsonPrimitive?.contentOrNull?.uppercase(Locale.US)
            ?: return TickerBatchFrameOutcome.Ignored
        val coinIds = productToCoinIds[productId] ?: return TickerBatchFrameOutcome.Ignored

        val usdPrice = root["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: root["price"]?.jsonPrimitive?.doubleOrNull
            ?: return TickerBatchFrameOutcome.Ignored
        if (!usdPrice.isFinite() || usdPrice <= 0.0 || !fxRate.isFinite() || fxRate <= 0.0) {
            return TickerBatchFrameOutcome.Ignored
        }

        val open24h = root["open_24h"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: root["open_24h"]?.jsonPrimitive?.doubleOrNull
        val change24h = if (open24h != null && open24h.isFinite() && open24h > 0.0) {
            (((usdPrice - open24h) / open24h) * 100.0).takeIf { it.isFinite() }
        } else {
            null
        }

        val timestampMs = root["time"]?.jsonPrimitive?.contentOrNull
            ?.let { iso -> runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull() }
            ?: nowMillis

        val convertedPrice = usdPrice * fxRate
        val ticks = coinIds.map { coinId ->
            FavoriteLiveTicker(
                coinId = coinId,
                price = convertedPrice,
                change24h = change24h,
                timestampMillis = timestampMs
            )
        }
        return TickerBatchFrameOutcome.Ticks(ticks)
    }

    internal companion object {
        private const val COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com"
        private const val COINBASE_PRODUCTS_URL = "https://api.exchange.coinbase.com/products"
        private const val CHANNEL_TICKER_BATCH = "ticker_batch"
        private const val CHANNEL_TICKER_1000 = "ticker_1000"
        private const val DEFAULT_VS_CURRENCY = "usd"
        private const val QUOTE_USD = "USD"
        private const val TETHER_ID = "tether"
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RETRY_MS = 30_000L
        private const val FX_REFRESH_INTERVAL_MS = 60_000L
        private const val PRODUCTS_CACHE_TTL_MS = 15 * 60_000L
        private val INVALID_PRODUCT_REGEX = Regex("""\b([A-Z0-9._-]+-USD)\b""", RegexOption.IGNORE_CASE)

        internal fun buildTickerBatchSubscribeFrame(productIds: List<String>): String {
            return buildJsonObject {
                put("type", "subscribe")
                put("product_ids", buildJsonArray {
                    productIds.forEach { add(JsonPrimitive(it)) }
                })
                put("channels", buildJsonArray {
                    add(
                        buildJsonObject {
                            put("name", CHANNEL_TICKER_BATCH)
                            put("product_ids", buildJsonArray {
                                productIds.forEach { add(JsonPrimitive(it)) }
                            })
                        }
                    )
                })
            }.toString()
        }

        internal fun extractInvalidProductId(reason: String): String? {
            if (!reason.contains("valid product", ignoreCase = true) &&
                !reason.contains("not found", ignoreCase = true)
            ) {
                return null
            }
            return INVALID_PRODUCT_REGEX.find(reason)?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
        }
    }
}

private val JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null

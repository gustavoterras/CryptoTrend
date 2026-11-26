package br.com.terras.app.coins.detail.network

import br.com.terras.app.network.cache.CoinGeckoSimplePriceService
import br.com.terras.app.network.di.CoinWebSocketClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LivePriceSource {
    WEBSOCKET,
    REST_POLLING
}

data class LivePriceTick(
    val coinId: String,
    val vsCurrency: String,
    val price: Double,
    val change24h: Double? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val source: LivePriceSource = LivePriceSource.WEBSOCKET
)

interface CoinWebSocketService {
    fun observeLivePrice(
        coinId: String,
        coinSymbol: String,
        vsCurrency: String
    ): Flow<LivePriceTick>
}

sealed interface WebSocketFrameOutcome {
    data class Rejected(val reason: String) : WebSocketFrameOutcome
    data class PriceUpdate(val tick: LivePriceTick) : WebSocketFrameOutcome
    data object Ignored : WebSocketFrameOutcome
}

private class WebSocketRejectedException(message: String) : RuntimeException(message)

@Singleton
class CoinWebSocketServiceImpl @Inject constructor(
    @CoinWebSocketClient private val wsClient: HttpClient,
    private val simplePriceService: CoinGeckoSimplePriceService,
    private val json: Json
) : CoinWebSocketService {

    override fun observeLivePrice(
        coinId: String,
        coinSymbol: String,
        vsCurrency: String
    ): Flow<LivePriceTick> = flow {
        val cleanCoinId = coinId.trim().lowercase(Locale.US)
        val cleanSymbol = coinSymbol.trim().uppercase(Locale.US)
        val cleanVsCurrency = vsCurrency.trim().lowercase(Locale.US).ifEmpty { DEFAULT_VS_CURRENCY }
        if (cleanCoinId.isEmpty()) return@flow

        var coinbaseEnabled = cleanSymbol.isNotEmpty()
        var krakenEnabled = cleanSymbol.isNotEmpty()

        while (currentCoroutineContext().isActive) {
            if (coinbaseEnabled || krakenEnabled) {
                val fxRate = resolveUsdToQuoteRate(cleanCoinId, cleanVsCurrency)
                if (fxRate != null) {
                    if (coinbaseEnabled) {
                        try {
                            streamCoinbaseWebSocket(
                                coinId = cleanCoinId,
                                coinSymbol = cleanSymbol,
                                vsCurrency = cleanVsCurrency,
                                initialFxRate = fxRate
                            )
                            delay(RECONNECT_DELAY_MS)
                            continue
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: WebSocketRejectedException) {
                            coinbaseEnabled = false
                        } catch (_: Throwable) {
                            coinbaseEnabled = false
                        }
                    }

                    if (krakenEnabled) {
                        try {
                            streamKrakenWebSocket(
                                coinId = cleanCoinId,
                                coinSymbol = cleanSymbol,
                                vsCurrency = cleanVsCurrency,
                                initialFxRate = fxRate
                            )
                            delay(RECONNECT_DELAY_MS)
                            continue
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: WebSocketRejectedException) {
                            krakenEnabled = false
                        } catch (_: Throwable) {
                            krakenEnabled = false
                        }
                    }
                }
            }

            // Fallback when neither exchange WS supports the pair or network blocks sockets
            pollRestLiveLoop(cleanCoinId, cleanVsCurrency)
            return@flow
        }
    }

    private suspend fun FlowCollector<LivePriceTick>.streamCoinbaseWebSocket(
        coinId: String,
        coinSymbol: String,
        vsCurrency: String,
        initialFxRate: Double
    ) {
        val productId = "$coinSymbol-USD"
        var currentFxRate = initialFxRate
        var lastFxRefreshMillis = System.currentTimeMillis()
        var lastEmitMillis = 0L

        wsClient.webSocket(urlString = COINBASE_WS_URL) {
            send(Frame.Text(buildCoinbaseSubscribeFrame(productId)))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val rawText = frame.readText()
                val now = System.currentTimeMillis()
                if (vsCurrency != DEFAULT_VS_CURRENCY && now - lastFxRefreshMillis >= FX_REFRESH_INTERVAL_MS) {
                    lastFxRefreshMillis = now
                    resolveUsdToQuoteRate(coinId, vsCurrency)?.let { updatedRate ->
                        currentFxRate = updatedRate
                    }
                }

                when (val outcome = parseCoinbaseFrame(rawText, coinId, vsCurrency, currentFxRate)) {
                    is WebSocketFrameOutcome.PriceUpdate -> {
                        if (now - lastEmitMillis >= MIN_EMIT_INTERVAL_MS) {
                            lastEmitMillis = now
                            emit(outcome.tick)
                        }
                    }
                    is WebSocketFrameOutcome.Rejected -> throw WebSocketRejectedException(outcome.reason)
                    is WebSocketFrameOutcome.Ignored -> Unit
                }
            }
        }
    }

    private suspend fun FlowCollector<LivePriceTick>.streamKrakenWebSocket(
        coinId: String,
        coinSymbol: String,
        vsCurrency: String,
        initialFxRate: Double
    ) {
        val pairSymbol = "$coinSymbol/USD"
        var currentFxRate = initialFxRate
        var lastFxRefreshMillis = System.currentTimeMillis()
        var lastEmitMillis = 0L

        wsClient.webSocket(urlString = KRAKEN_WS_URL) {
            send(Frame.Text(buildKrakenSubscribeFrame(pairSymbol)))

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val rawText = frame.readText()
                val now = System.currentTimeMillis()
                if (vsCurrency != DEFAULT_VS_CURRENCY && now - lastFxRefreshMillis >= FX_REFRESH_INTERVAL_MS) {
                    lastFxRefreshMillis = now
                    resolveUsdToQuoteRate(coinId, vsCurrency)?.let { updatedRate ->
                        currentFxRate = updatedRate
                    }
                }

                when (val outcome = parseKrakenFrame(rawText, coinId, vsCurrency, currentFxRate)) {
                    is WebSocketFrameOutcome.PriceUpdate -> {
                        if (now - lastEmitMillis >= MIN_EMIT_INTERVAL_MS) {
                            lastEmitMillis = now
                            emit(outcome.tick)
                        }
                    }
                    is WebSocketFrameOutcome.Rejected -> throw WebSocketRejectedException(outcome.reason)
                    is WebSocketFrameOutcome.Ignored -> Unit
                }
            }
        }
    }

    private suspend fun resolveUsdToQuoteRate(
        coinId: String,
        vsCurrency: String
    ): Double? {
        if (vsCurrency == DEFAULT_VS_CURRENCY) return 1.0
        return try {
            val quotePrices = simplePriceService.getSimplePrices(listOf(coinId, TETHER_ID), vsCurrency)
            val usdPrices = try {
                simplePriceService.getSimplePrices(listOf(coinId), DEFAULT_VS_CURRENCY)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                emptyMap()
            }
            val coinQuote = quotePrices[coinId]?.price?.takeIf { it.isFinite() && it > 0.0 }
            val coinUsd = usdPrices[coinId]?.price?.takeIf { it.isFinite() && it > 0.0 }
            if (coinQuote != null && coinUsd != null) {
                coinQuote / coinUsd
            } else {
                quotePrices[TETHER_ID]?.price?.takeIf { it.isFinite() && it > 0.0 }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun FlowCollector<LivePriceTick>.pollRestLiveLoop(
        coinId: String,
        vsCurrency: String
    ) {
        var pollDelayMs = REST_POLL_INTERVAL_MS
        while (currentCoroutineContext().isActive) {
            val succeeded = pollRestTick(coinId, vsCurrency)
            pollDelayMs = if (succeeded) {
                REST_POLL_INTERVAL_MS
            } else {
                (pollDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
            }
            delay(pollDelayMs)
        }
    }

    private suspend fun FlowCollector<LivePriceTick>.pollRestTick(
        coinId: String,
        vsCurrency: String
    ): Boolean {
        return try {
            val prices = simplePriceService.getSimplePrices(listOf(coinId), vsCurrency)
            val result = prices[coinId] ?: return false
            val validPrice = result.price.takeIf { it.isFinite() && it > 0.0 } ?: return false
            val validChange24h = result.change24h?.takeIf { it.isFinite() }
            val timestampMs = result.lastUpdatedAtSeconds
                ?.takeIf { it > 0L }
                ?.times(1_000L)
                ?: System.currentTimeMillis()
            emit(
                LivePriceTick(
                    coinId = coinId,
                    vsCurrency = vsCurrency,
                    price = validPrice,
                    change24h = validChange24h,
                    timestampMillis = timestampMs,
                    source = LivePriceSource.REST_POLLING
                )
            )
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
    }

    internal fun parseCoinbaseFrame(
        rawText: String,
        coinId: String,
        vsCurrency: String,
        fxRate: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): WebSocketFrameOutcome {
        val root = runCatching { json.parseToJsonElement(rawText).jsonObject }.getOrNull()
            ?: return WebSocketFrameOutcome.Ignored

        val type = root["type"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.US)
            ?: return WebSocketFrameOutcome.Ignored

        if (type == "error") {
            val reason = root["reason"]?.jsonPrimitive?.contentOrNull
                ?: root["message"]?.jsonPrimitive?.contentOrNull
                ?: "Coinbase subscription error"
            return WebSocketFrameOutcome.Rejected(reason)
        }
        if (type != "ticker") return WebSocketFrameOutcome.Ignored

        val usdPrice = root["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: root["price"]?.jsonPrimitive?.doubleOrNull
            ?: return WebSocketFrameOutcome.Ignored
        if (!usdPrice.isFinite() || usdPrice <= 0.0 || !fxRate.isFinite() || fxRate <= 0.0) {
            return WebSocketFrameOutcome.Ignored
        }

        val open24h = root["open_24h"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: root["open_24h"]?.jsonPrimitive?.doubleOrNull
        val change24h = if (open24h != null && open24h.isFinite() && open24h > 0.0) {
            ((usdPrice - open24h) / open24h) * 100.0
        } else {
            null
        }

        val timestampMs = root["time"]?.jsonPrimitive?.contentOrNull
            ?.let { iso -> runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull() }
            ?: nowMillis

        return WebSocketFrameOutcome.PriceUpdate(
            LivePriceTick(
                coinId = coinId,
                vsCurrency = vsCurrency,
                price = usdPrice * fxRate,
                change24h = change24h?.takeIf { it.isFinite() },
                timestampMillis = timestampMs,
                source = LivePriceSource.WEBSOCKET
            )
        )
    }

    internal fun parseKrakenFrame(
        rawText: String,
        coinId: String,
        vsCurrency: String,
        fxRate: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): WebSocketFrameOutcome {
        val root = runCatching { json.parseToJsonElement(rawText).jsonObject }.getOrNull()
            ?: return WebSocketFrameOutcome.Ignored

        val error = root["error"]?.jsonPrimitive?.contentOrNull
        if (!error.isNullOrBlank()) {
            return WebSocketFrameOutcome.Rejected(error)
        }

        val success = root["success"]?.jsonPrimitive?.booleanOrNull
        if (success == false) {
            return WebSocketFrameOutcome.Rejected("Kraken subscription failed")
        }

        val channel = root["channel"]?.jsonPrimitive?.contentOrNull
        if (channel != "ticker") return WebSocketFrameOutcome.Ignored

        val dataArray = root["data"] as? JsonArray ?: return WebSocketFrameOutcome.Ignored
        val firstObj = dataArray.firstOrNull() as? JsonObject ?: return WebSocketFrameOutcome.Ignored

        val usdPrice = firstObj["last"]?.jsonPrimitive?.doubleOrNull
            ?: firstObj["last"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: return WebSocketFrameOutcome.Ignored
        if (!usdPrice.isFinite() || usdPrice <= 0.0 || !fxRate.isFinite() || fxRate <= 0.0) {
            return WebSocketFrameOutcome.Ignored
        }

        val changePct = firstObj["change_pct"]?.jsonPrimitive?.doubleOrNull
            ?: firstObj["change_pct"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

        val timestampMs = firstObj["timestamp"]?.jsonPrimitive?.contentOrNull
            ?.let { iso -> runCatching { Instant.parse(iso).toEpochMilli() }.getOrNull() }
            ?: nowMillis

        return WebSocketFrameOutcome.PriceUpdate(
            LivePriceTick(
                coinId = coinId,
                vsCurrency = vsCurrency,
                price = usdPrice * fxRate,
                change24h = changePct?.takeIf { it.isFinite() },
                timestampMillis = timestampMs,
                source = LivePriceSource.WEBSOCKET
            )
        )
    }

    companion object {
        private const val COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com"
        private const val KRAKEN_WS_URL = "wss://ws.kraken.com/v2"
        private const val DEFAULT_VS_CURRENCY = "usd"
        private const val TETHER_ID = "tether"
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RETRY_MS = 30_000L
        private const val REST_POLL_INTERVAL_MS = 5_000L
        private const val FX_REFRESH_INTERVAL_MS = 60_000L
        private const val MIN_EMIT_INTERVAL_MS = 400L

        internal fun buildCoinbaseSubscribeFrame(productId: String): String {
            return buildJsonObject {
                put("type", "subscribe")
                put("product_ids", buildJsonArray { add(JsonPrimitive(productId)) })
                put("channels", buildJsonArray { add(JsonPrimitive("ticker")) })
            }.toString()
        }

        internal fun buildKrakenSubscribeFrame(pairSymbol: String): String {
            return buildJsonObject {
                put("method", "subscribe")
                put(
                    "params",
                    buildJsonObject {
                        put("channel", "ticker")
                        put("symbol", buildJsonArray { add(JsonPrimitive(pairSymbol)) })
                    }
                )
            }.toString()
        }
    }
}

private val JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null

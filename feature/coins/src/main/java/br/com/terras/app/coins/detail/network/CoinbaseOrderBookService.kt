package br.com.terras.app.coins.detail.network

import br.com.terras.app.coins.detail.domain.model.OrderBookEntryVO
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplier
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplierOptionVO
import br.com.terras.app.coins.detail.domain.model.OrderBookSnapshotVO
import br.com.terras.app.common.formatLivePrice
import br.com.terras.app.network.di.WebSocketHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Collections
import java.util.Locale
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

sealed interface OrderBookStreamEvent {
    data class Snapshot(val data: OrderBookSnapshotVO) : OrderBookStreamEvent
    data object Unavailable : OrderBookStreamEvent
}

interface CoinbaseOrderBookService {
    suspend fun isPairAvailable(coinSymbol: String): Boolean = true
    fun observeOrderBook(
        coinSymbol: String,
        multiplier: StateFlow<OrderBookMultiplier> = MutableStateFlow(OrderBookMultiplier.X1)
    ): Flow<OrderBookStreamEvent>
}

internal sealed interface OrderBookFrameOutcome {
    data object SnapshotApplied : OrderBookFrameOutcome
    data object DeltaApplied : OrderBookFrameOutcome
    data class Rejected(val reason: String) : OrderBookFrameOutcome
    data object Ignored : OrderBookFrameOutcome
}

private class OrderBookRejectedException(message: String) : RuntimeException(message)

internal class OrderBookAggregator(
    private val pairSymbol: String,
    private val maxLevels: Int = DEFAULT_MAX_LEVELS,
    private val priceLocale: Locale = Locale.US
) {
    private val bids: TreeMap<Double, Double> = TreeMap(Collections.reverseOrder())
    private val asks: TreeMap<Double, Double> = TreeMap()

    fun applySnapshot(rawBids: JsonArray?, rawAsks: JsonArray?) {
        bids.clear()
        asks.clear()
        parseLevelsInto(rawBids, bids)
        parseLevelsInto(rawAsks, asks)
    }

    fun applyChanges(rawChanges: JsonArray?): Boolean {
        if (rawChanges == null || rawChanges.isEmpty()) return false
        var changed = false
        for (item in rawChanges) {
            val tuple = item as? JsonArray ?: continue
            if (tuple.size < 3) continue
            val side = (tuple[0] as? JsonPrimitive)?.contentOrNull?.lowercase(Locale.US) ?: continue
            val price = parsePositiveFiniteDouble(tuple[1] as? JsonPrimitive) ?: continue
            val size = parseNonNegativeFiniteDouble(tuple[2] as? JsonPrimitive) ?: continue

            val book = when (side) {
                "buy", "bid" -> bids
                "sell", "ask" -> asks
                else -> continue
            }

            if (size <= 0.0) {
                if (book.remove(price) != null) {
                    changed = true
                }
            } else {
                val previous = book.put(price, size)
                if (previous != size) {
                    changed = true
                }
            }
        }
        return changed
    }

    fun buildSnapshot(
        multiplier: OrderBookMultiplier = OrderBookMultiplier.X1,
        isLive: Boolean = true
    ): OrderBookSnapshotVO? {
        if (bids.isEmpty() || asks.isEmpty()) return null

        val rawBestBid = bids.firstKey()
        val rawBestAsk = asks.firstKey()
        val midPrice = (rawBestBid + rawBestAsk) / 2.0
        val baseTick = resolveBaseTick(midPrice)
        val availableOptions = buildAvailableMultipliers(midPrice = midPrice, baseTick = baseTick)
        val effectiveOption = availableOptions.firstOrNull { it.multiplier == multiplier }
            ?: availableOptions.lastOrNull()
            ?: OrderBookMultiplierOptionVO(OrderBookMultiplier.X1, formatStepPrice(baseTick))
        val effectiveMultiplier = effectiveOption.multiplier
        val step = baseTick * effectiveMultiplier.factor

        val topBidsRaw = if (effectiveMultiplier == OrderBookMultiplier.X1) {
            bids.entries.take(maxLevels).map { it.key to it.value }
        } else {
            aggregateSide(
                source = bids,
                step = step,
                roundingMode = RoundingMode.FLOOR
            )
        }

        val topAsksRaw = if (effectiveMultiplier == OrderBookMultiplier.X1) {
            asks.entries.take(maxLevels).map { it.key to it.value }
        } else {
            aggregateSide(
                source = asks,
                step = step,
                roundingMode = RoundingMode.CEILING
            )
        }

        if (topBidsRaw.isEmpty() || topAsksRaw.isEmpty()) return null

        var bidCumulative = 0.0
        val bidEntriesTemp = topBidsRaw.map { (price, size) ->
            bidCumulative += size
            Triple(price, size, bidCumulative)
        }

        var askCumulative = 0.0
        val askEntriesTemp = topAsksRaw.map { (price, size) ->
            askCumulative += size
            Triple(price, size, askCumulative)
        }

        val maxCumulative = max(bidCumulative, askCumulative).coerceAtLeast(1e-12)

        val bidEntries = bidEntriesTemp.map { (price, size, cumulative) ->
            OrderBookEntryVO(
                price = price,
                priceFormatted = if (effectiveMultiplier == OrderBookMultiplier.X1) {
                    formatBookPrice(price)
                } else {
                    formatGroupedPrice(price, step)
                },
                size = size,
                sizeFormatted = formatBookSize(size),
                cumulativeTotal = cumulative,
                depthPercentage = (cumulative / maxCumulative).toFloat().coerceIn(MIN_DEPTH_BAR_FRACTION, 1f)
            )
        }

        val askEntries = askEntriesTemp.map { (price, size, cumulative) ->
            OrderBookEntryVO(
                price = price,
                priceFormatted = if (effectiveMultiplier == OrderBookMultiplier.X1) {
                    formatBookPrice(price)
                } else {
                    formatGroupedPrice(price, step)
                },
                size = size,
                sizeFormatted = formatBookSize(size),
                cumulativeTotal = cumulative,
                depthPercentage = (cumulative / maxCumulative).toFloat().coerceIn(MIN_DEPTH_BAR_FRACTION, 1f)
            )
        }

        val spreadAbsolute = (rawBestAsk - rawBestBid).coerceAtLeast(0.0)
        val spreadPercentage = if (rawBestAsk > 0.0) {
            (spreadAbsolute / rawBestAsk) * 100.0
        } else {
            0.0
        }

        return OrderBookSnapshotVO(
            pairSymbol = pairSymbol,
            bids = bidEntries,
            asks = askEntries,
            spreadAbsolute = spreadAbsolute,
            spreadAbsoluteFormatted = formatBookPrice(spreadAbsolute),
            spreadPercentageFormatted = formatSpreadPercentage(spreadPercentage),
            isLive = isLive,
            selectedMultiplier = effectiveMultiplier,
            selectedStepFormatted = effectiveOption.stepFormatted,
            availableMultipliers = availableOptions
        )
    }

    private fun aggregateSide(
        source: TreeMap<Double, Double>,
        step: Double,
        roundingMode: RoundingMode
    ): List<Pair<Double, Double>> {
        val stepBd = BigDecimal.valueOf(step)
        if (stepBd <= BigDecimal.ZERO) return source.entries.take(maxLevels).map { it.key to it.value }

        val buckets = linkedMapOf<Double, Double>()
        for ((price, size) in source) {
            val bucketPrice = BigDecimal.valueOf(price)
                .divide(stepBd, 0, roundingMode)
                .multiply(stepBd)
                .toDouble()
            val existing = buckets[bucketPrice]
            if (existing == null) {
                if (buckets.size >= maxLevels) {
                    break
                }
                buckets[bucketPrice] = size
            } else {
                buckets[bucketPrice] = existing + size
            }
        }
        return buckets.entries.map { it.key to it.value }
    }

    private fun resolveBaseTick(midPrice: Double): Double = when {
        midPrice >= 10.0 -> 0.01
        midPrice >= 0.1 -> 0.0001
        midPrice >= 0.001 -> 0.000001
        else -> 0.00000001
    }

    private fun buildAvailableMultipliers(
        midPrice: Double,
        baseTick: Double
    ): List<OrderBookMultiplierOptionVO> {
        val maxReasonableStep = (midPrice * 0.15).coerceAtLeast(baseTick * 10.0)
        return OrderBookMultiplier.entries
            .filter { option ->
                option == OrderBookMultiplier.X1 || (baseTick * option.factor) <= maxReasonableStep
            }
            .map { option ->
                OrderBookMultiplierOptionVO(
                    multiplier = option,
                    stepFormatted = formatStepPrice(baseTick * option.factor)
                )
            }
    }

    private fun parseLevelsInto(levels: JsonArray?, target: TreeMap<Double, Double>) {
        if (levels == null) return
        for (entry in levels) {
            val pair = entry as? JsonArray ?: continue
            if (pair.size < 2) continue
            val price = parsePositiveFiniteDouble(pair[0] as? JsonPrimitive) ?: continue
            val size = parsePositiveFiniteDouble(pair[1] as? JsonPrimitive) ?: continue
            target[price] = size
        }
    }

    private fun formatBookPrice(value: Double): String {
        return value.formatLivePrice(priceLocale)
    }

    private fun formatGroupedPrice(value: Double, step: Double): String {
        val safe = if (value.isFinite() && value >= 0.0) value else 0.0
        val formatter = DecimalFormat.getCurrencyInstance(priceLocale)
        val decimals = decimalsForStep(step)
        formatter.maximumFractionDigits = decimals
        formatter.minimumFractionDigits = decimals
        return formatter.format(safe)
    }

    private fun formatStepPrice(step: Double): String {
        val formatter = DecimalFormat.getCurrencyInstance(priceLocale)
        val decimals = decimalsForStep(step)
        formatter.maximumFractionDigits = decimals
        formatter.minimumFractionDigits = decimals
        return formatter.format(step)
    }

    private fun decimalsForStep(step: Double): Int = when {
        step >= 1.0 -> 0
        step >= 0.01 -> 2
        step >= 0.001 -> 3
        step >= 0.0001 -> 4
        step >= 0.00001 -> 5
        step >= 0.000001 -> 6
        else -> 8
    }

    private fun formatBookSize(size: Double): String {
        val safe = if (size.isFinite() && size >= 0.0) size else 0.0
        val absValue = abs(safe)
        val symbols = DecimalFormatSymbols(priceLocale)
        val pattern = when {
            absValue >= 1_000.0 -> "#,##0.0"
            absValue >= 10.0 -> "#,##0.00"
            absValue >= 1.0 -> "#,##0.000"
            absValue >= 0.01 -> "#,##0.0000"
            else -> "#,##0.000000"
        }
        return DecimalFormat(pattern, symbols).format(safe)
    }

    private fun formatSpreadPercentage(percentage: Double): String {
        val safe = if (percentage.isFinite() && percentage >= 0.0) percentage else 0.0
        val symbols = DecimalFormatSymbols(priceLocale)
        return "${DecimalFormat("0.00", symbols).format(safe)}%"
    }

    private fun parsePositiveFiniteDouble(primitive: JsonPrimitive?): Double? {
        val value = primitive?.contentOrNull?.toDoubleOrNull()
            ?: primitive?.content?.toDoubleOrNull()
            ?: return null
        return value.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun parseNonNegativeFiniteDouble(primitive: JsonPrimitive?): Double? {
        val value = primitive?.contentOrNull?.toDoubleOrNull()
            ?: primitive?.content?.toDoubleOrNull()
            ?: return null
        return value.takeIf { it.isFinite() && it >= 0.0 }
    }

    companion object {
        const val DEFAULT_MAX_LEVELS = 8
        private const val MIN_DEPTH_BAR_FRACTION = 0.04f
    }
}

@Singleton
class CoinbaseOrderBookServiceImpl @Inject constructor(
    @WebSocketHttpClient private val wsClient: HttpClient,
    private val json: Json
) : CoinbaseOrderBookService {

    override suspend fun isPairAvailable(coinSymbol: String): Boolean {
        val cleanSymbol = coinSymbol.trim().uppercase(Locale.US)
        if (cleanSymbol.isEmpty() || cleanSymbol == QUOTE_USD) return false
        val productId = "$cleanSymbol-$QUOTE_USD"
        return try {
            val response = wsClient.get("$COINBASE_REST_PRODUCTS_URL/$productId")
            response.status.value in 200..299
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            e.response.status != HttpStatusCode.NotFound
        } catch (_: Throwable) {
            true
        }
    }

    override fun observeOrderBook(
        coinSymbol: String,
        multiplier: StateFlow<OrderBookMultiplier>
    ): Flow<OrderBookStreamEvent> = channelFlow {
        val cleanSymbol = coinSymbol.trim().uppercase(Locale.US)
        if (cleanSymbol.isEmpty() || cleanSymbol == QUOTE_USD) {
            send(OrderBookStreamEvent.Unavailable)
            return@channelFlow
        }

        val productId = "$cleanSymbol-$QUOTE_USD"
        val aggregator = OrderBookAggregator(pairSymbol = productId)
        val stateMutex = Mutex()
        var hasDirtyUpdate = false
        var hasEverReceivedSnapshot = false
        var initialConnectFailures = 0
        var activeMultiplier = multiplier.value

        val multiplierJob = launch {
            multiplier.collectLatest { nextMultiplier ->
                val snapshot = stateMutex.withLock {
                    activeMultiplier = nextMultiplier
                    if (hasEverReceivedSnapshot) {
                        hasDirtyUpdate = false
                        aggregator.buildSnapshot(multiplier = nextMultiplier, isLive = true)
                    } else {
                        null
                    }
                }
                if (snapshot != null) {
                    send(OrderBookStreamEvent.Snapshot(snapshot))
                }
            }
        }

        val tickerJob = launch {
            while (isActive) {
                delay(EMIT_THROTTLE_MS)
                val snapshot = stateMutex.withLock {
                    if (hasDirtyUpdate) {
                        hasDirtyUpdate = false
                        aggregator.buildSnapshot(multiplier = activeMultiplier, isLive = true)
                    } else {
                        null
                    }
                }
                if (snapshot != null) {
                    send(OrderBookStreamEvent.Snapshot(snapshot))
                }
            }
        }

        try {
            var reconnectDelayMs = RECONNECT_DELAY_MS
            while (currentCoroutineContext().isActive) {
                try {
                    wsClient.webSocket(urlString = COINBASE_WS_URL) {
                        send(Frame.Text(buildSubscribeFrame(productId)))
                        reconnectDelayMs = RECONNECT_DELAY_MS

                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val outcome = processRawFrame(
                                rawText = frame.readText(),
                                productId = productId,
                                aggregator = aggregator,
                                mutex = stateMutex
                            )
                            when (outcome) {
                                is OrderBookFrameOutcome.SnapshotApplied -> {
                                    hasEverReceivedSnapshot = true
                                    initialConnectFailures = 0
                                    val snapshot = stateMutex.withLock {
                                        hasDirtyUpdate = false
                                        aggregator.buildSnapshot(multiplier = activeMultiplier, isLive = true)
                                    }
                                    if (snapshot != null) {
                                        send(OrderBookStreamEvent.Snapshot(snapshot))
                                    }
                                }
                                is OrderBookFrameOutcome.DeltaApplied -> {
                                    stateMutex.withLock {
                                        hasDirtyUpdate = true
                                    }
                                }
                                is OrderBookFrameOutcome.Rejected -> {
                                    throw OrderBookRejectedException(outcome.reason)
                                }
                                is OrderBookFrameOutcome.Ignored -> Unit
                            }
                        }
                    }

                    if (!hasEverReceivedSnapshot) {
                        initialConnectFailures++
                        if (initialConnectFailures >= MAX_INITIAL_CONNECT_FAILURES) {
                            send(OrderBookStreamEvent.Unavailable)
                            return@channelFlow
                        }
                    }
                    delay(reconnectDelayMs)
                    reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: OrderBookRejectedException) {
                    send(OrderBookStreamEvent.Unavailable)
                    return@channelFlow
                } catch (_: Throwable) {
                    if (!hasEverReceivedSnapshot) {
                        initialConnectFailures++
                        if (initialConnectFailures >= MAX_INITIAL_CONNECT_FAILURES) {
                            send(OrderBookStreamEvent.Unavailable)
                            return@channelFlow
                        }
                    }
                    delay(reconnectDelayMs)
                    reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                }
            }
        } finally {
            tickerJob.cancel()
            multiplierJob.cancel()
        }
    }

    internal suspend fun processRawFrame(
        rawText: String,
        productId: String,
        aggregator: OrderBookAggregator,
        mutex: Mutex
    ): OrderBookFrameOutcome {
        val root = runCatching { json.parseToJsonElement(rawText).jsonObject }.getOrNull()
            ?: return OrderBookFrameOutcome.Ignored
        val type = root["type"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.US)
            ?: return OrderBookFrameOutcome.Ignored

        return when (type) {
            "error" -> {
                val reason = root["reason"]?.jsonPrimitive?.contentOrNull
                    ?: root["message"]?.jsonPrimitive?.contentOrNull
                    ?: "Coinbase order book error"
                OrderBookFrameOutcome.Rejected(reason)
            }
            "snapshot" -> {
                val frameProduct = root["product_id"]?.jsonPrimitive?.contentOrNull
                if (!frameProduct.isNullOrBlank() && !frameProduct.equals(productId, ignoreCase = true)) {
                    return OrderBookFrameOutcome.Ignored
                }
                val rawBids = root["bids"] as? JsonArray
                val rawAsks = root["asks"] as? JsonArray
                mutex.withLock {
                    aggregator.applySnapshot(rawBids, rawAsks)
                }
                OrderBookFrameOutcome.SnapshotApplied
            }
            "l2update" -> {
                val frameProduct = root["product_id"]?.jsonPrimitive?.contentOrNull
                if (!frameProduct.isNullOrBlank() && !frameProduct.equals(productId, ignoreCase = true)) {
                    return OrderBookFrameOutcome.Ignored
                }
                val rawChanges = root["changes"] as? JsonArray
                val changed = mutex.withLock {
                    aggregator.applyChanges(rawChanges)
                }
                if (changed) OrderBookFrameOutcome.DeltaApplied else OrderBookFrameOutcome.Ignored
            }
            else -> OrderBookFrameOutcome.Ignored
        }
    }

    companion object {
        private const val COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com"
        private const val COINBASE_REST_PRODUCTS_URL = "https://api.exchange.coinbase.com/products"
        private const val QUOTE_USD = "USD"
        private const val EMIT_THROTTLE_MS = 100L
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RETRY_MS = 30_000L
        private const val MAX_INITIAL_CONNECT_FAILURES = 2

        internal fun buildSubscribeFrame(productId: String): String {
            return buildJsonObject {
                put("type", "subscribe")
                put("product_ids", buildJsonArray { add(JsonPrimitive(productId)) })
                put("channels", buildJsonArray { add(JsonPrimitive("level2_batch")) })
            }.toString()
        }
    }
}

private val JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null

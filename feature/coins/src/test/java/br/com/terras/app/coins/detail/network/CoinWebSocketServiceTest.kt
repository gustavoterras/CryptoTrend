package br.com.terras.app.coins.detail.network

import br.com.terras.app.network.cache.CoinGeckoSimplePriceService
import br.com.terras.app.network.cache.SimpleCoinPrice
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test

class CoinWebSocketServiceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val simplePriceService: CoinGeckoSimplePriceService = mockk()
    private val service = CoinWebSocketServiceImpl(
        wsClient = HttpClient(CIO),
        simplePriceService = simplePriceService,
        json = json
    )

    @Test
    fun `buildCoinbaseSubscribeFrame formats ticker subscription for product`() {
        val frame = CoinWebSocketServiceImpl.buildCoinbaseSubscribeFrame("BTC-USD")
        val obj = json.parseToJsonElement(frame).jsonObject
        Assert.assertEquals("subscribe", obj["type"]?.jsonPrimitive?.content)
        Assert.assertEquals("BTC-USD", obj["product_ids"]?.jsonArray?.first()?.jsonPrimitive?.content)
        Assert.assertEquals("ticker", obj["channels"]?.jsonArray?.first()?.jsonPrimitive?.content)
    }

    @Test
    fun `buildKrakenSubscribeFrame formats v2 ticker subscription for pair`() {
        val frame = CoinWebSocketServiceImpl.buildKrakenSubscribeFrame("ETH/USD")
        val obj = json.parseToJsonElement(frame).jsonObject
        Assert.assertEquals("subscribe", obj["method"]?.jsonPrimitive?.content)
        val params = obj["params"]?.jsonObject
        Assert.assertEquals("ticker", params?.get("channel")?.jsonPrimitive?.content)
        Assert.assertEquals("ETH/USD", params?.get("symbol")?.jsonArray?.first()?.jsonPrimitive?.content)
    }

    @Test
    fun `parseCoinbaseFrame parses ticker updates applies FX conversion and rejects unsupported products`() {
        val tickerJson = """{"type":"ticker","product_id":"BTC-USD","price":"90000.00","open_24h":"85000.00","time":"2026-11-26T10:00:00Z"}"""
        val outcome = service.parseCoinbaseFrame(tickerJson, "bitcoin", "brl", fxRate = 5.5)
        Assert.assertTrue(outcome is WebSocketFrameOutcome.PriceUpdate)
        val tick = (outcome as WebSocketFrameOutcome.PriceUpdate).tick
        Assert.assertEquals("bitcoin", tick.coinId)
        Assert.assertEquals("brl", tick.vsCurrency)
        Assert.assertEquals(495000.0, tick.price, 0.01)
        Assert.assertEquals(((90000.0 - 85000.0) / 85000.0) * 100.0, tick.change24h!!, 0.001)
        Assert.assertEquals(LivePriceSource.WEBSOCKET, tick.source)

        val errorJson = """{"type":"error","message":"Failed to subscribe","reason":"XYZ-USD is not a valid product"}"""
        Assert.assertTrue(
            service.parseCoinbaseFrame(errorJson, "xyz", "usd", fxRate = 1.0) is WebSocketFrameOutcome.Rejected
        )
    }

    @Test
    fun `parseKrakenFrame parses ticker updates and rejects failed subscriptions`() {
        val krakenJson = """{"channel":"ticker","type":"update","data":[{"symbol":"BTC/USD","last":91250.5,"change_pct":2.75,"timestamp":"2026-11-26T10:00:05Z"}]}"""
        val outcome = service.parseKrakenFrame(krakenJson, "bitcoin", "usd", fxRate = 1.0)
        Assert.assertTrue(outcome is WebSocketFrameOutcome.PriceUpdate)
        val tick = (outcome as WebSocketFrameOutcome.PriceUpdate).tick
        Assert.assertEquals(91250.5, tick.price, 0.001)
        Assert.assertEquals(2.75, tick.change24h!!, 0.001)
        Assert.assertEquals(LivePriceSource.WEBSOCKET, tick.source)

        val rejectedJson = """{"method":"subscribe","success":false,"error":"Currency pair not supported"}"""
        Assert.assertTrue(
            service.parseKrakenFrame(rejectedJson, "unknown", "usd", fxRate = 1.0) is WebSocketFrameOutcome.Rejected
        )
    }

    @Test
    fun `parseCoinbaseFrame and parseKrakenFrame ignore non-ticker or invalid price frames`() {
        val subscriptionAck = """{"type":"subscriptions","channels":[{"name":"ticker","product_ids":["BTC-USD"]}]}"""
        Assert.assertEquals(
            WebSocketFrameOutcome.Ignored,
            service.parseCoinbaseFrame(subscriptionAck, "bitcoin", "usd", fxRate = 1.0)
        )

        val zeroPriceCoinbase = """{"type":"ticker","product_id":"BTC-USD","price":"0.0"}"""
        Assert.assertEquals(
            WebSocketFrameOutcome.Ignored,
            service.parseCoinbaseFrame(zeroPriceCoinbase, "bitcoin", "usd", fxRate = 1.0)
        )

        val zeroPriceKraken = """{"channel":"ticker","type":"update","data":[{"symbol":"BTC/USD","last":-5.0}]}"""
        Assert.assertEquals(
            WebSocketFrameOutcome.Ignored,
            service.parseKrakenFrame(zeroPriceKraken, "bitcoin", "usd", fxRate = 1.0)
        )
    }

    @Test
    fun `observeLivePrice falls back to REST simple price polling when WS symbol unavailable`() = runBlocking {
        coEvery {
            simplePriceService.getSimplePrices(listOf("bitcoin"), "usd")
        } returns mapOf(
            "bitcoin" to SimpleCoinPrice(
                price = 97500.25,
                change24h = 1.85,
                lastUpdatedAtSeconds = 1700000200L
            )
        )

        val tick = service.observeLivePrice("bitcoin", "", "usd").first()
        Assert.assertEquals("bitcoin", tick.coinId)
        Assert.assertEquals("usd", tick.vsCurrency)
        Assert.assertEquals(97500.25, tick.price, 0.0001)
        Assert.assertEquals(1.85, tick.change24h!!, 0.0001)
        Assert.assertEquals(1700000200_000L, tick.timestampMillis)
        Assert.assertEquals(LivePriceSource.REST_POLLING, tick.source)
    }
}

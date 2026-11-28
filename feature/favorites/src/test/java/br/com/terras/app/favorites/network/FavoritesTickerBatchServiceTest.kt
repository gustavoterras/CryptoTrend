package br.com.terras.app.favorites.network

import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test

class FavoritesTickerBatchServiceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val simplePriceService: CoinGeckoSimplePriceService = mockk(relaxed = true)
    private val service = FavoritesTickerBatchServiceImpl(
        wsClient = HttpClient(CIO),
        simplePriceService = simplePriceService,
        json = json
    )

    @Test
    fun `buildTickerBatchSubscribeFrame formats Coinbase ticker_batch subscription`() {
        val frame = FavoritesTickerBatchServiceImpl.buildTickerBatchSubscribeFrame(
            listOf("BTC-USD", "ETH-USD")
        )
        val obj = json.parseToJsonElement(frame).jsonObject
        Assert.assertEquals("subscribe", obj["type"]?.jsonPrimitive?.content)

        val topProducts = obj["product_ids"]?.jsonArray?.map { it.jsonPrimitive.content }
        Assert.assertEquals(listOf("BTC-USD", "ETH-USD"), topProducts)

        val channelObj = obj["channels"]?.jsonArray?.first()?.jsonObject
        Assert.assertEquals("ticker_batch", channelObj?.get("name")?.jsonPrimitive?.content)
        val channelProducts = channelObj?.get("product_ids")?.jsonArray?.map { it.jsonPrimitive.content }
        Assert.assertEquals(listOf("BTC-USD", "ETH-USD"), channelProducts)
    }

    @Test
    fun `parseTickerBatchFrame handles subscriptions ack, ticker updates with FX, and errors`() {
        val ackJson = """
            {
              "type": "subscriptions",
              "channels": [
                { "name": "ticker_batch", "product_ids": ["BTC-USD", "ETH-USD"] }
              ]
            }
        """.trimIndent()
        val ackOutcome = service.parseTickerBatchFrame(
            rawText = ackJson,
            productToCoinIds = mapOf("BTC-USD" to listOf("bitcoin"), "ETH-USD" to listOf("ethereum")),
            fxRate = 5.5
        )
        Assert.assertEquals(TickerBatchFrameOutcome.Subscribed(2), ackOutcome)

        val tickerJson = """
            {
              "type": "ticker",
              "product_id": "ETH-USD",
              "price": "3000.00",
              "open_24h": "2500.00",
              "time": "2026-11-28T10:00:00Z"
            }
        """.trimIndent()
        val tickOutcome = service.parseTickerBatchFrame(
            rawText = tickerJson,
            productToCoinIds = mapOf("BTC-USD" to listOf("bitcoin"), "ETH-USD" to listOf("ethereum")),
            fxRate = 5.5
        )
        Assert.assertTrue(tickOutcome is TickerBatchFrameOutcome.Ticks)
        val tick = (tickOutcome as TickerBatchFrameOutcome.Ticks).items.single()
        Assert.assertEquals("ethereum", tick.coinId)
        Assert.assertEquals(16500.0, tick.price, 0.01)
        Assert.assertEquals(20.0, tick.change24h!!, 0.001)

        val errorJson = """{"type":"error","message":"Failed to subscribe","reason":"XYZ-USD is not a valid product"}"""
        val errorOutcome = service.parseTickerBatchFrame(
            rawText = errorJson,
            productToCoinIds = mapOf("BTC-USD" to listOf("bitcoin")),
            fxRate = 1.0
        )
        Assert.assertTrue(errorOutcome is TickerBatchFrameOutcome.Rejected)
        Assert.assertEquals(
            "XYZ-USD",
            FavoritesTickerBatchServiceImpl.extractInvalidProductId(
                (errorOutcome as TickerBatchFrameOutcome.Rejected).reason
            )
        )
    }

    @Test
    fun `parseCoinbaseUsdProducts keeps only online USD pairs`() {
        val productsJson = """
            [
              {"id":"BTC-USD","quote_currency":"USD","status":"online"},
              {"id":"ETH-EUR","quote_currency":"EUR","status":"online"},
              {"id":"OLD-USD","quote_currency":"USD","status":"delisted"},
              {"id":"SOL-USD","quote_currency":"USD","status":"online"}
            ]
        """.trimIndent()

        val parsed = service.parseCoinbaseUsdProducts(productsJson)
        Assert.assertEquals(setOf("BTC-USD", "SOL-USD"), parsed)
    }
}

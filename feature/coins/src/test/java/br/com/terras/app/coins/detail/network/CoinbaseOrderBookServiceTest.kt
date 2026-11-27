package br.com.terras.app.coins.detail.network

import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinbaseOrderBookServiceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `buildSubscribeFrame targets level2_batch channel for pair`() {
        val frame = CoinbaseOrderBookServiceImpl.buildSubscribeFrame("BTC-USD")
        assertTrue(frame.contains("\"type\":\"subscribe\""))
        assertTrue(frame.contains("\"product_ids\":[\"BTC-USD\"]"))
        assertTrue(frame.contains("\"channels\":[\"level2_batch\"]"))
    }

    @Test
    fun `OrderBookAggregator sorts bids descending asks ascending and computes depth and spread`() {
        val aggregator = OrderBookAggregator(pairSymbol = "BTC-USD", maxLevels = 8)

        val bids = buildJsonArray {
            add(jsonStringTuple("65000.00", "1.0"))
            add(jsonStringTuple("65010.00", "2.0"))
            add(jsonStringTuple("64990.00", "3.0"))
        }
        val asks = buildJsonArray {
            add(jsonStringTuple("65020.00", "1.5"))
            add(jsonStringTuple("65015.00", "0.5"))
            add(jsonStringTuple("65030.00", "2.0"))
        }

        aggregator.applySnapshot(bids, asks)
        val snapshot = aggregator.buildSnapshot(isLive = true)
        assertNotNull(snapshot)
        snapshot!!

        assertEquals("BTC-USD", snapshot.pairSymbol)
        // Best bid first (highest price)
        assertEquals(65010.0, snapshot.bids.first().price, 0.0001)
        assertEquals(64990.0, snapshot.bids.last().price, 0.0001)
        // Best ask first (lowest price)
        assertEquals(65015.0, snapshot.asks.first().price, 0.0001)
        assertEquals(65030.0, snapshot.asks.last().price, 0.0001)

        // Cumulative total on bids = 2 + 1 + 3 = 6.0; asks = 0.5 + 1.5 + 2.0 = 4.0 -> maxCumulative = 6.0
        assertEquals(2.0, snapshot.bids[0].cumulativeTotal, 0.0001)
        assertEquals(6.0, snapshot.bids[2].cumulativeTotal, 0.0001)
        assertEquals(1.0f, snapshot.bids[2].depthPercentage, 0.001f)
        assertEquals(4.0f / 6.0f, snapshot.asks[2].depthPercentage, 0.001f)

        // Spread = 65015 - 65010 = 5.0
        assertEquals(5.0, snapshot.spreadAbsolute, 0.0001)
        assertEquals("$5.00", snapshot.spreadAbsoluteFormatted)
        assertEquals("0.01%", snapshot.spreadPercentageFormatted)
    }

    @Test
    fun `OrderBookAggregator applyChanges updates inserts and removes zero-size levels`() {
        val aggregator = OrderBookAggregator(pairSymbol = "ETH-USD", maxLevels = 8)
        aggregator.applySnapshot(
            rawBids = buildJsonArray {
                add(jsonStringTuple("3000.00", "2.0"))
                add(jsonStringTuple("2995.00", "1.0"))
            },
            rawAsks = buildJsonArray {
                add(jsonStringTuple("3005.00", "1.5"))
                add(jsonStringTuple("3010.00", "3.0"))
            }
        )

        val changed = aggregator.applyChanges(
            buildJsonArray {
                add(jsonStringTuple("buy", "3000.00", "0")) // Remove best bid
                add(jsonStringTuple("buy", "2998.00", "4.0")) // Insert new best bid
                add(jsonStringTuple("sell", "3005.00", "2.5")) // Update best ask size
            }
        )
        assertTrue(changed)

        val snapshot = aggregator.buildSnapshot()!!
        assertEquals(2, snapshot.bids.size)
        assertEquals(2998.0, snapshot.bids.first().price, 0.0001)
        assertEquals(4.0, snapshot.bids.first().size, 0.0001)
        assertEquals(2.5, snapshot.asks.first().size, 0.0001)
        assertEquals(7.0, snapshot.spreadAbsolute, 0.0001)
    }

    @Test
    fun `OrderBookAggregator caps levels at maxLevels`() {
        val aggregator = OrderBookAggregator(pairSymbol = "SOL-USD", maxLevels = 8)
        val manyBids = buildJsonArray {
            for (i in 1..15) {
                add(jsonStringTuple("${100 + i}.00", "1.0"))
            }
        }
        val manyAsks = buildJsonArray {
            for (i in 1..15) {
                add(jsonStringTuple("${120 + i}.00", "1.0"))
            }
        }
        aggregator.applySnapshot(manyBids, manyAsks)
        val snapshot = aggregator.buildSnapshot()!!
        assertEquals(8, snapshot.bids.size)
        assertEquals(8, snapshot.asks.size)
        assertEquals(115.0, snapshot.bids.first().price, 0.0001)
        assertEquals(121.0, snapshot.asks.first().price, 0.0001)
    }

    @Test
    fun `processRawFrame handles snapshot delta error and wrong product_id`() = runBlocking {
        val service = CoinbaseOrderBookServiceImpl(
            wsClient = HttpClient(CIO),
            json = json
        )
        val aggregator = OrderBookAggregator("BTC-USD")
        val mutex = Mutex()

        val snapshotOutcome = service.processRawFrame(
            rawText = """{"type":"snapshot","product_id":"BTC-USD","bids":[["50000","1"]],"asks":[["50010","2"]]}""",
            productId = "BTC-USD",
            aggregator = aggregator,
            mutex = mutex
        )
        assertTrue(snapshotOutcome is OrderBookFrameOutcome.SnapshotApplied)

        val wrongProductOutcome = service.processRawFrame(
            rawText = """{"type":"l2update","product_id":"ETH-USD","changes":[["buy","3000","1"]]}""",
            productId = "BTC-USD",
            aggregator = aggregator,
            mutex = mutex
        )
        assertTrue(wrongProductOutcome is OrderBookFrameOutcome.Ignored)

        val validDeltaOutcome = service.processRawFrame(
            rawText = """{"type":"l2update","product_id":"BTC-USD","changes":[["buy","50005","1.5"]]}""",
            productId = "BTC-USD",
            aggregator = aggregator,
            mutex = mutex
        )
        assertTrue(validDeltaOutcome is OrderBookFrameOutcome.DeltaApplied)

        val noopDeltaOutcome = service.processRawFrame(
            rawText = """{"type":"l2update","product_id":"BTC-USD","changes":[["buy","50005","1.5"]]}""",
            productId = "BTC-USD",
            aggregator = aggregator,
            mutex = mutex
        )
        assertFalse(noopDeltaOutcome is OrderBookFrameOutcome.DeltaApplied)

        val errorOutcome = service.processRawFrame(
            rawText = """{"type":"error","message":"Failed to subscribe","reason":"FOO-USD is not a valid product"}""",
            productId = "FOO-USD",
            aggregator = aggregator,
            mutex = mutex
        )
        assertTrue(errorOutcome is OrderBookFrameOutcome.Rejected)
    }

    @Test
    fun `OrderBookAggregator groups bids floor and asks ceiling by multiplier factor`() {
        val aggregator = OrderBookAggregator(pairSymbol = "BTC-USD", maxLevels = 8)
        aggregator.applySnapshot(
            rawBids = buildJsonArray {
                add(jsonStringTuple("65009.90", "1.0"))
                add(jsonStringTuple("65001.20", "2.5"))
                add(jsonStringTuple("64998.00", "4.0"))
            },
            rawAsks = buildJsonArray {
                add(jsonStringTuple("65010.10", "1.5"))
                add(jsonStringTuple("65019.90", "0.5"))
                add(jsonStringTuple("65021.00", "3.0"))
            }
        )

        // For BTC (~65k), baseTick=0.01 -> X1000 = $10.00 step
        val grouped = aggregator.buildSnapshot(multiplier = OrderBookMultiplier.X1000)!!
        assertEquals(OrderBookMultiplier.X1000, grouped.selectedMultiplier)
        assertEquals("$10", grouped.selectedStepFormatted)

        // Bids 65009.90 and 65001.20 floor into 65000.0 (sum = 3.5); 64998.00 floors into 64990.0 (4.0)
        assertEquals(2, grouped.bids.size)
        assertEquals(65000.0, grouped.bids[0].price, 0.0001)
        assertEquals(3.5, grouped.bids[0].size, 0.0001)
        assertEquals("$65,000", grouped.bids[0].priceFormatted)
        assertEquals(64990.0, grouped.bids[1].price, 0.0001)
        assertEquals(4.0, grouped.bids[1].size, 0.0001)

        // Asks 65010.10 and 65019.90 ceil into 65020.0 (sum = 2.0); 65021.00 ceils into 65030.0 (3.0)
        assertEquals(2, grouped.asks.size)
        assertEquals(65020.0, grouped.asks[0].price, 0.0001)
        assertEquals(2.0, grouped.asks[0].size, 0.0001)
        assertEquals("$65,020", grouped.asks[0].priceFormatted)
        assertEquals(65030.0, grouped.asks[1].price, 0.0001)
        assertEquals(3.0, grouped.asks[1].size, 0.0001)

        // Market spread stays computed from raw top of book (65010.10 - 65009.90 = 0.20)
        assertEquals(0.20, grouped.spreadAbsolute, 0.0001)
        assertEquals("$0.20", grouped.spreadAbsoluteFormatted)
    }

    private fun jsonStringTuple(vararg values: String): JsonArray = buildJsonArray {
        values.forEach { add(JsonPrimitive(it)) }
    }
}

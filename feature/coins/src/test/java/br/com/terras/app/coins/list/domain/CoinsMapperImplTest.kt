package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.model.SparklineResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinData
import br.com.terras.app.coins.list.data.model.TrendingCoinItem
import br.com.terras.app.coins.list.data.model.TrendingCoinWrapper
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.mockCoinVOStub
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsResponseStub
import br.com.terras.app.coins.mockGlobalMarketResponseStub
import br.com.terras.app.coins.mockTrendingCoinsResponseStub
import br.com.terras.app.dsm.ui.component.TrendColor
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CoinsMapperImplTest {

    private lateinit var mapper: CoinsMapper

    @Before
    fun setup() {
        mapper = CoinsMapperImpl()
    }

    @Test
    fun `WHEN toCoins is called THEN coins are mapped correctly`() {
        val result = mapper.toCoins(mockCoinsListResponseStub)

        result.first().run {
            Assert.assertEquals(symbol, mockCoinVOStub.symbol)
            Assert.assertEquals(name, mockCoinVOStub.name)
            Assert.assertEquals(image, mockCoinVOStub.image)
            Assert.assertEquals(marketCapRank, mockCoinVOStub.marketCapRank)
            Assert.assertNotNull(price)
            Assert.assertNotNull(priceChangePercentage)
            Assert.assertEquals(trendColor, mockCoinVOStub.trendColor)
        }
    }

    @Test
    fun `WHEN toTrendingCoins has market enrichment THEN preserves trending order and uses market data`() {
        val trending = TrendingCoinsResponse(
            coins = listOf(
                TrendingCoinWrapper(
                    item = TrendingCoinItem(id = "pepe", symbol = "pepe", name = "Pepe", small = "pepe.png")
                ),
                TrendingCoinWrapper(
                    item = TrendingCoinItem(id = "id", symbol = "symbol", name = "name", small = "id.png")
                )
            )
        )
        val marketPepe = mockCoinsResponseStub.copy(
            id = "pepe",
            symbol = "pepe",
            name = "Pepe",
            priceChangePercentage = -2.5,
            totalVolume = 5000000.0,
            sparklineIn7d = SparklineResponse(price = listOf(1.0, 2.0, 1.5))
        )
        val marketId = mockCoinsResponseStub.copy(
            id = "id",
            totalVolume = 1000000.0,
            sparklineIn7d = SparklineResponse(price = listOf(10.0, 12.0))
        )

        val result = mapper.toTrendingCoins(trending, listOf(marketId, marketPepe))

        Assert.assertEquals(2, result.size)
        Assert.assertEquals("pepe", result[0].id)
        Assert.assertEquals("PEPE", result[0].symbol)
        Assert.assertEquals(TrendColor.DOWN, result[0].trendColor)
        Assert.assertEquals(listOf(1.0, 2.0, 1.5), result[0].sparklinePrices)
        Assert.assertEquals("id", result[1].id)
        Assert.assertEquals(TrendColor.UP, result[1].trendColor)
    }

    @Test
    fun `WHEN toTrendingCoins has no market enrichment THEN falls back to raw trending item data`() {
        val result = mapper.toTrendingCoins(mockTrendingCoinsResponseStub, emptyList())

        Assert.assertEquals(1, result.size)
        result.first().run {
            Assert.assertEquals("id", id)
            Assert.assertEquals("SYMBOL", symbol)
            Assert.assertEquals("name", name)
            Assert.assertEquals("image", image)
            Assert.assertEquals("1", marketCapRank)
            Assert.assertEquals(100.0, rawPrice, 0.001)
            Assert.assertEquals(1250000.0, rawVolume, 0.001)
            Assert.assertEquals(TrendColor.UP, trendColor)
        }
    }

    @Test
    fun `WHEN toTrendingCoins receives more than 20 items THEN caps result at top 20`() {
        val items = (1..25).map { idx ->
            TrendingCoinWrapper(
                item = TrendingCoinItem(
                    id = "coin-$idx",
                    symbol = "C$idx",
                    name = "Coin $idx",
                    data = TrendingCoinData(price = idx.toDouble())
                )
            )
        }

        val result = mapper.toTrendingCoins(TrendingCoinsResponse(coins = items), emptyList())

        Assert.assertEquals(20, result.size)
        Assert.assertEquals("coin-1", result.first().id)
        Assert.assertEquals("coin-20", result.last().id)
    }

    @Test
    fun `WHEN toGlobalMarket is called in pt-BR THEN formats compact BRL metrics and dominance`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("pt", "BR"))
            val result = mapper.toGlobalMarket(mockGlobalMarketResponseStub)

            Assert.assertEquals(44, result.fearAndGreedValue)
            Assert.assertEquals(FearAndGreedSentiment.FEAR, result.fearAndGreedSentiment)
            Assert.assertEquals("Medo", result.fearAndGreedLabel)
            Assert.assertTrue(result.hasFearAndGreed)
            Assert.assertTrue(result.totalMarketCap.contains("tri"))
            Assert.assertTrue(result.totalVolume24h.contains("bi"))
            Assert.assertEquals("54,2%", result.btcDominance)
            Assert.assertEquals("16,5%", result.ethDominance)
            Assert.assertEquals("+2,4%", result.marketCapChangePercentage)
            Assert.assertEquals(TrendColor.UP, result.trendColor)
            Assert.assertEquals("15.420", result.activeCryptocurrencies)
            Assert.assertEquals("1.120", result.marketsCount)
            Assert.assertEquals(54.2, result.btcDominancePercent, 0.01)
            Assert.assertEquals(54, result.btcDominanceScore)
            Assert.assertEquals(46, result.altcoinsDominanceScore)
            Assert.assertTrue(result.hasBtcDominance)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `WHEN toGlobalMarket is called in en-US THEN formats compact USD metrics`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val result = mapper.toGlobalMarket(mockGlobalMarketResponseStub)

            Assert.assertEquals(44, result.fearAndGreedValue)
            Assert.assertEquals("Fear", result.fearAndGreedLabel)
            Assert.assertTrue(result.totalMarketCap.endsWith("T"))
            Assert.assertTrue(result.totalVolume24h.endsWith("B"))
            Assert.assertEquals("54.2%", result.btcDominance)
            Assert.assertEquals("16.5%", result.ethDominance)
            Assert.assertEquals("+2.4%", result.marketCapChangePercentage)
            Assert.assertEquals("15,420", result.activeCryptocurrencies)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `WHEN CoinsResponse JSON has null rank or 24h change THEN coerces safely and maps coin`() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val payload = """
            [
              {
                "id": "new-token",
                "symbol": "new",
                "name": "New Token",
                "image": null,
                "market_cap_rank": null,
                "current_price": 1.25,
                "price_change_percentage_24h": null,
                "total_volume": null
              }
            ]
        """.trimIndent()

        val decoded = json.decodeFromString<List<br.com.terras.app.coins.list.data.model.CoinsResponse>>(payload)
        val mapped = mapper.toCoins(decoded)

        Assert.assertEquals(1, mapped.size)
        Assert.assertEquals("new-token", mapped.first().id)
        Assert.assertEquals("NEW", mapped.first().symbol)
        Assert.assertEquals("", mapped.first().marketCapRank)
        Assert.assertEquals(1.25, mapped.first().rawPrice, 0.001)
    }
}
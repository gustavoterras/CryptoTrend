package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.mockCoinDetailChartResponseStub
import br.com.terras.app.coins.mockCoinDetailResponseStub
import br.com.terras.app.coins.mockCoinInsightResponseStub
import br.com.terras.app.coins.mockCoinVOStub
import br.com.terras.app.dsm.ui.component.TrendColor
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class DetailMapperImplTest {
    private lateinit var mapper: DetailMapper

    @Before
    fun setup() {
        mapper = DetailMapperImpl()
    }

    @Test
    fun `WHEN toCoin is called THEN coins are mapped correctly`() {
        val result = mapper.toCoin(mockCoinDetailResponseStub)

        result.run {
            Assert.assertEquals(symbol, mockCoinVOStub.symbol)
            Assert.assertEquals(name, mockCoinVOStub.name)
            Assert.assertEquals(image, mockCoinVOStub.image)
            Assert.assertEquals(marketCapRank, mockCoinVOStub.marketCapRank)
            Assert.assertNotNull(price)
            Assert.assertNotNull(priceChangePercentageList)
            Assert.assertNotNull(marketDataList)
            Assert.assertTrue(marketDataList.size == 12)
            Assert.assertEquals(trendColor, mockCoinVOStub.trendColor)
        }
    }

    @Test
    fun `WHEN toCoin is called THEN coins are mapped correctly when trend color is down`() {
        val result = mapper.toCoin(
            mockCoinDetailResponseStub.copy(
                marketData = mockCoinDetailResponseStub.marketData.copy(
                    priceChangePercentage24h = -1.0
                )
            )
        )

        result.run {
            Assert.assertEquals(symbol, mockCoinVOStub.symbol)
            Assert.assertEquals(name, mockCoinVOStub.name)
            Assert.assertEquals(image, mockCoinVOStub.image)
            Assert.assertEquals(marketCapRank, mockCoinVOStub.marketCapRank)
            Assert.assertNotNull(price)
            Assert.assertNotNull(priceChangePercentageList)
            Assert.assertNotNull(marketDataList)
            Assert.assertEquals(trendColor, TrendColor.DOWN)
        }
    }

    @Test
    fun `WHEN toCoin is called THEN coins are mapped correctly when trend color is flat`() {
        val result = mapper.toCoin(
            mockCoinDetailResponseStub.copy(
                marketData = mockCoinDetailResponseStub.marketData.copy(
                    priceChangePercentage24h = 0.0
                )
            )
        )

        result.run {
            Assert.assertEquals(symbol, mockCoinVOStub.symbol)
            Assert.assertEquals(name, mockCoinVOStub.name)
            Assert.assertEquals(image, mockCoinVOStub.image)
            Assert.assertEquals(marketCapRank, mockCoinVOStub.marketCapRank)
            Assert.assertNotNull(price)
            Assert.assertNotNull(priceChangePercentageList)
            Assert.assertNotNull(marketDataList)
            Assert.assertEquals(trendColor, TrendColor.FLAT)
        }
    }

    @Test
    fun `WHEN toChart is called THEN coins are mapped correctly`() {
        val result = mapper.toChart(mockCoinDetailChartResponseStub)

        Assert.assertNotNull(result.chartData)
        Assert.assertTrue(result.chartData.isNotEmpty())
        Assert.assertTrue(result.chartData.all { !it.xLabel.isNullOrBlank() })
    }

    @Test
    fun `WHEN toChart is called for 24h span THEN xLabel uses hour and minute format`() {
        val baseMillis = 1_716_249_600_000.0
        val hourMillis = 3_600_000.0
        val response = mockCoinDetailChartResponseStub.copy(
            prices = listOf(
                listOf(baseMillis, 34000.0),
                listOf(baseMillis + 12 * hourMillis, 34500.0),
                listOf(baseMillis + 24 * hourMillis, 35000.0)
            )
        )

        val result = mapper.toChart(response)

        Assert.assertEquals(3, result.chartData.size)
        result.chartData.forEach { point ->
            Assert.assertTrue(
                "Expected HH:mm format for 24h span, got ${point.xLabel}",
                point.xLabel?.matches(Regex("\\d{2}:\\d{2}")) == true
            )
            Assert.assertTrue(
                "Expected HH:mm hourLabel, got ${point.hourLabel}",
                point.hourLabel.matches(Regex("\\d{2}:\\d{2}"))
            )
            Assert.assertTrue(
                "Expected dd/MM/yyyy dayLabel, got ${point.dayLabel}",
                point.dayLabel.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))
            )
        }
    }

    @Test
    fun `WHEN toChart is called for 30d span THEN xLabel uses day and month format`() {
        val baseMillis = 1_716_249_600_000.0
        val dayMillis = 86_400_000.0
        val response = mockCoinDetailChartResponseStub.copy(
            prices = listOf(
                listOf(baseMillis, 34000.0),
                listOf(baseMillis + 15 * dayMillis, 34500.0),
                listOf(baseMillis + 30 * dayMillis, 35000.0)
            )
        )

        val result = mapper.toChart(response)

        Assert.assertEquals(3, result.chartData.size)
        result.chartData.forEach { point ->
            Assert.assertTrue(
                "Expected dd/MM format for 30d span, got ${point.xLabel}",
                point.xLabel?.matches(Regex("\\d{2}/\\d{2}")) == true
            )
            Assert.assertTrue(
                "Expected dd/MM/yyyy dayLabel, got ${point.dayLabel}",
                point.dayLabel.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))
            )
        }
    }

    @Test
    fun `WHEN toInsights is called THEN insights are mapped and blank entries filtered`() {
        val response = listOf(
            mockCoinInsightResponseStub,
            mockCoinInsightResponseStub.copy(title = "", description = "")
        )

        val result = mapper.toInsights(response)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals(mockCoinInsightResponseStub.title, result.first().title)
        Assert.assertEquals(mockCoinInsightResponseStub.description, result.first().description)
        Assert.assertEquals(mockCoinInsightResponseStub.relatedCoinIds, result.first().relatedCoinIds)
        Assert.assertTrue(result.first().postedAt.isNotBlank())
    }
}
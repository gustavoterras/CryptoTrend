package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.mockCoinDetailChartResponseStub
import br.com.terras.app.coins.mockCoinDetailResponseStub
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
    }
}
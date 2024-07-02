package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.mockCoinDetailChartResponseStub
import br.com.terras.app.coins.mockCoinDetailResponseStub
import br.com.terras.app.coins.mockCoinVOStub
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
            Assert.assertEquals(trendColor, mockCoinVOStub.trendColor)
        }
    }

    @Test
    fun `WHEN toChart is called THEN coins are mapped correctly`() {
        val result = mapper.toChart(mockCoinDetailChartResponseStub)

        Assert.assertNotNull(result.chartData)
    }
}
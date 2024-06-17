package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.domain.CoinsMapper
import br.com.terras.app.coins.list.domain.CoinsMapperImpl
import br.com.terras.app.coins.mockCoinVOStub
import br.com.terras.app.coins.mockCoinsListResponseStub
import org.junit.Assert
import org.junit.Before
import org.junit.Test

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
}
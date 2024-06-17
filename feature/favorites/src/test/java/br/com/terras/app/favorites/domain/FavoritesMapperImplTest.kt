package br.com.terras.app.favorites.domain

import br.com.terras.app.favorites.mockCoinVOStub
import br.com.terras.app.favorites.mockCoinsListResponseStub
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FavoritesMapperImplTest {

    private lateinit var mapper: FavoritesMapper

    @Before
    fun setup() {
        mapper = FavoritesMapperImpl()
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
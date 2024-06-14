package br.com.terras.app.coins.domain

import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsVOListStub
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

        Assert.assertEquals(result, mockCoinsVOListStub)
    }
}
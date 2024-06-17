package br.com.terras.app.search.domain

import br.com.terras.app.search.mockCoinVOStub
import br.com.terras.app.search.mockCoinsResponseStub
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SearchMapperImplTest {

    private lateinit var mapper: SearchMapper

    @Before
    fun setup() {
        mapper = SearchMapperImpl()
    }

    @Test
    fun `WHEN toSearch is called THEN coins are mapped correctly`() {
        val result = mapper.toSearch(mockCoinsResponseStub)

        result.first().run {
            Assert.assertEquals(id, mockCoinVOStub.id)
            Assert.assertEquals(symbol, mockCoinVOStub.symbol)
            Assert.assertEquals(name, mockCoinVOStub.name)
            Assert.assertEquals(thumb, mockCoinVOStub.thumb)
        }
    }
}
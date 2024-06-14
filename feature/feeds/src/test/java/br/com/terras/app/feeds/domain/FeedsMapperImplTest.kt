package br.com.terras.app.feeds.domain

import br.com.terras.app.feeds.mockArticlesVOStub
import br.com.terras.app.feeds.mockFeedsResponseStub
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FeedsMapperImplTest {

    private lateinit var feedsMapper: FeedsMapper

    @Before
    fun setup() {
        feedsMapper = FeedsMapperImpl()
    }

    @Test
    fun `WHEN toFeeds is called THEN feeds are mapped correctly`() {
        val result = feedsMapper.toFeeds(mockFeedsResponseStub.articles)

        Assert.assertEquals(result, mockArticlesVOStub)
    }
}
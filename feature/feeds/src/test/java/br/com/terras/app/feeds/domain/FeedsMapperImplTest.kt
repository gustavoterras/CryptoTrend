package br.com.terras.app.feeds.domain

import br.com.terras.app.feeds.mockArticleVOStub
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

        result.first().run {
            Assert.assertEquals(author, mockArticleVOStub.author)
            Assert.assertEquals(title, mockArticleVOStub.title)
            Assert.assertEquals(description, mockArticleVOStub.description)
            Assert.assertEquals(url, mockArticleVOStub.url)
            Assert.assertEquals(urlToImage, mockArticleVOStub.urlToImage)
            Assert.assertEquals(publishedAt, mockArticleVOStub.publishedAt)
            Assert.assertEquals(content, mockArticleVOStub.content)
        }
    }
}